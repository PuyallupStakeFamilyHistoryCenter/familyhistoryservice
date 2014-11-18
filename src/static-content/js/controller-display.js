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
            nav: function(cmd) {
                navigate(cmd.dest);
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
    if (!urlVars) {
        urlVars = getUrlVars(window.location.search.substr(1));
    }
    
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
    var pageFooter = $("#page-footer");
    if (settings.page.footer) {
        pageFooter.html("settings.page.footer");
    } else if (settings.page.footerFile) {
        $.ajax("fragments/"+ settings.page.footerFile)
        .done(function(data) {
            pageFooter.html(data);
        }).fail(function() {
            logger.error("Failed to load footer from " + settings.page.footerFile);
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
        settings.page.verbs.name({name:displayName});
    }
}

var defaultSettings = {
    display: {
        title: "Display",
        header: '<img src="/nmedia/logo.png" alt="Puyallup Family History Center logo" />',
        contentPadding: false,
        verbs: {
            standby: function() {
                navigate("display-ready");
            },
            name: function(obj) {
                displayName = obj.name;
                if (displayName) {
                    $.cookie("display-name", displayName);
                    ws.socketSend("display " + displayName);
                }
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
        footerFile: 'controller-footer.html',
        contentPadding: true,
        verbs: {
            attached: function() {
                $.cookie("display-name", displayName);
                console.info("Found token '" + token + "'");
                navigateDisplay("display-login");
                navigate("controller-login");
            },
            name: function(obj) {
                settings.page.gotNewDisplayName(obj.name);
            },
            token: function(response) {
                setUsername(response.username);
                token = response.token;
                navigateDisplay("display-main");
                clearHistory();
                navigate("controller-main");
            },
            precacheEvent: function(obj) {
                
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
            ws.socketSend("logout " + token);
            token = null;
            navigateDisplay("display-login");
            navigate("controller-login");
        },
        destroyAccessToken: function(userId, pin) {
            ws.socketSend("destroy-access-token " + userId + " " + pin);
        }
    },
    kiosk: {
        title: "Kiosk",
        header: '<img src="/nmedia/logo.png" alt="Puyallup Family History Center logo" />',
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
    logger.clear();
    
    if (message.data === "connected") {
        return;
    }
    
    var obj=JSON.parse(message.data);
    if (settings.local.verbs[obj.responseType]) {
        settings.local.verbs[obj.responseType](obj);
    } else if (settings.page.verbs[obj.responseType]) {
        settings.page.verbs[obj.responseType](obj);
    } else if (settings.global.verbs[obj.responseType]) {
        settings.global.verbs[obj.responseType](obj);
    } else {
        var errorMessage;
        if (obj.responseType === "error") {
            errorMessage = obj.message;
        } else {
            errorMessage = "unrecognized command '" + obj.responseType + "'";
        }
        logger.error(errorMessage);
        //throw new Error(errorMessage);
    }
    return;
}

var logger = {
    error: function (message, ttl) {
        log("danger", "<strong>Error:</strong> " + message, ttl);
    },

    warn: function (message, ttl) {
        log("warning", "<strong>Warning:</strong> " + message, ttl);
    },

    info: function (message, ttl) {
        log("info", "<strong>Info:</strong> " + message, ttl);
    },

    success: function (message, ttl) {
        log("success", message, ttl);
    },
    
    clear: function() {
        clearLog();
    }
};

function log(level, message, ttl) {
    //TODO: Add timeout parameter for messages to disappear
    var id = Math.round(Math.random() * 10000);
    $("#messages").append('<div id="message-'+id+'" class="alert alert-' + level + ' alert-dismissible" role="alert">'+
            '<button type="button" class="close" data-dismiss="alert"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>' + 
            message + '</div>');
    
    if (!ttl) {
        ttl = 5000;
    }
    setTimeout(function() {
        $("#messages").remove("#message-" + id);
    }, ttl);
}

function clearLog() {
    //TODO: Add timeout parameter for messages to disappear
    $("#messages").html();
}

function navigate(dest, timeout) {
    if (!timeout) {
        timeout = 1000;
    }
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
    setTimeout(function() {
        deferred.reject();
        //throw new Error("Timed out while loading " + dest);
    }, timeout);

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
    console.info("History: " + navHistory);
}

function popHistory() {
    var item = navHistory.pop();
    if (navHistory.length <= 1) {
        $("#back-btn").hide();
    }
    console.info("History: " + navHistory);
    return item;
}

function clearHistory() {
    $("#back-btn").hide();
    navHistory = [];
}

function navigateDisplay(dest) {
    sendToDisplay(JSON.stringify({responseType:"nav",dest:dest}));
}

function sendToDisplay(message) {
    ws.socketSend("send " + token + " " + displayName + " " + message);
}

function setHeaderName(headerName) {}