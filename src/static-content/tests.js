

function configureSockete(verbResponses) {
    Sockete.Server.reset();
    var endpoint = "ws://" + window.location.host + "/remote-control/";
    Sockete.Server.configure(endpoint, function () {
        for (var i = 0; i < verbResponses.length; i++) {
            var entry = verbResponses[i];
            if (entry.response) {
                this.onmessage(entry.verb).respond(entry.response);
            } else if (entry.error) {
                this.onmessage(entry.verb).fail(entry.error);
            } else {
                //We should probably throw an configuration exception here
            }
        };
    });
    Sockete.mock();
}

QUnit.module("UI testing controller", {
    setupOnce: function (assert) {
    },
    setup: function (assert) {
        configureSockete([
            { verb: "ping", response: "pong" }
        ]);
        $("#qunit-fixture").append("<div id='messages'></div><div id='page-header'></div><div id='content'></div>");
        //NOTE: Don't try to change location.search from within the test; it causes a page refresh
        content = null;
        mode = null;
        displayName = null;
//        settings.global = {
//            verbs: {
//                connected: doNothing,
//                ok: doNothing,
//                pong: doNothing
//            }
//        };
//        settings.page = {verbs: {}};
//        settings.local = {verbs: {}};
        token = null;
        userName = null;
        urlVars = null;
        $.cookie("display-name", "336f7a86");
        $.cookie("token", "336f7a86336f7a86");
    },
    teardown: function() {
        ws.close();
    }
});

QUnit.cases([
    {title: "messages", selector: "#messages"},
    {title: "page-header", selector: "#page-header"},
    {title: "content", selector: "#content"}
]).test("Test module setup", function (params, assert) {
    assert.expect(1);
    $.each($(params.selector), function () {
        assert.ok(1 === 1, "Found a DOM element matching " + params.selector);
    });
});

QUnit.cases([
    {title: "error", level: "error", class: "danger", label: "Error"},
    {title: "warn", level: "warn", class: "warning", label: "Warning"},
    {title: "info", level: "info", label: "Info"},
    {title: "success", level: "success"}
]).test("Test messages", function (params, assert) {
    logger[params.level]("MESSAGE");

    assert.equal($("#messages").html(), '<div class="alert alert-' + (params.class ? params.class : params.level) + ' alert-dismissible" role="alert">' +
            '<button type="button" class="close" data-dismiss="alert"><span aria-hidden="true">×</span><span class="sr-only">Close</span></button>' +
            (params.label ? '<strong>' + params.label + ':</strong> ' : '') + 'MESSAGE</div>');
});

QUnit.cases([
    {
        title: "ok",
        message: {data: "ok"}
    }, {
        title: "connected",
        message: {data: "connected"}
    }, {
        title: "pong",
        message: {data: "pong"}
    }
]).test("Test web-socket message handling", function (params, assert) {
    assert.expect(0);
    messageHandler(params.message);
});

QUnit.cases([
    {
        title: "global",
        message: {data: "spam 5"},
        level: "global"
    }, {
        title: "page",
        message: {data: "spam 3"},
        toInject: "page",
        level: "page"
    }, {
        title: "local",
        message: {data: "spam 9"},
        level: "local"
    }
]).combinatorial([
    {
        verb: "spam"
    }
]).test("Test message handling", function (params, assert) {
    assert.expect(1);

    if (params.level) {
        settings[params.level].verbs[params.verb] = function (parts) {
            assert.equal(parts[0], params.verb);
        };
    }

    messageHandler(params.message);
});

QUnit.cases([
    {
        title: "none",
        message: {data: "spam 9"}
    }, {
        title: "error message",
        message: {data: "Error: 9"}
    }
]).combinatorial([
    {
        verb: "spam"
    }
]).test("Test message handling error", function (params, assert) {
    assert.expect(1);

    messageHandler(params.message);
    
    assert.ok($(".alert-dismissible").length, "Found alert");
});

QUnit.cases([
    {title: "display", mode: "display"},
    {title: "controller", mode: "controller"}
]).test("Test set mode, get display name", function (params, assert) {
    urlVars = {mode: params.mode};
    getReady();
    assert.ok(displayName, "Display name cannot be null");
});



QUnit.cases([
    {title: "unknown", mode: "unknown"},
    {title: "kiosk", mode: "kiosk"},
    {title: "null", mode: null}
]).test("Test set mode, null display name", function (params, assert) {
    urlVars = {mode: params.mode};
    getReady();
    assert.ok(!displayName, "Display name should be null");
});

