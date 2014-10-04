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
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.familysearch.api.client.memories.FamilySearchMemories;
import org.gedcomx.rs.client.PersonState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.rs.client.StateTransitionOption;
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
        public final String pin;
        public final String accessToken;

        public AccessTokenInfo(String userId, String userName, String pin, String accessToken) {
            this.userName = userName;
            this.userId = userId;
            this.pin = pin;
            this.accessToken = accessToken;
        }
    }
    
    private final Map<String, RemoteEndpoint> remoteDisplays = new HashMap<>();
    private final Map<String, RemoteEndpoint> remoteControllers = new HashMap<>();
    private final Map<String, String> tokenDisplayMap = new HashMap<>();
    private final Map<String, String> tokenUserIdMap = new HashMap<>();
    private final Map<String, AccessTokenInfo> accessTokenMap = new LinkedHashMap<>();
    private final Random rand = new SecureRandom();
    private final PersonDao personCache;

    public FamilyHistoryCenterSocket(PersonDao personCache) {
        this.personCache = personCache;
        accessTokenMap.put("guest", new AccessTokenInfo("guest", "Guest%20account", "1234", null));
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
                        session.getRemote().sendString("nav controller-attach.html");
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
                    
                    AccessTokenInfo tokenInfo = accessTokenMap.get(userId);
                    if (tokenInfo != null && tokenInfo.pin.equals(pin)) {
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
                    String userId = tokenUserIdMap.get(token);

                    if (userId == null) {
                        response = "Error: invalid token";
                        break;
                    }

                    AccessTokenInfo accessTokenInfo = accessTokenMap.get(userId);
                    if (accessTokenInfo == null) {
                        response = "Error: invalid token";
                        break;
                    }

                    FamilySearchMemories memories = new FamilyHistoryMemories(true)
                            .authenticate(accessTokenInfo.accessToken);


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
                    
                    //TODO: Check token
                    Person person = personCache.getPerson(personId);
                    if (person != null) {
                        response = "person " + GSON.toJson(person);
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "send-person": {
                    String token = scanner.next();
                    String displayId = scanner.next();
                    String personId = scanner.next();
                    
                    RemoteEndpoint displayEndpoint = remoteDisplays.get(displayId);
                    if (displayEndpoint == null) {
                        response = "Error: token '" + token + "' has no attached display";
                        break;
                    }
                    
                    try {
                        displayEndpoint.sendString("person " + personCache.getPerson(personId));
                    } catch (IOException e) {
                        response = "Error: failed to send person '" + personId + "' to display " + displayId + ": " + e.getMessage();
                    }
                    
                    break;
                }
                
                case "get-family": {
                    String token = scanner.next();
                    String personId = scanner.next();
                    String lastPageId = scanner.next();
                    
                    //TODO: Check token
                    //TODO: Actually get family
                    Person person = personCache.getPerson(personId);
                    if (person != null) {
                        response = "family [" + GSON.toJson(person) + "]";
                    } else {
                        response = "Error: person " + personId + " not found";
                    }
                    
                    break;
                }
                
                case "get-ancestors": {
                    String token = scanner.next();
                    String personId = scanner.next();
                    String paginationKey = null;
                    
                    //TODO: Check token
                    //TODO: Actually get family
                    Person person = personCache.getPerson(personId);
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
                    
                    //TODO: Check token
                    //TODO: Actually get descendents
                    Person person = personCache.getPerson(personId);
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
                    String pin = scanner.next();
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

                    accessTokenMap.put(userId, new AccessTokenInfo(userId, userName, pin, accessToken));
                    break;
                }

                case "list-current-users": {
                    StringBuilder userListBuilder = new StringBuilder("user-list");
                    for (AccessTokenInfo ati : accessTokenMap.values()) {
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
}
