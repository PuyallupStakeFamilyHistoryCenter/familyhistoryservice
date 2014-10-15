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

import com.google.gson.Gson;
import org.puyallupfamilyhistorycenter.service.cache.PersonDao;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.familysearch.api.client.memories.FamilySearchMemories;
import org.gedcomx.rs.client.PersonState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.rs.client.StateTransitionOption;
import org.puyallupfamilyhistorycenter.service.SpringContextInitializer;
import org.puyallupfamilyhistorycenter.service.cache.Precacher;
import org.puyallupfamilyhistorycenter.service.models.Person;

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
    static {
        personDao = (PersonDao) SpringContextInitializer.getContext().getBean("person-dao");
        appKeyConfig = (AppKeyConfig) SpringContextInitializer.getContext().getBean("app-key-config");
    }
    

    private static final class UserContext {
        public final String userName;
        public final String userId;
        public final String hashedPin;
        public final String accessToken;
        public long lastUsed;
        public final Set<String> tokens; //TODO: Rename to prevent confusion with access token
        public final Precacher precacher;
        public UserContext(String userId, String userName, String hashedPin, String accessToken, Precacher precacher) {
            this.userName = userName;
            this.userId = userId;
            this.hashedPin = hashedPin;
            this.accessToken = accessToken;
            this.lastUsed = System.currentTimeMillis();
            this.tokens = new HashSet<>();
            this.precacher = precacher;
        }
    }
    
    private static final long tokenInactivityTimeout = TimeUnit.MINUTES.toMillis(1); //TODO: Reset this
    private static final long userInactivityTimeout  = TimeUnit.MINUTES.toMillis(2);
    
    private static final Map<String, RemoteEndpoint> remoteDisplays = new HashMap<>();
    private static final Map<String, RemoteEndpoint> remoteControllers = new HashMap<>();
    private static final Map<String, RemoteEndpoint> tokenControllerMap = new HashMap<>();
    private static final Map<String, String> tokenUserIdMap = new HashMap<>();
    private static final Map<String, Long> tokenLastUse = new HashMap<>();
    private static final Map<String, UserContext> userContextMap = new LinkedHashMap<>();
    private static final SecureRandom rand;
    static {
        try {
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to create SecureRand instance", ex);
        }
    }
    
    
    private static final ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);
    static {
        cleanupService.schedule(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Iterator<Map.Entry<String, Long>> it = tokenLastUse.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Long> entry = it.next();
                    if (entry.getValue() + tokenInactivityTimeout < System.currentTimeMillis()) {
                        String token = entry.getKey();
                        try {
                            deactivateUserToken(token);
                        } catch (IOException ex) {
                            logger.error("Failed to delete token " + token + " controller has probably already disconnected", ex);
                        }
                        it.remove();
                    }
                }
                
                return null;
            }
        
        }, 1, TimeUnit.MINUTES);
        cleanupService.schedule(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Iterator<Map.Entry<String, UserContext>> it = userContextMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, UserContext> entry = it.next();
                    if (entry.getValue().lastUsed + userInactivityTimeout < System.currentTimeMillis()) {
                        String userId = entry.getKey();
                        // TODO: Figure out how to invalidate access token
                        String accessToken = userContextMap.remove(userId).accessToken;
                        
                        it.remove();
                    }
                }
                
                resendUserListToControllers();
                
                return null;
            }
        
        }, 1, TimeUnit.MINUTES);
    }

    public FamilyHistoryCenterSocket() {
        String salt = newSalt();
        userContextMap.put("KW79-H8X", new UserContext("KW79-H8X", "Guest%20account", hashPin("1234", salt), null, null));
    }
    
    @OnWebSocketConnect
    public void handleConnection(Session session) throws IOException {
        session.getRemote().sendString("connected");
    }
    
    @OnWebSocketMessage
    public void handleMessage(Session session, String message) throws IOException, URISyntaxException {
        logger.debug("Got websocket request '" + message + "'");
        String response = "ok";
        try {
            Scanner scanner = new Scanner(message);
            scanner.useDelimiter(" ");
            String token = null;
            String userId = null;
            String cmd = scanner.next();
            switch (cmd) {
                case "ping":
                    response = "pong";
                    break;

                case "controller": {
                    String id = scanner.next();
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(id);
                    if (displayEndpoint == null) {
                        response = "Error: display not found '" + id + "'";
                        break;
                    }
                    boolean alreadyConnected = false;
                    if (remoteControllers.containsKey(id)) {
                        try {
                            remoteControllers.get(id).sendString("pong");
                            alreadyConnected = true;
                        } catch (IOException ex) {
                            //DO NOTHING
                        }
                    }
                    if (!alreadyConnected) {
                        remoteControllers.put(id, session.getRemote());
                        response = "attached";
                    } else {
                        session.getRemote().sendString("nav controller-attach");
                        response = "Error: display " + id + " is already connected to another controller";
                    }
                    break;
                }

                case "display": {
                    String id = scanner.next();
                    boolean alreadyConnected = false;
                    if (remoteDisplays.containsKey(id)) {
                        try {
                            remoteDisplays.get(id).sendString("pong");
                            alreadyConnected = true;
                        } catch (IOException ex) {
                            //DO NOTHING
                        }
                    }
                    if (!alreadyConnected) {
                        remoteDisplays.put(id, session.getRemote());
                        response = "standby";
                    } else {
                        response = "Error: display " + id + " is already connected";
                    }
                    break;
                }

                case "display-name": {
                    String id = Integer.toHexString(rand.nextInt());
                    response = "name " + id;
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
                        response = "token " + token + " " + tokenInfo.userName;
                    } else {
                        response = "Error: username and PIN do not match";
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

                case "get-images": {
                    token = scanner.next();
                    String ancestorId = scanner.next();
                    String accessToken = tokenToAccessToken(token);

                    FamilySearchMemories memories = new FamilyHistoryMemories(true)
                            .authenticate(accessToken);


                    StateTransitionOption[] options = new StateTransitionOption[] {
                        //new QueryParameter(cmd, value)
                    };
                    PersonState person = memories.readPerson(new URI("http://gedcomx.org/Person/KWWQ-NCT"), options);
                    person.loadMediaReferences(options);

                    SourceDescriptionsState ref = person.readArtifacts(options);

                    break;
                }
                
                case "get-person": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    Person person = personDao.getPerson(personId, accessToken);
                    if (person != null) {
                        response = "person " + GSON.toJson(person).replaceAll(" ", "%20");
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "send-person": {
                    token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(displayId);
                    if (displayEndpoint == null) {
                        response = "Error: token '" + token + "' has no attached display";
                        break;
                    }
                    
                    Person person = personDao.getPerson(personId, accessToken);
                    try {
                        displayEndpoint.sendString("person " + GSON.toJson(person).replaceAll(" ", "%20"));
                    } catch (IOException e) {
                        response = "Error: failed to send person '" + personId + "' to display " + displayId + ": " + e.getMessage();
                    }
                    
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
                    
                    //TODO: Check token
                    //TODO: Actually get family
                    Iterator<Person> it = personDao.traverseImmediateFamily(personId, 10, lastPageId, accessToken);
                    if (it != null && it.hasNext()) {
                        response = "family [" + toString(it).replaceAll(" ", "%20") + "]";
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "get-ancestors": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String paginationKey = null;
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    //TODO: Actually get family
                    Person person = personDao.getPerson(personId, accessToken);
                    if (person != null) {
                        response = "family [" + GSON.toJson(person) + "]";
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "get-descendents": {
                    token = scanner.next();
                    String personId = scanner.next();
                    String paginationKey = null;
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Actually get descendents
                    Person person = personDao.getPerson(personId, accessToken);
                    if (person != null) {
                        response = "family [" + GSON.toJson(person) + "]";
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "send": {
                    String id = scanner.next();
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(id);
                    if (displayEndpoint != null) {
                        StringBuilder builder = new StringBuilder();
                        boolean isFirst = true;
                        while (scanner.hasNext()) {
                            if (isFirst) {
                                isFirst = false;
                            } else {
                                builder.append(" ");
                            }
                            builder.append(scanner.next());
                        }
                        
                        try {
                            displayEndpoint.sendString(builder.toString());
                        } catch (IOException e) {
                            response = "Error: failed to communicate with display " + id + ": " + e.getMessage();
                        }
                    } else {
                        response = "Error: display not found '" + id + "'";
                    }
                    break;
                }

                case "nav": {
                    String id = scanner.next();
                    String dest = scanner.next();
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(id);
                    if (displayEndpoint != null) {
                        try {
                            displayEndpoint.sendString("nav " + dest);
                        } catch (IOException e) {
                            response = "Error: failed to communicate with display " + id + ": " + e.getMessage();
                        }
                    } else {
                        response = "Error: display not found '" + id + "'";
                    }
                    break;
                }
                
                case "get-app-key": {
                    response = "app-key " + appKeyConfig.appKey + " " + appKeyConfig.environment;
                    
                    break;
                }

                case "access-token": {
                    userId = scanner.next();
                    String userName = scanner.next();
                    String salt = newSalt();
                    String pin = hashPin(scanner.next(), salt);
                    String accessToken = scanner.next();

                    FamilySearchFamilyTree tree = new FamilyHistoryFamilyTree(true)
                            .authenticate(accessToken);

                    PersonState person = tree.readPersonForCurrentUser();

                    if (!person.getSelfUri().getPath().endsWith(userId)) {
                        response = "Error: Access token does not match userId";
                        break;
                    }
                    
                    Precacher precacher = new Precacher(accessToken);
                    precacher.precache();
                    
                    userContextMap.put(userId, new UserContext(userId, userName, pin, accessToken, precacher));
                    
                    resendUserListToControllers();
                    break;
                }

                case "destroy-access-token": {
                    userId = scanner.next();
                    String pin = scanner.next(); //TODO: This is pretty insecure
                    
                    UserContext userContext = userContextMap.get(userId);
                    if (userContext != null && validatePin(pin, userContext.hashedPin)) {
                        // TODO: Revoke access token
                        userContextMap.remove(userId);
                        for (String t : userContext.tokens) {
                            deactivateUserToken(t);
                        }
                        userId = null;
                        
                        if (userContext.precacher != null) {
                            userContext.precacher.cancel();
                        }
                        
                        resendUserListToControllers();
                    } else {
                        response = "Error: username and PIN do not match";
                    }
                    break;
                }

                case "list-current-users": {
                    response = generateNewUserListResponse();
                    break;
                }

                default:
                    response = "Error: unrecognized command '" + message + "'";
            }
            
            if (token != null) {
                tokenLastUse.put(token, System.currentTimeMillis());
            }
            if (userId != null) {
                userContextMap.get(userId).lastUsed = System.currentTimeMillis();
            }
        } catch (Throwable e) {
            logger.error("Unexpected exception: " + e, e);
            response = "Error: " + e.getMessage();
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
        StringBuilder userListBuilder = new StringBuilder("user-list");
        for (UserContext ati : userContextMap.values()) {
            userListBuilder
                    .append(" ").append(ati.userName)
                    .append(" ").append(ati.userId);
        }
        return userListBuilder.toString();
    }
    
    protected static void resendUserListToControllers() {
        String listUsersResponse = generateNewUserListResponse();
        Iterator<Map.Entry<String, RemoteEndpoint>> it = remoteControllers.entrySet().iterator();
        while (it.hasNext()) {
            RemoteEndpoint controller = it.next().getValue();
            try {
                controller.sendString(listUsersResponse);
            } catch (IOException ex) {
                it.remove();
            }
        }
    }
    
    protected static void deactivateUserToken(String token) throws IOException {
        RemoteEndpoint controllerEndpoint = tokenControllerMap.remove(token);
        controllerEndpoint.sendString("nav controller-login");
        controllerEndpoint.sendString("Error: logged out due to inactivity");
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
}
