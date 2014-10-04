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

package org.puyallupfamilyhistorycenter.service.cache;

import com.google.gson.Gson;
import org.puyallupfamilyhistorycenter.service.models.Person;

/**
 *
 * @author tibbitts
 */


public class MockPersonSource implements Source<Person> {
    private static final Gson GSON = new Gson();

    @Override
    public Person get(String personId) {
        String personJson = null;
        switch (personId) {
        case "KWCB-HZV":
            personJson = "{"
                        + "\"id\":\"KWCB-HZV\","
                        + "\"name\":\"Willis Aaron Dial\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"23 December 1897\","
                        + "\"place\":\"Hooper, Weber, Utah, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"19 January 1985\","
                        + "\"place\":\"Logan, Cache, Utah, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"image\":\"/image-cache?ref=https%3A%2F%2Ffamilysearch.org%2Fpatron%2Fv2%2FTH-303-40516-109-69%2Fdist.jpg%3Fctx%3DArtCtxPublic%26angle%3D0\","
                        + "\"spouses\":["
                        + "{"
                        + "\"id\":\"KWCB-HZ2\","
                        + "\"name\":\"Ida Lovisa Beckstrand\","
                        + "\"relationship\":\"wife\""
                        + "}"
                        + "],"
                        + "\"parents\":["
                        + "{"
                        + "\"relationship\":\"father\","
                        + "\"id\":\"KWZP-8K5\","
                        + "\"name\":\"William Burris Dial\""
                        + "},"
                        + "{"
                        + "\"relationship\":\"mother\","
                        + "\"id\":\"KWZP-8KG\","
                        + "\"name\":\"Temperance Lavina Moore\""
                        + "}"
                        + "],"
                        + "\"children\":["
                        + "{"
                        + "\"id\":\"KWC6-X7D\","
                        + "\"name\":\"Glen \\\"B\\\" Dial\","
                        + "\"relationship\":\"son\""
                        + "},"
                        + "{"
                        + "\"id\":\"KWJJ-4XH\","
                        + "\"name\":\"Merlin \\\"B\\\" Dial\","
                        + "\"relationship\":\"son\""
                        + "}"
                        + "]"
                        + "}";
            break;
        case "KWC6-X7D":
            personJson = "{"
                        + "\"id\":\"KWC6-X7D\","
                        + "\"name\":\"Glen \\\"B\\\" Dial\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"23 December 1934\","
                        + "\"place\":\"Logan, Cache, Utah, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"19 January 1970\","
                        + "\"place\":\"San Diego, San Diego, California, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"spouses\":["
                        + "{"
                        + "\"id\":null,"
                        + "\"name\":\"Beth Humphries\""
                        + "}"
                        + "],"
                        + "\"parents\":["
                        + "{"
                        + "\"relationship\":\"father\","
                        + "\"id\":\"KWCB-HZV\","
                        + "\"name\":\"Willis Aaron Dial\""
                        + "}"
                        + "],"
                        + "\"children\":["
                        + "{"
                        + "\"id\":\"KJWD-Z94\","
                        + "\"name\":\"Wanda Ann Dial\","
                        + "\"relationship\":\"daughter\""
                        + "}"
                        + "]"
                        + "}";
            break;
        }

        if (personJson != null) {
            return GSON.fromJson(personJson, Person.class);
        }

        return null;
    }
    
}
