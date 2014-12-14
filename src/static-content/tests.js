
QUnit.module("UI testing controller", {
    setupOnce: function (assert) {
    },
    setup: function (assert) {
        configureSockete([
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
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
        title: "{\"responseType\":\"ok\"}",
        message: {data: "{\"responseType\":\"ok\"}"}
    }, {
        title: "connected",
        message: {data: "{\"responseType\":\"connected\"}"}
    }, {
        title: "{\"responseType\":\"pong\"}",
        message: {data: "{\"responseType\":\"pong\"}"}
    }
]).test("Test web-socket message handling", function (params, assert) {
    assert.expect(0);
    messageHandler(params.message);
});

QUnit.cases([
    {
        title: "global",
        message: {data: "{\"responseType\":\"spam\",\"data\":5}"},
        level: "global"
    }, {
        title: "page",
        message: {data: "{\"responseType\":\"spam\",\"data\":5}"},
        toInject: "page",
        level: "page"
    }, {
        title: "local",
        message: {data: "{\"responseType\":\"spam\",\"data\":5}"},
        level: "local"
    }
]).combinatorial([
    {
        verb: "spam"
    }
]).test("Test message handling", function (params, assert) {
    assert.expect(1);

    if (params.level) {
        settings[params.level].verbs[params.verb] = function (obj) {
            assert.equal(obj.responseType, params.verb);
        };
    }

    messageHandler(params.message);
});

QUnit.cases([
    {
        title: "none",
        message: {data: "{\"responseType\":\"spam\",\"data\":5}"}
    }, {
        title: "error message",
        message: {data: "{\"responseType\":\"error\",\"data\":5}"}
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
        assert.equal("{\"responseType\":\"pong\"}", message.data);
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
    },
    teardown: function() {
        ws.close();
    }
});

QUnit.cases([
    { title: "controller-login", page: "controller-login", waitForVerb: "user-list", searchFor: ".username-btn", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "list-current-users", response: "{\"responseType\":\"user-list\",\"users\":[{\"id\":\"userid1\",\"name\":\"username1\"},{\"id\":\"userid2\",\"name\":\"username2\"}]}" }
    ]},
    { title: "display-login", page: "display-login", searchFor: ".jumbotron", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
    ]},
    { title: "controller-attach", page: "controller-attach", searchFor: "#display-name-input", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
    ]},
    { title: "display-ready", page: "display-ready", searchFor: "#display-name-output", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
    ]},
    { title: "display-main", page: "display-main", searchFor: ".carousel", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
    ]},
    { title: "controller-main", page: "controller-main", searchFor: "#nav-buttons", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
    ]}
]).test("Test load", function (params, assert) {
    QUnit.stop();
    configureSockete(params.verbResponses);
    
    getReady();
    
    var verbPromise = new $.Deferred();
    if (params.waitForVerb) {
        ws.addMessageListener(function(message) {
            var obj = JSON.parse(message.data);
            if (obj.responseType === params.waitForVerb) {
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
    { title: "controller-attached-and-logged-in", targetPage: "controller-main", targetVerb: "{\"responseType\":\"ok\"}",
        searchFor: "#nav-buttons", noDisplay: false, noToken: false, verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "controller 336f7a86", response: "{\"responseType\":\"attached\"}" },
            { verb: "nav 336f7a86 display-main", response: "{\"responseType\":\"ok\"}" }
    ]},
    { title: "controller-not-attached", targetPage: "controller-attach", targetVerb: "{\"responseType\":\"pong\"}",
        searchFor: "#display-name-input", noDisplay: true, noToken: false, verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "controller 336f7a86", response: "{\"responseType\":\"attached\"}" },
            { verb: "nav 336f7a86 display-main", response: "{\"responseType\":\"ok\"}" }
    ]},
    { title: "controller-not-logged-in", targetPage: "controller-login", targetVerb: "{\"responseType\":\"ok\"}",
        searchFor: ".username-btn", noDisplay: false, noToken: true, verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "controller 336f7a86", response: "{\"responseType\":\"attached\"}" },
            { verb: "nav 336f7a86 display-login", response: "{\"responseType\":\"ok\"}" },
            { verb: "list-current-users", response: "{\"responseType\":\"user-list\",\"users\":[{\"id\":\"userid1\",\"name\":\"username1\"},{\"id\":\"userid2\",\"name\":\"username2\"}]}" }
    ]}
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
    }, 1000);
    ws.addMessageListener(function(message) {
        var obj = JSON.parse(message.data);
        if (obj.responseType === params.targetVerb) {
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
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "trigger", response: "{\"responseType\":\"nav\",\"dest\":\"controller-main\"}" }
    ]},
    { title: "nav-display", mode: "display", targetPage: "display-main", targetVerb: "nav",
        searchFor: ".carousel", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "trigger", response: "{\"responseType\":\"nav\",\"dest\":\"display-main\"}" }
    ]},
    { title: "standby", mode: "display", targetPage: "display-ready", targetVerb: "standby",
        searchFor: "#display-name-output", verbResponses: [
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "trigger", response: "{\"responseType\":\"standby\"}" }
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
        var obj = JSON.parse(message.data);
        if (obj.responseType === params.targetVerb) {
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
            { verb: "ping", response: "{\"responseType\":\"pong\"}" },
            { verb: "trigger", response: "{\"responseType\":\"person\",\"person\":{\"id\":\"asdf\",\"name\":\"John Smith\",\"image\":\"life-map.jpg\"}}" }
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
        var obj = JSON.parse(message.data);
        if (obj.responseType === params.targetVerb) {
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
        { verb: "ping", response: "{\"responseType\":\"pong\"}"},
        { verb: "controller 336f7a86", response: "{\"responseType\":\"attached\"}"},
        { verb: "nav 336f7a86 display-main", response: "{\"responseType\":\"ok\"}" },
        { verb: "nav 336f7a86 display-login", response: "{\"responseType\":\"ok\"}" },
        { verb: "list-current-users", response: "{\"responseType\":\"user-list\",\"users\":[{\"id\":\"userid1\",\"name\":\"username1\"},{\"id\":\"userid2\",\"name\":\"username2\"}]}" }
    ]);
    
    urlVars = {mode: "controller"};
    getReady();
    
    setTimeout(function() {
        setTimeout(function() {
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

QUnit.module("Test utils");

QUnit.cases([
    { title: "Empty", input:[], expected:[] },
    { title: "By type", input:[
            {type:"Burial"},
            {type:"Death"},
            {type:"Residence"},
            {type:"Birth"}
    ], expected:[
            {type:"Birth"},
            {type:"Residence"},
            {type:"Death"},
            {type:"Burial"}
    ] },
    { title: "By date", input:[
            {type:"Residence", sortableDate:"+1920-10"},
            {type:"Residence", sortableDate:"+1910-11-20"},
            {type:"Residence", sortableDate:"+1900"}
    ], expected:[
            {type:"Residence", sortableDate:"+1900"},
            {type:"Residence", sortableDate:"+1910-11-20"},
            {type:"Residence", sortableDate:"+1920-10"}
    ] },
    { title: "By date and type", input:[
            {type:"Burial"},
            {type:"Death"},
            {type:"Birth"},
            {type:"Residence", sortableDate:"+1920-10"},
            {type:"Residence", sortableDate:"+1910-11-20"},
            {type:"Residence", sortableDate:"+1900"}
    ], expected:[
            {type:"Birth"},
            {type:"Residence", sortableDate:"+1900"},
            {type:"Residence", sortableDate:"+1910-11-20"},
            {type:"Residence", sortableDate:"+1920-10"},
            {type:"Death"},
            {type:"Burial"},
    ] },
    { title: "By type fallback to date", input:[
            {type:"Birth",place:"before"},
            {type:"Birth", sortableDate:"+1910"},
            {type:"Birth", sortableDate:"+1900-03"},
            {type:"Birth", sortableDate:"+1900-01-31"},
            {type:"Birth", sortableDate:"+1900"},
            {type:"Birth",place:"after"},
    ], expected:[
            {type:"Birth", sortableDate:"+1900"},
            {type:"Birth", sortableDate:"+1900-01-31"},
            {type:"Birth", sortableDate:"+1900-03"},
            {type:"Birth", sortableDate:"+1910"},
            {type:"Birth",place:"before"},
            {type:"Birth",place:"after"},
    ] },
]).test("Test sort facts", function(params, assert) {
    var actual = sortFacts(params.input);
    assert.deepEqual(params.expected, actual);
});

QUnit.cases([
    { title: "Empty", array:[], filters:"", expected: [] },
    { title: "Single filter", array:[{gender:"Male"},{gender:"Female"}], filters:"gender=Female", expected: [{gender:"Female"}] },
    { title: "Single filter w/ missing key", array:[{},{gender:"Female"}], filters:"gender=Female", expected: [{gender:"Female"}] },
    { title: "Single filter numeric", array:[{age:25},{age:26}], filters:"age=26", expected: [{age:26}] },
    { title: "Single filter boolean", array:[{living:false},{living:true}], filters:"living=false", expected: [{living:false}] },
    { title: "Single filter numeric >", array:[{age:25},{age:26}], filters:"age>25", expected: [{age:26}] },
    { title: "Single filter numeric >=", array:[{age:25},{age:26}], filters:"age>=26", expected: [{age:26}] },
    { title: "Single filter numeric <=", array:[{age:25},{age:26}], filters:"age<=25", expected: [{age:25}] },
    { title: "Single filter numeric <", array:[{age:25},{age:26}], filters:"age<26", expected: [{age:25}] },
    { title: "Single filter numeric !=", array:[{age:25},{age:26}], filters:"age!=26", expected: [{age:25}] },
    { title: "Single filter no LHS !=", array:["old","young"], filters:"!=old", expected: ["young"] },
    { title: "Array filter ∋", array:[{images:["image1","image2"]},{images:["image2","image3"]}], filters:"images∋image1", expected: [{images:["image1","image2"]}] },
    { title: "Array filter 'contains'", array:[{images:["image1","image2"]},{images:["image2","image3"]}], filters:"images contains image1", expected: [{images:["image1","image2"]}] },
    { title: "Array filter ∌", array:[{images:["image1","image2"]},{images:["image2","image3"]}], filters:"images∌image1", expected: [{images:["image2","image3"]}] },
    { title: "Array filter '!contains'", array:[{images:["image1","image2"]},{images:["image2","image3"]}], filters:"images !contains image1", expected: [{images:["image2","image3"]}] },
    { title: "Compound filter", array:[{gender:"Male",age:25},{gender:"Female",age:26},{gender:"Male",age:26},{gender:"Female",age:28}], filters:"age=26,gender=Female", expected: [{gender:"Female",age:26}] },
]).test("Test filterArray", function(params, assert) {
    assert.expect(1);
    var actual = filterArray(params.array, params.filters);
    assert.deepEqual(actual, params.expected);
});

QUnit.cases([
    { title: "Ascending run 1", input: "1234", output: false },
    { title: "Ascending run 2", input: "0123", output: false },
    { title: "Ascending run 3", input: "6789", output: false },
    { title: "Ascending run 4", input: "7890", output: false },
    { title: "Descending run 1", input: "0987", output: false },
    { title: "Descending run 2", input: "9876", output: false },
    { title: "Descending run 3", input: "4321", output: false },
    { title: "Descending run 3", input: "3210", output: false },
    { title: "Single digit 0", input: "0000", output: false },
    { title: "Single digit 1", input: "1111", output: false },
    { title: "Single digit 2", input: "2222", output: false },
    { title: "Single digit 3", input: "3333", output: false },
    { title: "Single digit 4", input: "4444", output: false },
    { title: "Single digit 5", input: "5555", output: false },
    { title: "Single digit 6", input: "6666", output: false },
    { title: "Single digit 7", input: "7777", output: false },
    { title: "Single digit 8", input: "8888", output: false },
    { title: "Single digit 9", input: "9999", output: false },
    { title: "Good example 1", input: "3764", output: true },
    { title: "Good example 2", input: "7276", output: true },
    { title: "Good example 3", input: "9966", output: true },
    { title: "Good example 4", input: "1763", output: true },
    { title: "Good example 5", input: "9786", output: true },
    { title: "Good example 6", input: "6673", output: true },
    { title: "Good example 7", input: "0918", output: true },
    { title: "Good example 8", input: "0011", output: true },
    { title: "Good example 9", input: "8446", output: true }
]).test("Test PIN validation", function(params, assert) {
    var result = validatePin(params.input);
    assert.equal(result, params.output, "Input: " + params.input + " expected: " + params.output + " actual: " + result);
});



QUnit.module("Test quiz functions", {
    setup: function (assert) {
        $("#qunit-fixture").html("<div id='messages'></div><div id='page-header'></div><div id='content'></div>");
        //NOTE: Don't try to change location.search from within the test; it causes a page refresh
        configureSockete([
            { verb: "ping", response: "{\"responseType\":\"pong\"}" }
        ]);
        content = null;
        mode = null;
        displayName = null;
        settings.page = {verbs: {}};
        settings.local = {verbs: {}};
        token = null;
        userName = null;
        urlVars = null;
        $.cookie("display-name", "336f7a86");
        urlVars = { mode: "controller" };
        getReady();
    },
    teardown: function() {
        ws.close();
    }
});


QUnit.cases([
    { title: "Empty", person:{}, path:"", expected: null },
    { title: "Name", person:{name:"Graham Tibbitts"}, path:"name", expected: "Graham Tibbitts"},
    { title: "Facts generic filter", person:{name:"Graham Tibbitts", facts:[{type:"Birth",date:"2014-10-27"}]}, path:"facts.*type=Birth.date", expected: "2014-10-27"},
    { title: "Array index", person:{name:"Graham Tibbitts", spouses:[{id:"asdf",name:"Katrina Tibbitts"}]}, path:"spouses.0.id", expected: "asdf"},
    { title: "Star operator array single option", person:{name:"Graham Tibbitts", spouses:[{id:"asdf",name:"Katrina Tibbitts"}]}, path:"spouses.*.id", expected: "asdf"},
    { title: "Star operator array multiple options", person:{name:"Graham Tibbitts", children:[{id:"asdf",name:"Allison Tibbitts"},{id:"asdf",name:"James Tibbitts"}]}, path:"children.*.id", expected: "asdf"},
//    { title: "Star operator object", person:{name:"Graham Tibbitts", children:[{id:"asdf"}]}, path:"children.0.*", expected: "asdf"},
    { title: "Star operator person", person:{}, people:[{name:"Graham Tibbitts"}], path: "*.name", expected: "Graham Tibbitts"},
    { title: "Star operator person w/ filter", person:{}, people:[{name:"Graham Tibbitts",gender:"Male"},{name:"Allison Tibbitts",gender:"Female"}], path: "*gender=Male.name", expected: "Graham Tibbitts"},
    { title: "Star operator filter on string", person:{}, people:["filter", "filter", "filter", "filter", "filter", "nofilter"], path: "*!=filter", expected: "nofilter"}
]).test("Test resolveChildProperty", function(params, assert) {
    assert.expect(1);
    QUnit.stop();
    navigate("controller-quiz", 100).then(function() {
        people = params.people;
        
        var actual = resolveChildProperty(params.person, params.path);
        assert.deepEqual(actual, params.expected);
        QUnit.start();
    }).fail(function() {
        QUnit.start();
    });
});

QUnit.cases([
    { title: "Empty", person:{}, prerequisites:[], expected: true },
    { title: "Living", person:{living:true}, prerequisites:[], expected: false },
    { title: "Missing name", person:{}, prerequisites:["name"], expected: false },
    { title: "Name", person:{name:"Graham Tibbitts"}, prerequisites:["name"], expected: true },
    { title: "Name and missing birth date", person:{name:"Graham Tibbitts"}, prerequisites:["name", "facts.*type=Birth.date"], expected: false },
    { title: "Name and birth date", person:{name:"Graham Tibbitts", facts:[{type:"Birth",date:"2014-10-27"}]}, prerequisites:["name", "facts.*type=Birth.date"], expected: true },
]).test("Test satisfiesPrerequisites", function(params, assert) {
    assert.expect(1);
    QUnit.stop();
    navigate("controller-quiz", 100).then(function() {
        var actual = satisfiesPrerequisites(params.person, params.prerequisites);
        assert.equal(actual, params.expected);
        QUnit.start();
    }).fail(function() {
        QUnit.start();
    });
});

QUnit.cases([
    { title: "Empty", person:{}, original:"", expected: "" },
    { title: "Name", person:{name:"Graham Tibbitts"}, original:"My name is ${name}", expected: "My name is Graham Tibbitts" },
    { title: "Name and birth date", person:{name:"Graham Tibbitts", facts:[{type:"Birth",date:"2014-10-27"}]}, original: "${name} was born on ${facts.*type=Birth.date}", expected: "Graham Tibbitts was born on 2014-10-27" },
    { title: "Array length", person:{children:[{},{},{}]}, original:"I have ${children.length} children", expected: "I have 3 children" },
    { title: "Compound", person:{parents:[{id:"ASDF-123",gender:"Male"},{id:"QWER-456",gender:"Female"}]}, people:[{id:"ASDF-123",name:"Graham Tibbitts"}], original:"My father's name is ${*id=${parents.*gender=Male.id}.name}", expected: "My father's name is Graham Tibbitts" },
]).test("Test replaceVariables", function(params, assert) {
    assert.expect(1);
    QUnit.stop();
    navigate("controller-quiz", 100).then(function() {
        people = params.people;
        var actual = replaceVariables(params.person, params.original);
        assert.equal(actual, params.expected);
        QUnit.start();
    }).fail(function() {
        QUnit.start();
    });
});