QUnit.cases([
    {title: "clear-display-name", mode: "clear-display-name"}
]).test("Test set mode, null display name", function (params, assert) {
    urlVars = {mode: params.mode};
    getReady();
    assert.ok(!displayName, "Display name should be null");
    assert.ok(!$.cookie("display-name"), "Display name cookie should be null");
});


QUnit.cases([
    {title: "empty", dest: "empty"},
    {title: "empty?key=value", dest: "empty?key=value"}
]).test("Test navigate", function (params, assert) {
    getReady();
    assert.will(navigate(params.dest), "Navigate to " + params.dest);
});


QUnit.cases([
    {title: "nonexistent", dest: "nonexistent"}
]).test("Test navigate", function (params, assert) {
    getReady();
    assert.wont(navigate(params.dest), "Navigate to " + params.dest);
});

QUnit.test("Test websocket ping on connect", function (assert) {
    QUnit.stop();
    assert.expect(1);
    ws.addMessageListener(function (message) {
        assert.equal("pong", message.data);
    });
    ws.connect();
    
    setTimeout(function() {
        QUnit.start();
    }, 30);
});


QUnit.module("Test navigation with UI changes", {
    setup: function (assert) {
        $("#qunit-fixture").html("<div id='messages'></div><div id='page-header'></div><div id='content'></div>");
        //NOTE: Don't try to change location.search from within the test; it causes a page refresh
        content = null;
        mode = null;
        displayName = null;
//        settings.global = {
//            verbs: {
//                connected: doNothing,
//                ok: doNothing,
//                pong: doNothing
//            }
//        };
        settings.page = {verbs: {}};
        settings.local = {verbs: {}};
        token = null;
        userName = null;
        urlVars = null;
        $.cookie("display-name", "336f7a86");
        $.cookie("token", "336f7a86336f7a86");
    },
    teardown: function() {
        ws.close();
    }
});

QUnit.cases([
    { title: "controller-login", page: "controller-login", waitForVerb: "user-list", searchFor: ".username-btn", verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "list-current-users", response: "user-list userid1 username1 userid2 username2" }
    ]},
    { title: "display-login", page: "display-login", searchFor: ".jumbotron", verbResponses: [
            { verb: "ping", response: "pong" }
    ]},
    { title: "controller-attach", page: "controller-attach", searchFor: "#display-name-input", verbResponses: [
            { verb: "ping", response: "pong" }
    ]},
    { title: "display-ready", page: "display-ready", searchFor: "#display-name-output", verbResponses: [
            { verb: "ping", response: "pong" }
    ]},
    { title: "display-main", page: "display-main", searchFor: ".carousel", verbResponses: [
            { verb: "ping", response: "pong" }
    ]},
    { title: "controller-main", page: "controller-main", searchFor: "#nav-buttons", verbResponses: [
            { verb: "ping", response: "pong" }
    ]}
]).test("Test load", function (params, assert) {
    QUnit.stop();
    configureSockete(params.verbResponses);
    
    getReady();
    
    var verbPromise = new $.Deferred();
    if (params.waitForVerb) {
        ws.addMessageListener(function(message) {
            if (message.data.split(" ")[0] === params.waitForVerb) {
                verbPromise.resolve();
            }
        });
        setTimeout(function() {
            verbPromise.resolve();
        }, 100);
    } else {
        setTimeout(function() {
            verbPromise.resolve();
        }, 10);
    }
    
    var navigatePromise = navigate(params.page);
    
    $.when(verbPromise, navigatePromise).then(function(result) {
        assert.ok($(params.searchFor).length, "Search for added elements with " + params.searchFor + " selector");
        
        QUnit.start();
    });
});

QUnit.cases([
    { title: "controller-attached-and-logged-in", targetPage: "controller-main", targetVerb: "ok",
        searchFor: "#nav-buttons", noDisplay: false, noToken: false, verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "controller 336f7a86", response: "attached" },
            { verb: "nav 336f7a86 display-main", response: "ok" }
    ]},
    { title: "controller-not-attached", targetPage: "controller-attach", targetVerb: "pong",
        searchFor: "#display-name-input", noDisplay: true, noToken: false, verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "controller 336f7a86", response: "attached" },
            { verb: "nav 336f7a86 display-main", response: "ok" }
    ]},
    { title: "controller-not-logged-in", targetPage: "controller-login", targetVerb: "ok",
        searchFor: ".username-btn", noDisplay: false, noToken: true, verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "controller 336f7a86", response: "attached" },
            { verb: "nav 336f7a86 display-login", response: "ok" },
            { verb: "list-current-users", response: "user-list userid1 username1 userid2 username2" }
    ]},
]).test("Test get display name", function(params, assert) {
    console.info("Starting test " + params.title);
    assert.expect(1);
    
    if (params.noDisplay) {
        $.removeCookie("display-name");
    }
    if (params.noToken) {
        $.removeCookie("token");
    }
    
    QUnit.stop();
    configureSockete(params.verbResponses);
    
    urlVars = {mode: "controller"};
    getReady();
    
    var timeout = setTimeout(function() {
        QUnit.start();
    }, 1000)
    ws.addMessageListener(function(message) {
        if (message.data.split(" ")[0] === params.targetVerb) {
            clearTimeout(timeout);
            setTimeout(function() {
                assert.ok($(params.searchFor).length, "Did not navigate to " + params.targetPage);
                QUnit.start();
            }, 30);
        }
    });
    
});

