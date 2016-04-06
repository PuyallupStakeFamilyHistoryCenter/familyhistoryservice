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

"use strict";

// Read a page's GET URL variables and return them as an associative array.
// Courtesy of http://jquery-howto.blogspot.com/2009/09/get-url-parameters-values-with-jquery.html
var urlVars;
var fragmentVars;

function getUrlVars(hashes)
{
    var vars = [];
    var split = hashes.split('&');
    for (var i = 0; i < split.length; i++)
    {
        var hash = split[i].split('=');
        vars.push(hash[0]);
        if (hash.length >= 2) {
            var value = decodeURIComponent(hash[1].replace(/\+/g, " "));
            if (hash[0] === "target") {
                console.warn("Query string: '" + hashes + "'; target=" + value)
            }
            vars[hash[0]] = value;
        } else {
            if (hash[0] === "target") {
                console.warn("Query string: '" + hashes)
            }
            vars[hash[0]] = true;
        }
    }
    return vars;
}

function getParameterByName(name) {
    if (fragmentVars && fragmentVars[name]) {
        return fragmentVars[name];
    } else if (urlVars && urlVars[name]) {
        return urlVars[name];
    }
    return null;
}

function getPathComponent(index) {
    var split = window.location.pathname.split("/");
    if (index > 0) {
        return split[index];
    } else {
        return split[split.length + index];
    }
}

function parseBool(b) {
    return b === "true";
}


function sortFacts(facts) {
    facts.sort(function (a, b) {
        //TODO: Smart sorting based on some fact types (e.g. Birth comes before all other events, Death comes before Burial, Burial comes after all other events"
        if (a.type === b.type) {
            //Sort identical types by date
        } else if (a.type === "Birth") {
            return -1;
        } else if (b.type === "Birth") {
            return 1;
        } else if (a.type === "Christening") {
            return -1;
        } else if (b.type === "Christening") {
            return 1;
        } else if (a.type === "Burial") {
            return 1;
        } else if (b.type === "Burial") {
            return -1
        } else if (a.type === "Death") {
            return 1;
        } else if (a.type === "Death") {
            return -1;
        }

        var aDate = a.sortableDate;
        var bDate = b.sortableDate;
        if (bDate == null)
            return -1;
        if (aDate == null)
            return 1;

        return aDate < bDate ? -1 : 1;
    });
    return facts;
}

function removeDuplicates(facts) {
    var uniqueFacts = [];
    $.each(facts, function(index, fact) {
        if ($.inArray(fact, uniqueFacts) === -1) {
            uniqueFacts.push(fact);
        }
    });
    return uniqueFacts;
}

function rand(min, max) {
    return Math.floor(Math.random() * (max - min)) + min;
}

function getRandomElement(array) {
    return array[rand(0, array.length)];
}

/* Courtesy of http://stackoverflow.com/a/2450976/303601 */
function shuffle(array) {
    var currentIndex = array.length, temporaryValue, randomIndex;

    // While there remain elements to shuffle...
    while (0 !== currentIndex) {

        // Pick a remaining element...
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex -= 1;

        // And swap it with the current element.
        temporaryValue = array[currentIndex];
        array[currentIndex] = array[randomIndex];
        array[randomIndex] = temporaryValue;
    }

    return array;
}

function dedupe(array) {
    var varUseMap = {};
    var newArray = [];
    
    $.each(array, function(index, value) {
        if (!varUseMap[value]) {
            varUseMap[value] = 1;
            newArray.push(value);
        }
    });
    
    return newArray;
}

