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

var messageQueue = [];
var connection;
var blocking = true;
var isOpen = false;
var messageListeners = [];
var pingTimeout;

var ws = {
    close: function() {
        console.info("Closing web socket");
        if (pingTimeout) clearTimeout(pingTimeout);
        if (connection) connection.close();
        connection = null;
        messageListeners = [];
        messageQueue = [];
        isOpen = false;
    },
    connect: function(endpoint) {
        if (connection) {
            throw new Error("Already connected");
            //return;
        }
        
        console.info("Connecting web socket");
        if (!endpoint) {
            endpoint = window.location.host;
        }
        blocking = true;
        connection = new WebSocket('ws://' + endpoint + '/remote-control/', ['soap', 'xmpp']); //TODO: Use secure web sockets (need certificate)
        
        connection.onmessage = function(message) {
            if (!isOpen) return;
            
            console.info("Got message '" + message.data + "' from web socket");
            for (var i = 0; i < messageListeners.length; i++) {
                messageListeners[i](message);
            }
            drainQueue();
        };
        connection.onopen = function() {
            console.info("Web socket open");
            isOpen = true;
            ping();
            drainQueue();
        };
        function drainQueue() {
            if (!connection) {
                return;
            }
            
            if (messageQueue.length > 0) {
                var nextMessage = messageQueue[0];
                messageQueue = messageQueue.slice(1, messageQueue.length);
                console.info("Sending message '" + nextMessage + "' from queue");
                connection.send(nextMessage);
            } else {
                console.info("No messages waiting to send");
                blocking = false;
            }
        }
        function ping() {
            ws.socketSend("ping");
            pingTimeout = setTimeout(ping, 60000);
        }
        $(window).unload(function() {
            close();
        });
    },
    socketSend: function (message) {
        if (blocking) {
            console.info("Queueing message '" + message + "'");
            messageQueue.push(message);
        } else {
            console.info("Sending message '" + message + "'");
            blocking = true;
            connection.send(message);
        }
    },
    addMessageListener: function (listener) {
        messageListeners.push(listener);
    }
};
