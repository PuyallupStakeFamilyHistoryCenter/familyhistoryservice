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
var displayName;
var settings;
var token;

//Add message listener to websocket
ws.addMessageListener(function(message) {
    var parts = message.data.split(" ");
    switch (parts[0]) {
        case "connected":
        case "ok":
        case "pong":
            break;
        case "nav":
            navigate(parts[1]);
            break;
        case "name":
            settings.gotNewDisplayName(parts[1]);
            break;
        case "standby":
            navigate("display-ready");
            break;
        case "attached":
            ws.socketSend("nav " + displayName + " " + "display-login");
            token = $.cookie("token", {expires: 1/24, path: '/'});
            if (!token) {
                navigate("controller-login");
            } else {
                settings.gotToken(token);
            }
            break;
        case "token":
            token = parts[1];
            settings.gotToken(token);
            break;
            
        default:
            if (parts[0] === "Error:") {
                error(message.data.substring(6));
            } else {
                error("unrecognized command '" + message.data + "'");
            }
    }
});

$(window).ready(function() {
    content = $("#content");
    mode = getParameterByName("mode");
    if (!mode) {
        error("Mode parameter is required");
        return;
    }
    
    switch (mode) {
        case "controller":
            settings = controllerSettings;
            break;
            
        case "display":
            settings = displaySettings;
            break;
            
        case "clear-display-name":
            $.removeCookie("display-name");
            break;
            
        default:
            error("Invalid type mode '" + mode + "'");
            return;
    }
    
    setup();
    
    displayName = $.cookie("display-name");
    if (!displayName) {
        settings.getNewDisplayName();
    } else {
        settings.gotNewDisplayName(displayName);
    }
});

function setup() {
    document.title = settings.title;
    var pageHeader = $("#page-header"); //TODO: This isn't working
    pageHeader.html("<h1>" + settings.header + "</h1>");
}

var displaySettings = {
    title: "Display",
    header: "Display",
    getNewDisplayName: function() {
        ws.socketSend("display-name");
    },
    gotNewDisplayName: function(name) {
        displayName = name;
        $.cookie("display-name", displayName);
        ws.socketSend("display " + displayName);
    }
};

var controllerSettings = {
    title: "Controller",
    header: '<img src="logo.png" alt="Puyallup Family History Center logo" />',
    getNewDisplayName: function() {
        navigate("controller-attach");
    },
    gotNewDisplayName: function(name) {
        displayName = name;
        $.cookie("display-name", displayName);
        ws.socketSend("controller " + displayName);
    },
    getToken: function(username, pin) {
        ws.socketSend("login " + username + " " + pin);
    },
    gotToken: function(newToken) {
        $.cookie("token", newToken, {expires: 1/24, path: '/'});
        ws.socketSend("nav " + displayName + " " + "display-main");
        navigate("controller-main");
    },
    logOut: function() {
        $.removeCookie("token", {expires: 1/24, path: '/'});
        ws.socketSend("nav " + displayName + " display-login");
        navigate("controller-login");
    }
};

function error(message) {
    log("danger", "<strong>Error:</strong> " + message);
}

function warn(message) {
    log("warning", "<strong>Warning:</strong> " + message);
}

function info(message) {
    log("info", "<strong>Info:</strong> " + message);
}

function success(message) {
    log("success", message);
}

function log(level, message) {
    $("#messages").append('<div class="alert alert-' + level + '">' + message + '</div>');
}

function navigate(dest) {
    $("#messages").html("");
    $.get(dest + ".html", function(data) {
            content.html(data);
        });
}

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}
