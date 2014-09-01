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
var messageListeners = [];

$(window).ready(function() {
    
    connection = new WebSocket('ws://' + window.location.host + '/remote-control/', ['soap', 'xmpp']);
    connection.onmessage = function(message) {
        for (var i = 0; i < messageListeners.length; i++) {
            messageListeners[i](message);
        }
        
        if (messageQueue.length > 0) {
            var nextMessage = messageQueue[0];
            messageQueue = messageQueue.slice(1, messageQueue.length);
            connection.send(nextMessage);
        } else {
            blocking = false;
        }
    };
    connection.onopen = function() {
        ping();
    };
});

var ws = {
    socketSend: function (message) {
        if (blocking) {
            messageQueue.push(message);
        } else {
            blocking = true;
            connection.send(message);
        }
    },
    addMessageListener: function (listener) {
        messageListeners.push(listener);
    }
};

function ping() {
    ws.socketSend("ping");
    setTimeout(ping, 60000);
}


