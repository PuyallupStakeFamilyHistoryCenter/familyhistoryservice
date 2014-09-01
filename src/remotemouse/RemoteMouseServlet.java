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

package remotemouse;


import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import org.eclipse.jetty.websocket.server.WebSocketHandler;


/**
 *
 * @author tibbitts
 */
public class RemoteMouseServlet {
    
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, Exception {
        WebSocketHandler mouseHandler = new WebSocketHandler.Simple(RemoteMouseSocket.class);
        ContextHandler mouseContext = new ContextHandler("/remote-control");
        mouseContext.setHandler(mouseHandler);
        mouseContext.setBaseResource(Resource.newResource(""));
        
        ContextHandler indexContext = new ContextHandler("/static-content");
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setBaseResource(Resource.newClassPathResource("static-content"));
        resourceHandler.setCacheControl("private, max-age=0, no-cache");
        indexContext.setHandler(resourceHandler);
        
        ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        handlerCollection.setHandlers(new Handler[]{mouseContext, indexContext});
        
        Server server = new Server(8080);
        server.setHandler(handlerCollection);
        server.start();
        server.join();
    }
}