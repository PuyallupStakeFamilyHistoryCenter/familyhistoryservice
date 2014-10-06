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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.familysearch.api.client.memories.FamilySearchMemories;
import org.gedcomx.rs.client.PersonState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.rs.client.StateTransitionOption;
import org.puyallupfamilyhistorycenter.service.cache.CachingSource;
import org.puyallupfamilyhistorycenter.service.cache.FamilySearchPersonSource;
import org.puyallupfamilyhistorycenter.service.cache.InMemoryCache;
import org.puyallupfamilyhistorycenter.service.cache.Source;
import org.puyallupfamilyhistorycenter.service.models.Person;

/**
 *
 * @author tibbitts
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class FamilyHistoryCenterSocket {
    private static final Gson GSON = new Gson();

    private static final class AccessTokenInfo {
        public final String userName;
        public final String userId;
        public final String hashedPin;
        public final String accessToken;

        public AccessTokenInfo(String userId, String userName, String hashedPin, String accessToken) {
            this.userName = userName;
            this.userId = userId;
            this.hashedPin = hashedPin;
            this.accessToken = accessToken;
        }
    }
    
    private static final Map<String, RemoteEndpoint> remoteDisplays = new HashMap<>();
    private static final Map<String, RemoteEndpoint> remoteControllers = new HashMap<>();
    private static final Map<String, String> tokenUserIdMap = new HashMap<>();
    private static final Map<String, AccessTokenInfo> userIdAccessTokenMap = new LinkedHashMap<>();
    private static final SecureRandom rand;
    static {
        try {
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to create SecureRand instance", ex);
        }
    }
    
    //TODO: Wire via configuration (Spring?)
    private static final Source fsSource = new FamilySearchPersonSource(); //new MockPersonSource();
    //private static final Source fileSource = new CachingSource(fsSource, new FileCache<>(Person.class, new File("/tmp/fhc/person-cache"), TimeUnit.DAYS.toMillis(3)));
    private static final Source inMemorySource = new CachingSource(fsSource, new InMemoryCache<String, Person>());
    private static final PersonDao personCache = new PersonDao(inMemorySource);

    public FamilyHistoryCenterSocket() {
        String salt = newSalt();
        userIdAccessTokenMap.put("guest", new AccessTokenInfo("KW79-H8X", "Guest%20account", hashPin("1234", salt), null));
    }
    
    
//    private static final ScheduledExecutorService remoteEndpointCleanup = Executors.newScheduledThreadPool(1);
//    {
//        remoteEndpointCleanup.schedule(new Callable<Void>() {
//
//            @Override
//            public Void call() throws Exception {
//                Iterator<Map.Entry<String, RemoteEndpoint>> it = remoteDisplays.entrySet().iterator();
//                while(it.hasNext()) {
//                    Map.Entry<String, RemoteEndpoint> item = it.next();
//                    try {
//                        RemoteEndpoint re = item.getValue();
//                        re.sendString("pong");
//                    } catch (IOException ex) {
//                        it.remove();
//                    }
//                }
//                
//                return null;
//            }
//        
//        }, 1, TimeUnit.MINUTES);
//    }
    
    @OnWebSocketConnect
    public void handleConnection(Session session) throws IOException {
        session.getRemote().sendString("connected");
    }
    
    @OnWebSocketMessage
    public void handleMessage(Session session, String message) throws IOException, URISyntaxException {
        String response = "ok";
        try {
            Scanner scanner = new Scanner(message);
            scanner.useDelimiter(" ");
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
                    String userId = scanner.next();
                    String pin = scanner.next(); //TODO: This is pretty insecure
                    
                    AccessTokenInfo tokenInfo = userIdAccessTokenMap.get(userId);
                    if (tokenInfo != null && validatePin(pin, tokenInfo.hashedPin)) {
                        String token = Long.toHexString(rand.nextLong());
                        tokenUserIdMap.put(token, userId);
                        response = "token " + token + " " + tokenInfo.userName;
                    } else {
                        response = "Error: username and PIN do not match";
                    }
                    break;
                }

                case "get-images": {
                    String token = scanner.next();
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
                    String token = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    Person person = personCache.getPerson(personId, accessToken);
                    if (person != null) {
                        response = "person " + GSON.toJson(person).replaceAll(" ", "%20");
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "send-person": {
                    String token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    String accessToken = tokenToAccessToken(token);
                    
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(displayId);
                    if (displayEndpoint == null) {
                        response = "Error: token '" + token + "' has no attached display";
                        break;
                    }
                    
                    Person person = personCache.getPerson(personId, accessToken);
                    try {
                        displayEndpoint.sendString("person " + GSON.toJson(person).replaceAll(" ", "%20"));
                    } catch (IOException e) {
                        response = "Error: failed to send person '" + personId + "' to display " + displayId + ": " + e.getMessage();
                    }
                    
                    break;
                }
                
                case "get-family": {
                    String token = scanner.next();
                    String personId = scanner.next();
                    String lastPageId = null;
                    if (scanner.hasNext()) {
                        lastPageId = scanner.next();
                    }
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    //TODO: Actually get family
                    Iterator<Person> it = personCache.traverseImmediateFamily(personId, 10, lastPageId, accessToken);
                    if (it != null && it.hasNext()) {
                        response = "family [" + toString(it).replaceAll(" ", "%20") + "]";
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "get-ancestors": {
                    String token = scanner.next();
                    String personId = scanner.next();
                    String paginationKey = null;
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    //TODO: Actually get family
                    Person person = personCache.getPerson(personId, accessToken);
                    if (person != null) {
                        response = "family [" + GSON.toJson(person) + "]";
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "get-descendents": {
                    String token = scanner.next();
                    String personId = scanner.next();
                    String paginationKey = null;
                    String accessToken = tokenToAccessToken(token);
                    
                    //TODO: Check token
                    //TODO: Actually get descendents
                    Person person = personCache.getPerson(personId, accessToken);
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
                        while (scanner.hasNext()) {
                            builder.append(scanner.next()).append(" ");
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

                case "access-token": {
                    String userId = scanner.next();
                    String userName = scanner.next();
                    String salt = newSalt();
                    String pin = hashPin(scanner.next(), salt);
                    String accessToken = scanner.next();

                    FamilySearchFamilyTree tree = new FamilyHistoryFamilyTree(true)
                            .authenticate(accessToken);

                    StateTransitionOption[] options = new StateTransitionOption[] {
                        //new QueryParameter(cmd, value)
                    };
                    PersonState person = tree.readPersonForCurrentUser(options);

                    if (!person.getSelfUri().getPath().endsWith(userId)) {
                        response = "Error: Access token does not match userId";
                        break;
                    }

                    userIdAccessTokenMap.put(userId, new AccessTokenInfo(userId, userName, pin, accessToken));
                    break;
                }

                case "list-current-users": {
                    StringBuilder userListBuilder = new StringBuilder("user-list");
                    for (AccessTokenInfo ati : userIdAccessTokenMap.values()) {
                        userListBuilder
                                .append(" ").append(ati.userName)
                                .append(" ").append(ati.userId);
                    }
                    response = userListBuilder.toString();
                    break;
                }

                case "move-left":
                case "move-right":
                case "move-up":
                case "move-down":
                case "zoom-in":
                case "zoom-out": {
                    String id = scanner.next();
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(id);
                    if (displayEndpoint != null) {
                        try {
                            displayEndpoint.sendString(cmd);
                        } catch (IOException e) {
                            response = "Error: failed to communicate with display " + id + ": " + e.getMessage();
                        }
                    } else {
                        response = "Error: display not found '" + id + "'";
                    }
                    break;
                }


                default:
                    response = "Error: unrecognized command '" + message + "'";
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            response = "Error: " + e.getMessage();
        }
        if (session.isOpen()) {
            session.getRemote().sendString(response);
        }
    }
    
    private String tokenToAccessToken(String token) {
        String userId = tokenUserIdMap.get(token);

        if (userId == null) {
            throw new IllegalStateException("Invalid token");
        }

        AccessTokenInfo accessTokenInfo = userIdAccessTokenMap.get(userId);
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
    
    protected static String newSalt() {
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        return Base64.encodeBase64String(salt);
    }

    protected static String hashPin(String password, String salt) {
        return hashPin(password, salt, 1000);
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