QUnit.cases([
    { title: "nav-controller", mode: "controller", targetPage: "controller-main", targetVerb: "nav",
        searchFor: "#nav-buttons", verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "trigger", response: "nav controller-main" }
    ]},
    { title: "nav-display", mode: "display", targetPage: "display-main", targetVerb: "nav",
        searchFor: ".carousel", verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "trigger", response: "nav display-main" }
    ]},
    { title: "standby", mode: "display", targetPage: "display-ready", targetVerb: "standby",
        searchFor: "#display-name-output", verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "trigger", response: "standby" }
    ]}
]).combinatorial([
    {
        begin: getDisplayName
    }
]).test("Test verb", function(params, assert) {
    console.info("Starting test " + params.title);
    assert.expect(1);
    
    QUnit.stop();
    configureSockete(params.verbResponses);
    
    urlVars = { mode: params.mode };
    defaultSettings[params.mode].begin = undefined;
    getReady();
    
    var timeout = setTimeout(function() {
        QUnit.start();
    }, 1000);
    ws.addMessageListener(function(message) {
        if (message.data.split(" ")[0] === params.targetVerb) {
            clearTimeout(timeout);
            setTimeout(function() {
                assert.ok($(params.searchFor).length, "Did not navigate to " + params.targetPage);
                QUnit.start();
            }, 30);
        }
    });
    
    ws.socketSend("trigger");
    
    defaultSettings[params.mode].begin = params.begin;
});

QUnit.cases([
    { title: "display-person-detail", mode:"display", page: "display-person-detail", targetVerb: "person", searchFor: "#person-img", verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "trigger", response: "person {\"person_id\":\"asdf\",\"name\":\"John%20Smith\",\"image\":\"life-map.jpg\"}" }
    ]}
]).test("Test verb", function(params, assert) {
    console.info("Starting test " + params.title);
    assert.expect(1);
    
    QUnit.stop();
    configureSockete(params.verbResponses);
    
    urlVars = { mode: params.mode };
    defaultSettings[params.mode].begin = undefined;
    getReady();
    
    navigate(params.page);
    
    var timeout = setTimeout(function() {
        QUnit.start();
    }, 1000);
    ws.addMessageListener(function(message) {
        if (message.data.split(" ")[0] === params.targetVerb) {
            clearTimeout(timeout);
            setTimeout(function() {
                assert.ok($(params.searchFor).length, "Did not navigate to " + params.targetPage);
                QUnit.start();
            }, 30);
        }
    });
    
    ws.socketSend("trigger");
    
    defaultSettings[params.mode].begin = params.begin;
});

QUnit.test("Test log out", function (assert) {
    QUnit.stop();
    assert.expect(2);
    configureSockete([
        { verb: "ping", response: "pong"},
        { verb: "controller 336f7a86", response: "attached"},
        { verb: "nav 336f7a86 display-main", response: "ok" },
        { verb: "nav 336f7a86 display-login", response: "ok" },
        { verb: "list-current-users", response: "user-list userid1 username1 userid2 username2" }
    ]);
    
    urlVars = {mode: "controller"};
    getReady();
    
    setTimeout(function() {
        setTimeout(function() {
            assert.ok(!$.cookie("token"), "Token cookie should be empty");
            assert.ok(!$(".username-btn").length, "Did not navigate to controller-login");
            QUnit.start();
        }, 30);
        settings.page.logOut();
    }, 30);
});

//QUnit.test("Test display standby verb");
//QUnit.test("Test controller attached verb");
//QUnit.test("Test controller token verb");
//QUnit.test("Test controller log in");
//QUnit.test("Test controller log out");
//QUnit.test("Test Object.toType (or remove)")