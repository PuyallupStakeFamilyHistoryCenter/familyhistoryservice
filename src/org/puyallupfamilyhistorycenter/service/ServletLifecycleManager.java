/*
 * Copyright (c) 2015, tibbitts
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
package org.puyallupfamilyhistorycenter.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jetty.server.Server;

/**
 * Manages the life-cycle of the server, allowing the server to be
 * restarted without terminating the JVM
 * 
 * @author tibbitts
 */
public class ServletLifecycleManager {
    private static Server server;
    
    private static final AtomicReference<LifecycleAction> action = new AtomicReference<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static Future<Void> future;
    
    public static void start() {
        future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) { 
                    server.join();
                    
                    // The server must have been stopped or killed; check for action
                    LifecycleAction currentAction = action.get();
                    switch (currentAction) {
                        case RESTART:
                            doRestart();
                            break;
                            
                        case TERMINATE:
                            return null;
                    }
                }
            }
        });
    }
    
    public static void restart() {
        interrupt(LifecycleAction.RESTART);
    }
    
    public static void stop() {
        interrupt(LifecycleAction.TERMINATE);
    }
    
    public static void join() {
        if (future == null) {
            throw new IllegalStateException("The server hasn't been started");
        }
        
        try {
            future.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException("Failed while waiting for server", ex);
        }
    }
    
    public static void setServer(Server server) {
        ServletLifecycleManager.server = server;
    }
    
    private static void interrupt(final LifecycleAction currentAction) {
        executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                
                try {
                    action.set(currentAction);
                    server.stop();
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to " + currentAction + " the server", ex);
                }
                
                return null;
            }
        });
    }
    
    private static void doRestart() {
        File launchServerScript = new File("run-server.sh");
        if (launchServerScript.exists()) {
            try {
                //This is pretty hacky, but it should be safe for now
                Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", launchServerScript.getCanonicalPath()});
                System.exit(0);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to run restart script '" + launchServerScript + "'", ex);
            }
        } else {
            SpringContextInitializer.resetContext();
        }
    }
}
