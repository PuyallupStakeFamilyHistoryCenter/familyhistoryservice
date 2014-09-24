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



var content;
var mode;
var settings = {
    global: {
        verbs: {
            connected: doNothing,
            ok: doNothing,
            pong: doNothing
        }
    },
    page: {verbs:{}},
    local: {verbs:{}}
}
var displayName;
var token;
var userName;
var urlVars = {};

function doNothing() {}


function getReady() {
    //Add message listener to websocket
    ws.addMessageListener(messageHandler);
    ws.connect();
    
    content = $("#content");
    mode = getParameterByName("mode");
    if (!mode) {
        logger.error("Mode parameter is required");
        return;
    }

    switch (mode) {
        case "clear-display-name":
            $.removeCookie("display-name");
            return;

        default:
            settings.page = defaultSettings[mode];
            break;
    }
    
    if (!settings.page) {
        logger.error("Invalid type mode '" + mode + "'");
        return;
    }

    setup();

    if (settings.page.getNewDisplayName) {
        displayName = $.cookie("display-name");
        if (!displayName) {
            settings.page.getNewDisplayName();
        } else {
            settings.page.verbs.name(["name",displayName]);
        }
    } else if (settings.page.begin) {
        settings.page.begin();
    }
}

function setup() {
    document.title = settings.page.title;
    var pageHeader = $("#page-header"); //TODO: This isn't working
    pageHeader.html("<h1>" + settings.page.header + "</h1>");
    if (settings.page.contentPadding) {
        $("#messages").addClass("content-padding");
    } else {
        $("#messages").removeClass("content-padding");
    }
}

var defaultSettings = {
    display: {
        title: "Display",
        header: '<img src="logo.png<ERROR>" alt="Puyallup Family History Center logo" />',
        contentPadding: false,
        verbs: {
            nav: function(parts) {
                navigate(parts[1]);
            },
            standby: function(parts) {
                navigate("display-ready");
            },
            name: function(parts) {
                displayName = parts[1];
                $.cookie("display-name", displayName);
                ws.socketSend("display " + displayName);
            }
        },
        getNewDisplayName: function() {
            ws.socketSend("display-name");
        }
    },
    controller: {
        title: "Controller",
        header: '',
        contentPadding: true,
        verbs: {
            attached: function(parts) {
                $.cookie("display-name", displayName);
                ws.socketSend("nav " + displayName + " " + "display-login");
                token = $.cookie("token");
                if (!token) {
                    navigate("controller-login");
                } else {
                    settings.page.verbs.token(token);
                }
            },
            name: function(parts) {
                settings.page.gotNewDisplayName(parts[1]);
            },
            token: function(parts) {
                var type = Object.toType(parts);
                switch (type) {
                    case "array":
                        token = parts[1];
                        break;
                    case "string":
                        token = parts;
                        break;
                }
                $.cookie("token", token);
                ws.socketSend("nav " + displayName + " " + "display-main");
                navigate("controller-main");
                ws.socketSend("get-ancestors " + token);
            }
        },
        gotNewDisplayName: function(name) {
            displayName = name;
            ws.socketSend("controller " + displayName);
        },
        getNewDisplayName: function() {
            navigate("controller-attach");
        },
        getToken: function(userId, pin) {
            ws.socketSend("login " + userId + " " + pin);
        },
        logOut: function() {
            //TODO: This doesn't appear to be working
            if (!$.removeCookie("token")) {
                logger.error("Failed to remove login cookie");
            }
            ws.socketSend("nav " + displayName + " display-login");
            navigate("controller-login");
        },
    },
    kiosk: {
        title: "Kiosk",
        header: '<img src="logo.png<ERROR>" alt="Puyallup Family History Center logo" />',
        contentPadding: false,
        verbs:{},
        begin: function() {
            navigate("kiosk-main");
        }, 
        gotAccessToken: function(userId, userName, pin, accessToken) {
            ws.socketSend("access-token " + userId + " " + encodeURI(userName) + " " + pin + " " + accessToken);
        }
    }
};

function messageHandler(message) {
    $("#messages").html("");
    
    var parts = message.data.split(" ");
    // TODO: Incorporate these verbs into the settings.page object if possible
    // so that the different screens can define which verbs they accept and
    // how to handle each one
    if (settings.local.verbs[parts[0]]) {
        settings.local.verbs[parts[0]](parts);
    } else if (settings.page.verbs[parts[0]]) {
        settings.page.verbs[parts[0]](parts);
    } else if (settings.global.verbs[parts[0]]) {
        settings.global.verbs[parts[0]](parts);
    } else {
        if (parts[0] === "Error:") {
            logger.error(message.data.substring(6));
        } else {
            logger.error("unrecognized command '" + message.data + "'");
        }
    }
    return;
}

var logger = {
    error: function (message) {
        log("danger", "<strong>Error:</strong> " + message);
    },

    warn: function (message) {
        log("warning", "<strong>Warning:</strong> " + message);
    },

    info: function (message) {
        log("info", "<strong>Info:</strong> " + message);
    },

    success: function (message) {
        log("success", message);
    }
}

function log(level, message) {
    //TODO: Add timeout parameter for messages to disappear
    $("#messages").append('<div class="alert alert-' + level + ' alert-dismissible" role="alert">'+
            '<button type="button" class="close" data-dismiss="alert"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>' + 
            message + '</div>');
}

function navigate(dest) {
    var split = dest.split("?");
    var actualDest = split[0] + ".html";
    if (split.length > 1) {
        actualDest += "?" + split[1];
    }
    var tempVars = getUrlVars(split[1]);
    var deferred = new $.Deferred();
    
    $.ajax(actualDest)
        .done(
            function(data) {
                settings.local = {verbs: {}};
                content.html(data);
                urlVars = tempVars;
                deferred.resolve();
            }
        ).fail(
            function(response) {
                logger.error("Could not load " + actualDest + " - " + response.statusText);
                deferred.reject();
            }
        );
    return deferred.promise();
}

function navigateDisplay(dest) {
    ws.socketSend("nav " + displayName + " " + dest);
}

// Read a page's GET URL variables and return them as an associative array.
// Courtesy of http://jquery-howto.blogspot.com/2009/09/get-url-parameters-values-with-jquery.html
function getUrlVars(hashes)
{
    if (urlVars) {
        return urlVars;
    }
    
    var vars = [], hash;
    if (!hashes) {
        hashes = window.location.search.substring(1).split('&');
    }   
    for(var i = 0; i < hashes.length; i++)
    {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        if (hash.length === 2) {
            vars[hash[0]] = decodeURIComponent(hash[1]);
        } else {
            vars[hash[0]] = true;
        }
    }
    urlVars = vars;
    return vars;
}

function getParameterByName(name) {
    return getUrlVars()[name];
}

Object.toType = (function toType(global) {
  return function(obj) {
    if (obj === global) {
      return "global";
    }
    return ({}).toString.call(obj).match(/\s([a-z|A-Z]+)/)[1].toLowerCase();
  }
})(this)