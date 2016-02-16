/*
 * Copyright (c) 2014, tibbitts
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.puyallupfamilyhistorycenter.service.websocket;

import org.puyallupfamilyhistorycenter.service.ServletLifecycleManager;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.puyallupfamilyhistorycenter.service.cache.PersonDao;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.familysearch.api.client.UserState;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.gedcomx.rs.client.PersonState;
import org.joda.time.DateTime;
import org.puyallupfamilyhistorycenter.service.ApplicationProperties;
import org.puyallupfamilyhistorycenter.service.Contact;
import org.puyallupfamilyhistorycenter.service.SpringContextInitializer;
import org.puyallupfamilyhistorycenter.service.cache.Precacher;
import org.puyallupfamilyhistorycenter.service.models.Checklist;
import org.puyallupfamilyhistorycenter.service.models.ChecklistItem;
import org.puyallupfamilyhistorycenter.service.models.Statistics;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonImage;
import org.puyallupfamilyhistorycenter.service.models.Video;
import org.puyallupfamilyhistorycenter.service.utils.EmailUtils;
import org.puyallupfamilyhistorycenter.service.utils.S3Utils;

/**
 *
 * @author tibbitts
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class FamilyHistoryCenterSocket {
    private static final Logger logger = Logger.getLogger(FamilyHistoryCenterSocket.class);
    private static final Gson GSON = new Gson();
    private static final PersonDao personDao;
    private static final AppKeyConfig appKeyConfig;
    
    private static final String OK_RESPONSE = "{\"responseType\":\"ok\"}";
    
    static {
        personDao = (PersonDao) SpringContextInitializer.getContext().getBean("person-dao");
        appKeyConfig = (AppKeyConfig) SpringContextInitializer.getContext().getBean("app-key-config");
    }
    

    private static final class UserContext {
        public final String userName;
        public final String userId;
        public final String userEmail;
        public final String hashedPin;
        public final String accessToken;
        public long lastUsed;
        public final Set<String> tokens; //TODO: Rename to prevent confusion with access token
        public final Precacher precacher;
        public UserContext(String userId, String userName, String userEmail, String hashedPin, String accessToken, Precacher precacher) {
            this(userId, userName, userEmail, hashedPin, accessToken, precacher, new HashSet<String>());
        }
        public UserContext(String userId, String userName, String userEmail, String hashedPin, String accessToken, Precacher precacher, Set<String> tokens) {
            this.userName = userName;
            this.userId = userId;
            this.userEmail = userEmail;
            this.hashedPin = hashedPin;
            this.accessToken = accessToken;
            this.lastUsed = System.currentTimeMillis();
            this.tokens = tokens==null? new HashSet<String>() : tokens;
            this.precacher = precacher;
        }
    }
    
    private static final long tokenInactivityTimeout = TimeUnit.MINUTES.toMillis(30); //TODO: Reset this
    private static final long userInactivityTimeout  = TimeUnit.MINUTES.toMillis(60);
    
    private static final Map<String, RemoteEndpoint> remoteDisplays = new HashMap<>();
    private static final Map<String, RemoteEndpoint> remoteControllers = new HashMap<>();
    private static final Set<RemoteEndpoint> remotePresenters = new HashSet<>();
    private static final Set<RemoteEndpoint> otherRemotes = new HashSet<>();
    private static final Map<String, RemoteEndpoint> tokenControllerMap = new HashMap<>();
    private static final Map<String, String> tokenUserIdMap = new HashMap<>();
    private static final Map<String, Long> tokenLastUse = new HashMap<>();
    private static final Map<String, UserContext> userContextMap = new LinkedHashMap<>();
    private static final Set<String> stackTraces = new HashSet<>();
    private static final Checklist checklist = newChecklist();
    private static final SecureRandom rand;
    static {
        try {
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to create SecureRand instance", ex);
        }
    }
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static {
        scheduler.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    Iterator<Map.Entry<String, Long>> it = tokenLastUse.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Long> entry = it.next();
                        if (entry.getValue() + tokenInactivityTimeout < System.currentTimeMillis()) {
                            String token = entry.getKey();
                            deactivateUserToken(token, "Logged out due to inactivity");
                            it.remove();
                        }
                    }
                } catch (Throwable t) {
                    reportBug(t);
                }
            }
        
        }, 1, 1, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    Iterator<Map.Entry<String, UserContext>> it = userContextMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, UserContext> entry = it.next();
                        if (entry.getValue().lastUsed + userInactivityTimeout < System.currentTimeMillis()) {
                            String userId = entry.getKey();
                            // TODO: Figure out how to invalidate access token
                            String accessToken = entry.getValue().accessToken;

                            sendFinalEmail(userId);

                            it.remove();
                        }
                    }

                    resendUserList();
                } catch (Throwable t) {
                    reportBug(t);
                }
            }
        
        }, 1, 1, TimeUnit.MINUTES);
    }

    public FamilyHistoryCenterSocket() {
        setGuestUser();
    }
    
    @OnWebSocketConnect
    public void handleConnection(Session session) throws IOException {
        session.getRemote().sendString("connected");
    }
    
    @OnWebSocketMessage
    public void handleMessage(Session session, String message) throws IOException, URISyntaxException {
        logger.debug("Got websocket request '" + message + "'");
        String response = OK_RESPONSE;
        try {
            Scanner scanner = new Scanner(message);
            scanner.useDelimiter(" ");
            String token = null;
            String userId = null;
            String cmd = scanner.next();
            switch (cmd) {
                case "ping":
                    response = "{\"responseType\":\"pong\"}";
                    break;
                    
                case "reconnect":
                    String type = scanner.next();
                    switch (type) {
                        case "display":
                            scheduleReload(session.getRemote(), 1);
                            break;
                        default:
                            scheduleReload(session.getRemote(), 5);
                            break;
                    }
                    break;
                    
                case "restart-server": {
                    Iterator<UserContext> it = userContextMap.values().iterator();
                    while (it.hasNext()) {
                        UserContext context = it.next();
                        //TODO: Invalidate access tokens
                        if (context.precacher != null) {
                            context.precacher.cancel();
                            it.remove();
                        }
                    }
                    
                    for (RemoteEndpoint endpoint : remoteDisplays.values()) {
                        scheduleReload(endpoint, 10);
                    }
                    remoteDisplays.clear();
                    
                    for (RemoteEndpoint endpoint : remoteControllers.values()) {
                        scheduleReload(endpoint, 15);
                    }
                    remoteControllers.clear();
                    
                    for (RemoteEndpoint endpoint : otherRemotes) {
                        scheduleReload(endpoint, 15);
                    }
                    otherRemotes.clear();
                    
                    ServletLifecycleManager.restart();
                    break;
                }

                case "controller": {
                    String id = scanner.next();
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(id);
                    if (displayEndpoint == null) {
                        response = "{\"responseType\":\"error\",\"message\":\"display not found '" + id + "'\"}";
                        break;
                    }
                    boolean alreadyConnected = false;
                    if (remoteControllers.containsKey(id)) {
                        try {
                            remoteControllers.get(id).sendString("{\"responseType\":\"pong\"}");
                            alreadyConnected = true;
                        } catch (IOException ex) {
                            //DO NOTHING
                        }
                    }
                    if (!alreadyConnected) {
                        remoteControllers.put(id, session.getRemote());
                        response = "{\"responseType\":\"attached\"}";
                    } else {
                        session.getRemote().sendString("{\"responseType\":\"nav\",\"fragment\":\"controller-attach\"}");
                        response = "{\"responseType\":\"error\",\"message\":\"display " + id + " is already connected to another controller\"}";
                    }
                    break;
                }

                case "display": {
                    String id = scanner.next();
                    if (!isDisplayActive(id)) {
                        remoteDisplays.put(id, session.getRemote());
                        response = "{\"responseType\":\"standby\"}";
                        resendDisplayList();
                    } else {
                        response = "{\"responseType\":\"error\",\"message\":\"display " + id + " is already connected\"}";
                    }
                    break;
                }

                case "display-name": {
                    String id = Integer.toHexString(rand.nextInt());
                    response = "{\"responseType\":\"name\",\"name\":\""+id+"\"}";
                    break;
                }
                
                case "listDisplays": {
                    //TODO: Perhaps move this to a different 'attach' method, with authenticationß
                    remotePresenters.add(session.getRemote());
                    otherRemotes.add(session.getRemote());
                    
                    response = getDisplayListResponse();
                    break;
                }
                
                case "listControllers": {
                    response = "{\"responseType\":\"controllers\",\"controllers\":"+GSON.toJson(remoteControllers.keySet())+"}";
                    break;
                }
                
                case "pingDisplay": {
                    String id = scanner.next();
                    if (!isDisplayActive(id)) {
                        response = getErrorResponse("Display " + id + " is not active");
                    } else {
                        response = sendToDisplay(id, getIndentifyDisplayResponse());
                    }
                    break;
                }
                
                case "reloadDisplay": {
                    String displayName = scanner.next();
                    if (remoteDisplays.containsKey(displayName)) {
                        scheduleReload(remoteDisplays.get(displayName), 1);
                    }
                    if (remoteControllers.containsKey(displayName)) {
                        scheduleReload(remoteControllers.get(displayName), 3);
                    }
                    
                    break;
                }
                
                case "reportBug": {
                    String reporter = scanner.next().replaceAll("%20", " ");
                    String reportBody = scanner.next().replaceAll("%20", " ");
                    reportBug(reporter, reportBody);
                    break;
                }

                case "login": {
                    userId = scanner.next();
                    String pin = scanner.next(); //TODO: This is pretty insecure
                    
                    UserContext tokenInfo = userContextMap.get(userId);
                    if (tokenInfo != null && validatePin(pin, tokenInfo.hashedPin)) {
                        token = Long.toHexString(rand.nextLong());
                        tokenUserIdMap.put(token, userId);
                        tokenControllerMap.put(token, session.getRemote());
                        userContextMap.get(userId).tokens.add(token);
                        response = "{\"responseType\":\"token\",\"token\":\""+token+"\",\"username\":\""+tokenInfo.userName+"\"}";
                    } else {
                        throw new IllegalStateException("username and PIN do not match");
                    }
                    break;
                }
                
                case "logout": {
                    token = scanner.next();
                    userId = tokenUserIdMap.get(token);
                    
                    tokenUserIdMap.remove(token);
                    tokenControllerMap.remove(token);
                    userContextMap.get(userId).tokens.remove(token);
                    
                    token = null;
                    break;
                }
                
                case "get-person": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    Person person = personDao.getPerson(personId, accessToken);
                    if (person != null) {
                        response = getPersonResponse(person);
                    } else {
                        throw new IllegalStateException("person " + personId + " not found");
                    }
                    
                    break;
                }
                
                case "send-person": {
                    token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    Person person = personDao.getPerson(personId, accessToken);
                    
                    response = sendToDisplay(displayId, getPersonResponse(person));
                    
                    break;
                }
                
                case "get-family": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String lastPageId = null;
                    if (scanner.hasNext()) {
                        lastPageId = scanner.next();
                    }
                    String accessToken = tokenToAccessToken(token);
                    
                    List<Person> family = personDao.listImmediateFamily(personId, accessToken);
                    if (!family.isEmpty()) {
                        response = getPeopleResponse(family);
                    } else {
                        throw new IllegalStateException("person " + personId + " not found");
                    }
                    
                    break;
                }
                
                case "send-family": {
                    token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    List<Person> family = personDao.listImmediateFamily(personId, accessToken);
                    
                    response = sendToDisplay(displayId, getPeopleResponse(family));
                    
                    break;
                }
                
                case "get-ancestors": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    int depth = 4;
                    if (scanner.hasNext()) {
                        depth = scanner.nextInt();
                    }
                    
                    List<Person> family = personDao.listAncestors(personId, depth, accessToken, true);
                    if (!family.isEmpty()) {
                        response = getPeopleResponse(family);
                    } else {
                        throw new IllegalStateException("person " + personId + " not found");
                    }
                    
                    break;
                }
                
                case "send-ancestors": {
                    token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    List<Person> family = personDao.listAncestors(personId, 4, accessToken, true);
                    
                    response = sendToDisplay(displayId, getPeopleResponse(family));
                    
                    break;
                }
                
                case "get-descendants": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String paginationKey = null;
                    String accessToken = tokenToAccessToken(token);
                    
                    List<Person> family = personDao.listDescendants(personId, 2, accessToken);
                    if (!family.isEmpty()) {
                        response = getPeopleResponse(family);
                    } else {
                        throw new IllegalStateException("person " + personId + " not found");
                    }
                    
                    break;
                }
                
                case "send-descendants": {
                    token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    List<Person> family = personDao.listDescendants(personId, 3, accessToken);
                    
                    response = sendToDisplay(displayId, getPeopleResponse(family));
                    
                    break;
                }
                
                case "get-ancestor-images": {
                    token = scanner.next();
                    String personId = scanner.next();
                    List<Person> people = personDao.listAncestorsWithImages(personId, 5, token);
                    
                    response = getPeopleResponse(people);
                    
                    break;
                }
                
                case "get-ancestor-stats": {
                    token = scanner.next();
                    String personId = scanner.next();
                    List<Person> people = personDao.listAncestors(personId, 10, token, true);
                    
                    response = getStatisticsResponse(new Statistics(people));
                    
                    break;
                }
                
                case "send-ancestor-stats": {
                    token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    List<Person> people = personDao.listAncestors(personId, 10, token, true);
                    
                    response = sendToDisplay(displayId, getStatisticsResponse(new Statistics(people)));
                    
                    break;
                }
                
                case "send": {
                    token = scanner.next();
                    String id = scanner.next();
                    String toSend = scanner.nextLine();
                    response = sendToDisplay(id, toSend);
                    break;
                }
                
                case "get-app-key": {
                    otherRemotes.add(session.getRemote());
                    
                    response = "{\"responseType\":\"app-key\",\"key\":\"" + appKeyConfig.appKey + "\",\"environment\":\"" + appKeyConfig.environment + "\"}";
                    
                    break;
                }

                case "access-token": {
                    userId = scanner.next();
                    String userName = URLDecoder.decode(scanner.next(), StandardCharsets.UTF_8.name());
                    String salt = newSalt();
                    String pin = hashPin(scanner.next(), salt);
                    String accessToken = scanner.next();

                    FamilySearchFamilyTree tree = FamilyHistoryFamilyTree.getInstance(accessToken);

                    PersonState person = tree.readPersonForCurrentUser();

                    if (person == null || !person.getSelfUri().getPath().endsWith(userId)) {
                        throw new IllegalStateException("Access token does not match userId");
                    }
                    
                    UserState user = tree.readCurrentUser();
                    
                    String email = user.getUser().getEmail();
                    
                    //TODO: Factor this out into separate class (it's pretty complicated
                    final String finalUserId = userId;
                    final AtomicReference currentEvent = new AtomicReference();
                    final Future<?> progressThrottle = scheduler.scheduleAtFixedRate(new Runnable() {

                        @Override
                        public void run() {
                            Precacher.PrecacheEvent event = (Precacher.PrecacheEvent) currentEvent.get();
                            UserContext context = userContextMap.get(finalUserId);
                            if (event != null && context != null) {
                                Iterator<String> it = context.tokens.iterator();
                                while (it.hasNext()) {
                                    String token = it.next();
                                    try {
                                        tokenControllerMap.get(token).sendString(GSON.toJson(event));
                                    } catch (Exception e) {
                                        logger.warn("Failed to notify controller " + token + " about precache event; removing");
                                        it.remove();
                                        tokenControllerMap.remove(token);
                                    }
                                }
                                
                                Iterator<RemoteEndpoint> rit = remotePresenters.iterator();
                                while (rit.hasNext()) {
                                    RemoteEndpoint endpoint = rit.next();
                                    try {
                                        endpoint.sendString(GSON.toJson(event));
                                    } catch (Exception e) {
                                        logger.warn("Failed to notify presenter about precache event; removing");
                                        rit.remove();
                                        otherRemotes.remove(endpoint);
                                    }
                                }
                            }
                        }
                    }, 10, 10, TimeUnit.SECONDS);
                    
                    Precacher precacher = new Precacher(userId, accessToken, 8);
                    
                    precacher.addPrecacheListener(new Precacher.PrecacheListener() {

                        @Override
                        public void onPrecache(Precacher.PrecacheEvent event) {
                            currentEvent.set(event);
                        }

                        @Override
                        public void onFinish() {
                            progressThrottle.cancel(false);
                            UserContext context = userContextMap.get(finalUserId);
                            Precacher.PrecacheEvent previousEvent = (Precacher.PrecacheEvent) currentEvent.get();
                            if (previousEvent != null && context != null) {
                                Precacher.PrecacheEvent event = new Precacher.PrecacheEvent(finalUserId, previousEvent.totalCached + previousEvent.totalQueueSize, 0, 0, previousEvent.currentGeneration);
                                Iterator<String> it = context.tokens.iterator();
                                while (it.hasNext()) {
                                    String token = it.next();
                                    try {
                                        tokenControllerMap.get(token).sendString(GSON.toJson(event));
                                    } catch (Exception e) {
                                        logger.warn("Failed to notify controller " + token + " about precache event; removing");
                                        it.remove();
                                        tokenControllerMap.remove(token);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancel() {
                            progressThrottle.cancel(true);
                        }
                    });
                    precacher.precache();
                    
                    Set<String> tokens = null;
                    if (userContextMap.containsKey(userId)) {
                        UserContext oldContext = userContextMap.get(userId);
                        if (oldContext != null) {
                            tokens = oldContext.tokens;
                        }
                    }
                    userContextMap.put(userId, new UserContext(userId, userName, email, pin, accessToken, precacher, tokens));
                    
                    resendUserList();
                    break;
                }

                case "destroy-access-token": {
                    userId = scanner.next();
                    String pin = scanner.next(); //TODO: This is pretty insecure
                    
                    UserContext userContext = userContextMap.get(userId);
                    if (userContext != null && validatePin(pin, userContext.hashedPin)) {
                        // TODO: Revoke access token
                        sendFinalEmail(userId);
                        userContextMap.remove(userId);
                        for (String t : userContext.tokens) {
                            deactivateUserToken(t, "User " + userContext.userName + " logged out of the system");
                        }
                        userId = null;
                        
                        if (userContext.precacher != null) {
                            userContext.precacher.cancel();
                        }
                        
                        resendUserList();
                    } else {
                        getErrorResponse("username and PIN do not match");
                    }
                    break;
                }
                
                case "forceDestroyAccessToken": {
                    userId = scanner.next();
                    
                    UserContext userContext = userContextMap.get(userId);
                    if (userContext != null) {
                        // TODO: Revoke access token
                        sendFinalEmail(userId);
                        userContextMap.remove(userId);
                        for (String t : userContext.tokens) {
                            deactivateUserToken(t, "User " + userContext.userName + " logged out of the system");
                        }
                        userId = null;
                        
                        if (userContext.precacher != null) {
                            userContext.precacher.cancel();
                        }
                        
                        resendUserList();
                    }
                    break;
                }

                case "list-current-users": {
                    response = generateNewUserListResponse();
                    break;
                }
                
                case "listVideos": {
                    File videosDir = new File(System.getProperty("user.home") + "/Dropbox/Videos");
                    String[] videoFiles = videosDir.list(new WildcardFileFilter("*.mp4"));
                    List<Video> videos = new ArrayList<>(videoFiles.length);
                    for (String videoFile : videoFiles) {
                        videos.add(new Video("/videos/" + videoFile));
                    }
                    
                    response = "{\"responseType\":\"videosList\",\"videosList\":" + GSON.toJson(videos) + "}";
                    break;
                }
                
                case "getChecklist": {
                    response = GSON.toJson(checklist);
                    
                    break;
                }
                
                case "check": {
                    String id = scanner.next();
                    boolean value = scanner.nextBoolean();
                    checklist.setChecked(id, value);
                    break;
                }
                
                case "signedPutUrl": {
                    token = scanner.next();
                    String userName = "anonymous";
                    userId = tokenUserIdMap.get(token);
                    if (userId != null) {
                        UserContext context = userContextMap.get(userId);
                        userName = context.userName;
                    }
                    
                    String contentType = scanner.next();
                    URL url = S3Utils.getSignedPutUrl(
                            ApplicationProperties.getVideoS3Bucket(), 
                            ApplicationProperties.getVideoS3KeyPrefix() + userName + "/" + new DateTime().getMillis(),
                            contentType
                    );
                    response = "{\"responseType\":\"signedPutUrl\",\"signedUrl\":\"" + url.toString() + "\"}";
                    
                    break;
                }
                
                case "forceNavigateController": {
                    String controllerId = scanner.next();
                    String dest = scanner.next();
                    
                    //TODO: Verify controller has logged-in user
                    
                    sendToController(controllerId, "{\"responseType\":\"nav\",\"dest\":\"" + dest + "\"}");
                    break;
                }
                
                case "survey": {
                    token = scanner.next();
                    userId = tokenUserIdMap.get(token);
                    
                    if (userId == null) {
                        throw new IllegalStateException("Token doesn't exist");
                    }
                    
                    UserContext ctx = userContextMap.get(userId);
                    
                    if (ctx == null) {
                        throw new IllegalStateException("User not found");
                    }
                    
                    String stake = scanner.next();
                    String ward = scanner.next();
                    String interestsStr = scanner.next();
                    boolean contactMe = scanner.nextBoolean();
                    String phone = scanner.next();
                    
                    if (contactMe) {
                        String[] interestIds = interestsStr.split(",");
                        Contact contact = ApplicationProperties.getWardContact(stake, ward);
                        
                        EmailUtils.sendReferralEmail(contact.getFullName(), contact.getEmail(), ctx.userName, ctx.userEmail, phone, contact.getWard(), ApplicationProperties.getInterests(interestIds));
                    }
                    
                    response = "{\"responseType\":\"surveyFinished\"}";
                    
                    break;
                }

                default:
                    throw new IllegalStateException("unrecognized command '" + message + "'");
            }
            
            if (token != null) {
                tokenLastUse.put(token, System.currentTimeMillis());
            }
            if (userId != null && userContextMap.containsKey(userId)) {
                userContextMap.get(userId).lastUsed = System.currentTimeMillis();
            }
        } catch (Throwable e) {
            logger.error("Unexpected exception: " + e, e);
            response = getErrorResponse(e.getMessage());
            reportBug(e);
        }
        
        if (session.isOpen()) {
            logger.debug("Sending web socket response '" + StringUtils.abbreviate(response, 100) + "'");
            session.getRemote().sendString(response);
        }
    }
    
    private String tokenToAccessToken(String token) {
        String userId = tokenUserIdMap.get(token);

        if (userId == null) {
            throw new IllegalStateException("Invalid token");
        }

        UserContext accessTokenInfo = userContextMap.get(userId);
        if (accessTokenInfo == null) {
            throw new IllegalStateException("Invalid token");
        }
        
        return accessTokenInfo.accessToken;
    }
    
    private <E> String toString(Iterator<E> it) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        while (it.hasNext()) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(",");
            }
            builder.append(GSON.toJson(it.next()));
        }
        return builder.toString();
    }
    
    protected static String generateNewUserListResponse() {
        StringBuilder userListBuilder = new StringBuilder("{\"responseType\":\"user-list\",\"users\":[");
        boolean first = true;
        for (UserContext ati : userContextMap.values()) {
            if (first) {
                first = false;
            } else {
                userListBuilder.append(",");
            }
            userListBuilder
                    .append("{\"id\":\"").append(ati.userId).append("\",")
                    .append("\"name\":\"").append(ati.userName).append("\"}");
        }
        userListBuilder.append("]}");
        return userListBuilder.toString();
    }
    
    protected static void resendUserList() {
        String listUsersResponse = generateNewUserListResponse();
        Iterator<RemoteEndpoint> it = remoteControllers.values().iterator();
        while (it.hasNext()) {
            RemoteEndpoint controller = it.next();
            try {
                controller.sendString(listUsersResponse);
            } catch (Exception ex) {
                it.remove();
            }
        }
        it = remotePresenters.iterator();
        while (it.hasNext()) {
            RemoteEndpoint presenter = it.next();
            try {
                presenter.sendString(listUsersResponse);
            } catch (Exception ex) {
                it.remove();
            }
        }
    }
    
    protected static void resendDisplayList() {
        String listDisplaysResponse = getDisplayListResponse();
        Iterator<RemoteEndpoint> it = remotePresenters.iterator();
        while (it.hasNext()) {
            RemoteEndpoint presenter = it.next();
            try {
                presenter.sendString(listDisplaysResponse);
            } catch (Exception e) {
                it.remove();
            }
        }
    }
    
    protected static void deactivateUserToken(String token, String message) {
        try {
            RemoteEndpoint controllerEndpoint = tokenControllerMap.remove(token);
            if (controllerEndpoint != null) {
                controllerEndpoint.sendString("{\"responseType\":\"nav\",\"dest\":\"controller-login\"}");
                if (message != null) {
                    controllerEndpoint.sendString(getErrorResponse(message));
                }
            }
        } catch (Exception e) {
            reportBug(e);
        }
    }

    private static void sendFinalEmail(String userId) {
        UserContext context = userContextMap.get(userId);
        if (context != null && context.precacher != null) {
            //EmailUtils.sendEmail();
        }
    }
    
    protected void setGuestUser() {
        String salt = newSalt();
        String guestUser = ApplicationProperties.getGuestPersonId();
        userContextMap.put(guestUser, new UserContext(guestUser, "Guest account", null, hashPin("1234", salt), null, null));
    }
    
    protected static String newSalt() {
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    protected static String hashPin(String password, String salt) {
        return hashPin(password, salt, 10000);
    }
    
    protected static String hashPin(String password, String salt, int iterations) {
        try {
            char[] chars = password.toCharArray();
            byte[] saltBytes = Base64.decodeBase64(salt);
            
            PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, iterations, 64 * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return iterations + ":" + salt + ":" + Base64.encodeBase64String(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Failed to hash password", ex);
        }
    }
    
    protected static boolean validatePin(String pin, String hashedPin) {
        String[] parts = hashedPin.split(":");
        int iterations = Integer.parseInt(parts[0]);
        String salt = parts[1];
        
        String secondHash = hashPin(pin, salt, iterations);
        return secondHash.equals(hashedPin);
    }

    private void scheduleReload(RemoteEndpoint endpoint, int delay) {
        try {
            endpoint.sendString("{\"responseType\":\"scheduleReload\",\"delay\":"+(delay*1000)+"}");
        } catch (IOException ex) {
            // IGNORE
        }
    }
    
    protected static String getErrorResponse(String message) {
        return "{\"responseType\":\"error\",\"message\":\""+message+"\"}";
    }
    
    protected static String getPersonResponse(Person person) {
        return "{\"responseType\":\"person\",\"person\":"+GSON.toJson(person)+"}";
    }
    
    protected static String getPeopleResponse(List<Person> people) {
        return "{\"responseType\":\"people\",\"people\":"+GSON.toJson(people)+"}";
    }
    
    protected static String getImagesResponse(List<PersonImage> images) {
        return "{\"responseType\":\"images\",\"images\":"+GSON.toJson(images)+"}";
    }

    protected static String getStatisticsResponse(Statistics statistics) {
        return "{\"responseType\":\"stats\",\"stats\":"+GSON.toJson(statistics)+"}";
    }
    
    protected static String getIndentifyDisplayResponse() {
        return "{\"responseType\":\"identifyDisplay\"}";
    }
    
    protected static String getDisplayListResponse() {
        return "{\"responseType\":\"displays\",\"displays\":"+GSON.toJson(remoteDisplays.keySet())+"}";
    }
    
    protected static boolean isDisplayActive(String displayId) {
        boolean alreadyConnected = true;
        try {
            sendToDisplay(displayId, "{\"responseType\":\"pong\"}");
        } catch (IllegalStateException ex) {
            alreadyConnected = false; 
        }
        return alreadyConnected;
    }
    
    protected static String sendToDisplay(String id, String message) {
        RemoteEndpoint displayEndpoint = remoteDisplays.get(id);
        if (displayEndpoint != null) {
            try {
                logger.info("Sending '" + message + "' to display " + id);
                displayEndpoint.sendString(message);
            } catch (Exception e) {
                throw new IllegalStateException("failed to communicate with display " + id + ": " + e.getMessage(), e);
            }
        } else {
            throw new IllegalStateException("display not found '" + id + "'");
        }
        return OK_RESPONSE;
    }
    
    protected static String sendToController(String id, String message) {
        RemoteEndpoint controllerEndpoint = remoteControllers.get(id);
        if (controllerEndpoint != null) {
            try {
                logger.info("Sending '" + message + "' to controller " + id);
                controllerEndpoint.sendString(message);
            } catch (Exception e) {
                throw new IllegalStateException("failed to communicate with controller " + id + ": " + e.getMessage(), e);
            }
        } else {
            throw new IllegalStateException("controller not found '" + id + "'");
        }
        return OK_RESPONSE;
    }
    
    protected static Checklist newChecklist() {
        Checklist checklist = new Checklist();
        checklist.addOpenItem(new ChecklistItem("turn-on-screens", "Turn on screens"));
        checklist.addOpenItem(new ChecklistItem("turn-on-ipads", "Turn on iPads"));
        checklist.addOpenItem(new ChecklistItem("reset-server", "Reset server"));
        checklist.addCloseItem(new ChecklistItem("turn-off-screens", "Turn off screens"));
        checklist.addCloseItem(new ChecklistItem("turn-off-ipads", "Turn off iPads"));
        checklist.addCloseItem(new ChecklistItem("wipe-off-ipads", "Wipe off iPads"));
        checklist.addCloseItem(new ChecklistItem("plug-in-ipads", "Plug in iPads"));
        checklist.addCloseItem(new ChecklistItem("lock-cabinet", "Lock cabinet"));
        checklist.addCloseItem(new ChecklistItem("empty-trash", "Empty trash"));
        
        return checklist;
    }
    
    protected static void reportBug(Throwable t) {
        try (StringWriter writer = new StringWriter()) {
            t.printStackTrace(new PrintWriter(writer));
            String stackTrace = writer.toString();
            if (!stackTraces.contains(stackTrace)) {
                stackTraces.add(stackTrace);
                reportBug("system", stackTrace);
            }
        } catch (IOException e) {
            //DO NOTHING
        }
    }
    
    protected static void reportBug(String reporter, String reportBody) {
        
        String reportId = UUID.randomUUID().toString();

        logger.warn("Bug report " + reportId + " by " + reporter + ": " + reportBody);

        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("bugReports", true)))) {
            out.println(reportId);
            out.println(new DateTime());
            out.println(reporter);
            out.println(reportBody);
            out.println();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to record bug report " + reportId);
        }
    }
}