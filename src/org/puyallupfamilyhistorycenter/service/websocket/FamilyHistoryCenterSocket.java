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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

/**
 *
 * @author tibbitts
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class FamilyHistoryCenterSocket {
    private static final class AccessTokenInfo {
        public final String userId;
        public final int pin;
        public final String accessToken;

        public AccessTokenInfo(String userId, int pin, String accessToken) {
            this.userId = userId;
            this.pin = pin;
            this.accessToken = accessToken;
        }
    }
    
    private static final Map<String, RemoteEndpoint> remoteDisplays = new HashMap<>();
    private static final Map<String, RemoteEndpoint> remoteControllers = new HashMap<>();
    private static final Map<String, String> tokenDisplayMap = new HashMap<>();
    private static final Map<String, String> tokenUserIdMap = new HashMap<>();
    private static final Map<String, AccessTokenInfo> accessTokenMap = new HashMap<>();
    private static final Random rand = new Random();
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
    public void handleMessage(Session session, String message) throws IOException {
        Scanner scanner = new Scanner(message);
        String cmd = scanner.next();
        String response = "ok";
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
                String userId = scanner.next().toLowerCase();
                int pin = scanner.nextInt(); //TODO: This is pretty insecure
                
                if (accessTokenMap.containsKey(userId) && accessTokenMap.get(userId).pin == pin) {
                    String token = Long.toHexString(rand.nextLong());
                    tokenUserIdMap.put(token, userId);
                    response = "token " + token;
                } else {
                    response = "Error: username and password are not correct";
                }
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
                int pin = scanner.nextInt();
                String accessToken = scanner.next();
                
                accessTokenMap.put(userId, new AccessTokenInfo(userId, pin, accessToken));
                break;
            }

            default:
                response = "Error: unrecognized command '" + message + "'";
        }
        if (session.isOpen()) {
            session.getRemote().sendString(response);
        }
    }
}
