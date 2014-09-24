

QUnit.module("UI testing controller", {
    setupOnce: function (assert) {
    },
    setup: function (assert) {
        ws.reset();
        configureSockete([
            { verb: "ping", response: "pong" }
        ]);
        $("#qunit-fixture").append("<div id='messages'></div><div id='page-header'></div><div id='content'></div>");
        //NOTE: Don't try to change location.search from within the test; it causes a page refresh
        content = null;
        mode = null;
        displayName = null;
        settings.global = {
            verbs: {
                connected: doNothing,
                ok: doNothing,
                pong: doNothing
            }
        };
        settings.page = {verbs: {}};
        settings.local = {verbs: {}};
        token = null;
        userName = null;
        urlVars = null;
        $.cookie("display-name", "336f7a86");
    },
    teardown: function (assert) {
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
    }, {
        title: "none",
        message: {data: "spam 9"},
        level: null
    }, {
        title: "error message",
        message: {data: "Error: 9"},
        level: null
    }
]).combinatorial([
    {
        verb: "spam"
    }
]).test("Test message handling", function (params, assert) {
    assert.expect(params.level ? 1 : 0);

    if (params.level) {
        settings[params.level].verbs[params.verb] = function (parts) {
            assert.equal(parts[0], params.verb);
        };
    }

    messageHandler(params.message);
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

QUnit.cases([
    { title: "controller-login", page: "controller-login", searchFor: ".username-btn", waitForVerb: "user-list", verbResponses: [
            { verb: "ping", response: "pong" },
            { verb: "list-current-users", response: "user-list userid1 username1 userid2 username2" }
    ] }
]).test("Test load", function (params, assert) {
    QUnit.stop();
    configureSockete(params.verbResponses);
    
    getReady();
    
    var verbPromise = new $.Deferred();
    ws.addMessageListener(function(message) {
        if (message.data.split(" ")[0] === params.waitForVerb) {
            verbPromise.resolve();
        }
    });
    if (!params.waitForVerb) {
        verbPromise.resolve();
    }
    
    var navigatePromise = navigate(params.page);
    
    $.when(verbPromise, navigatePromise).then(function(result) {
        assert.ok($(params.searchFor).length, "Search for added elements");
        
        QUnit.start();
    });
    
    
});

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