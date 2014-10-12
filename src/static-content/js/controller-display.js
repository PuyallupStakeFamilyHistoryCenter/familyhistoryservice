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
            pong: doNothing,
            nav: function(parts) {
                navigate(parts[1]);
            },
            "user-list": doNothing
        }
    },
    page: {verbs:{}},
    local: {verbs:{}}
};
var displayName;
var token;
var userName;

function doNothing() {}


function getReady() {
    urlVars = getUrlVars(window.location.search.substr(1));
    
    //Add message listener to websocket
    ws.addMessageListener(messageHandler);
    ws.connect("localhost:8443");
    
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
            console.info("Selected mode " + mode);
            settings.page = defaultSettings[mode];
            break;
    }
    
    if (!settings.page) {
        logger.error("Invalid type mode '" + mode + "'");
        return;
    }

    setup();

    if (settings.page.begin) {
        settings.page.begin();
    }
}

function setup() {
    document.title = settings.page.title;
    var pageHeader = $("#page-header");
    if (settings.page.header) {
        pageHeader.html("<h1>" + settings.page.header + "</h1>");
    } else if (settings.page.headerFile) {
        $.ajax("fragments/"+ settings.page.headerFile)
        .done(function(data) {
            pageHeader.html(data);
        }).fail(function() {
            logger.error("Failed to load header from " + settings.page.headerFile);
        });
    }
    if (settings.page.contentPadding) {
        $("#messages").addClass("content-padding");
    } else {
        $("#messages").removeClass("content-padding");
    }
}

function getDisplayName() {
    console.info("Getting display name from cookie");
    displayName = $.cookie("display-name");
    if (!displayName) {
        console.info("No display name cookie found; getting display name assignment");
        settings.page.getNewDisplayName();
    } else {
        console.info("Found display name " + displayName);
        settings.page.verbs.name(["name",displayName]);
    }
}

var defaultSettings = {
    display: {
        title: "Display",
        header: '<img src="logo.png" alt="Puyallup Family History Center logo" />',
        contentPadding: false,
        verbs: {
            standby: function(parts) {
                navigate("display-ready");
            },
            name: function(parts) {
                displayName = parts[1];
                $.cookie("display-name", displayName);
                ws.socketSend("display " + displayName);
            }
        },
        begin: getDisplayName,
        getNewDisplayName: function() {
            ws.socketSend("display-name");
        }
    },
    controller: {
        title: "Controller",
        header: '',
        headerFile: 'controller-header.html',
        contentPadding: true,
        verbs: {
            attached: function(parts) {
                $.cookie("display-name", displayName);
                console.info("Found token '" + token + "'");
                ws.socketSend("nav " + displayName + " " + "display-login");
                navigate("controller-login");
            },
            name: function(parts) {
                settings.page.gotNewDisplayName(parts[1]);
            },
            token: function(parts) {
                setUsername(userName);
                token = parts[1];
                $.cookie("token", token);
                ws.socketSend("nav " + displayName + " " + "display-main");
                clearHistory();
                navigate("controller-main");
            }
        },
        begin: getDisplayName,
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
            ws.socketSend("logout " + token);
            token = null;
            ws.socketSend("nav " + displayName + " display-login");
            navigate("controller-login");
        },
        destroyAccessToken: function(userId, pin) {
            ws.socketSend("destroy-access-token " + userId + " " + pin);
        }
    },
    kiosk: {
        title: "Kiosk",
        header: '<img src="logo.png" alt="Puyallup Family History Center logo" />',
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
    if (settings.local.verbs[parts[0]]) {
        settings.local.verbs[parts[0]](parts);
    } else if (settings.page.verbs[parts[0]]) {
        settings.page.verbs[parts[0]](parts);
    } else if (settings.global.verbs[parts[0]]) {
        settings.global.verbs[parts[0]](parts);
    } else {
        var errorMessage;
        if (parts[0] === "Error:") {
            errorMessage = message.data.substring(6);
        } else {
            errorMessage = "unrecognized command '" + message.data + "'";
        }
        logger.error(errorMessage);
        //throw new Error(errorMessage);
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
    var actualDest = "fragments/" + split[0] + ".html";
    var tempVars = null;
    if (split.length > 1) {
        actualDest += "?" + split[1];
        tempVars = getUrlVars(split[1]);
    }
    var deferred = new $.Deferred();
    
    console.info("Navigating to " + actualDest);
    $.ajax(actualDest)
        .done(
            function(data) {
                settings.local = {verbs: {}};
                fragmentVars = tempVars;
                content.html(data);
                if (pushHistory !== undefined) {
                    pushHistory(dest);
                }
                
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
    
var navHistory = [];
function pushHistory(item) {
    if (navHistory[navHistory.length - 1] !== item) {
        navHistory.push(item);
    }
    if (navHistory.length > 1) {
        $("#back-btn").show();
    }
}

function popHistory() {
    var item = navHistory.pop();
    if (navHistory.length <= 1) {
        $("#back-btn").hide();
    }
    return item;
}

function clearHistory() {
    $("#back-btn").hide();
    navHistory = [];
}

function navigateDisplay(dest) {
    ws.socketSend("nav " + displayName + " " + dest);
}

function sendToDisplay(message) {
    ws.socketSend("send " + displayName + " " + message);
}