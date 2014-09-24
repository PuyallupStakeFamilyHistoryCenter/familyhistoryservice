/*
 * Copyright (c) 2014, tibbitts
 * All rights reserved.
 
                    @Override
                    public void apply(ClientRequest request) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                }
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.familysearch.api.client.memories.FamilySearchMemories;
import org.gedcomx.conclusion.Relationship;
import org.gedcomx.rs.client.PersonState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.rs.client.StateTransitionOption;

/**
 *
 * @author tibbitts
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class FamilyHistoryCenterSocket {
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
    
    private static final Map<String, RemoteEndpoint> remoteDisplays = new HashMap<>();
    private static final Map<String, RemoteEndpoint> remoteControllers = new HashMap<>();
    private static final Map<String, String> tokenDisplayMap = new HashMap<>();
    private static final Map<String, String> tokenUserIdMap = new HashMap<>();
    private static final Map<String, AccessTokenInfo> accessTokenMap = new LinkedHashMap<>();
    private static final Random rand = new SecureRandom();

    public FamilyHistoryCenterSocket() {
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

                case "get-ancestors": {
                    String token = scanner.next();
                    String userId = tokenUserIdMap.get(token);

                    response = "ancestor {\"first_name\":\"John\",\"last_name\":\"Smith\",\"birth\":\"1765\",\"death\":\"1824\"}";

                    break;
                }

                case "ancestry-stop": {
                    String token = scanner.next();


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
            response = "Error: " + e.getMessage();
        }
        if (session.isOpen()) {
            session.getRemote().sendString(response);
        }
    }
}