function filterArray(array, rawFilters) {
    var split = rawFilters.split(',');

    var currentArray = array;
    $.each(split, function (index, rawFilter) {
        var newArray = [];
        var filterRegex = /([a-z0-9]*)(=|<=|<|>=|>|!=|∋|∌| contains | !contains )(.+)/i;
        var filterMatches = rawFilter.match(filterRegex);
        if (!filterMatches || filterMatches.length < 4) {
            return [];
        }
        var filterKey = filterMatches[1];
        var operator = filterMatches[2];
        var filterValue = filterMatches[3];

        $.each(currentArray, function (index2, arrayValue) {
            var value = filterKey != null && filterKey.length > 0 ? arrayValue[filterKey] : arrayValue;
            
            switch (operator) {
                case "=":
                    if (value != null && value.toString && value.toString() === filterValue) {
                        newArray.push(arrayValue);
                    } else if (value == filterValue) {
                        newArray.push(arrayValue);
                    }
                    break;
                case "!=":
                    if (value != null && value.toString && value.toString() !== filterValue) {
                        newArray.push(arrayValue);
                    } else if (value != filterValue) {
                        newArray.push(arrayValue);
                    }
                    break;
                case ">":
                    if (value != null && value.toString && value.toString() > filterValue) {
                        newArray.push(arrayValue);
                    } else if (value > filterValue) {
                        newArray.push(arrayValue);
                    }
                    break;
                case ">=":
                    if (value != null && value.toString && value.toString() >= filterValue) {
                        newArray.push(arrayValue);
                    } else if (value >= filterValue) {
                        newArray.push(arrayValue);
                    }
                    break;
                case "<":
                    if (value != null && value.toString && value.toString() < filterValue) {
                        newArray.push(arrayValue);
                    } else if (value < filterValue) {
                        newArray.push(arrayValue);
                    }
                    break;
                case "<=":
                    if (value != null && value.toString && value.toString() <= filterValue) {
                        newArray.push(arrayValue);
                    } else if (value <= filterValue) {
                        newArray.push(arrayValue);
                    }
                    break;
                case "∋":
                case " contains ":
                    if (Array.isArray(value) && value.indexOf(filterValue) >= 0) {
                        newArray.push(arrayValue);
                    }
                    break;
                case "∌":
                case " !contains ":
                    if (Array.isArray(value) && value.indexOf(filterValue) == -1) {
                        newArray.push(arrayValue);
                    }
                    break;
                default:
                    throw new Error("Filter " + rawFilter + " contains invalid operator " + operator + "; context: " + rawFilters);
            }
        });

        currentArray = newArray;
    });

    return currentArray;
}

function say(message, cancel) {
    if (cancel) {
        speechSynthesis.cancel();
    }
    var msg = new SpeechSynthesisUtterance();
    msg.lang = "en-GB";
    msg.text = message;

    speechSynthesis.speak(msg);
}
    
function formatPlace(answer) {
    var formatted = answer
            .replace(/usa|united states/gi, "")
            .replace(/,\s*/, ", ")
            .replace(/^[,\s]+|[,\s]+$/g, "");
    return formatted;
}

function validatePin(pin) {
    return "01234567890".indexOf(pin) === -1 && "09876543210".indexOf(pin) === -1 && pin.search(/([0-9])\1\1\1/) === -1;
}
    
function replaceVariables(obj, original) {
    if (original == null) {
        return;
    }
    
    var regex = /\$\{([^\}\{]+)\}/g;
    var matches = original.match(regex);
    if (!matches) {
        return original;
    }

    var final = original;
    do {
        $.each(matches, function(index, found) {
            var variable = found.substr(2, found.length - 3);
            final = final.replace(found, resolveChildProperty(obj, variable)); 
        });
        matches = final.match(regex)
    } while (matches);

    return final;
}
    
var resolvedConstants = {};
    
function resolveChildProperty(obj, path) {
    console.info("Resolving " + path + " on " + JSON.stringify(obj));
    var split = path.split(".");
    var currentObject;
    var currentTry = 0;
    var maxTries = 10;

    do {
        currentObject = obj;
        $.each(split, function(index, key) {
            if (!currentObject) {
                return;
            }

            if (key[0] === "*" && index === 0) {
                currentObject = getRandomElement(filterArray(people, key.substr(1)));
            } else if (key === "stats" && index === 0) {
                currentObject = stats;
            } else if (key[0] === "*") {
                currentObject = getRandomElement(filterArray(currentObject, key.substr(1)));
            } else if (resolvedConstants[key] && index === 0) {
                currentObject = resolvedConstants[key];
            } else if (currentObject && currentObject[key]) {
                currentObject = currentObject[key];
            } else {
                currentObject = null;
            }
        });
    } while (split[0] === "*" && currentObject === null && currentTry < maxTries);

    return currentObject;
}

function splitWords(s) {
    if (!s) {
        return [];
    }
    return s.split(/[ .!?,-;:"'0-]/g);
}

function toFirstCaps(string) {
    return string.substring(0,1).toUpperCase() + string.substring(1);
}