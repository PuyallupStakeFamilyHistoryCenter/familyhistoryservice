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

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import org.puyallupfamilyhistorycenter.service.models.Person;

/**
 *
 * @author tibbitts
 */


public class MockPersonSource implements Source<Person> {
    private static final Gson GSON = new Gson();

    @Override
    public Person get(String personId, String accessToken) {
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
                        + "],"
                        + "\"image\":\"/image-cache?ref=https%3A%2F%2Ffamilysearch.org%2Fpatron%2Fv2%2FTH-303-48402-203-71%2Fthumb200s.jpg%3Fctx%3DArtCtxPublic%26amp%3B_%3D1412535799744%5C\""
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
                        + "\"relationship\":\"wife\","
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
        case "KWZP-8K5":
            personJson = "{"
                        + "\"id\":\"KWZP-8K5\","
                        + "\"name\":\"William Burriss Dial\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"16 November 1862\","
                        + "\"place\":\"Six Mile Prairie, Franklin, Illinois, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"6 September 1935\","
                        + "\"place\":\"Shelley, Bingham, Idaho, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"spouses\":["
                        + "{"
                        + "\"relationship\":\"wife\","
                        + "\"id\":\"KWZP-8KG\","
                        + "\"name\":\"Temperance Lavina Moore\""
                        + "}"
                        + "],"
                        + "\"children\":["
                        + "{"
                        + "\"id\":\"KWCB-HZV\","
                        + "\"name\":\"Willis Aaron Dial\","
                        + "\"relationship\":\"son\""
                        + "}"
                        + "]"
                        + "}";
            break;
        case "KWZP-8KG":
            personJson = "{"
                        + "\"id\":\"KWZP-8KG\","
                        + "\"name\":\"Temperance Lavina Moore\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"14 October 1873\","
                        + "\"place\":\"Crawford's Prairie, Franklin, Illinois, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"24 October 1946\","
                        + "\"place\":\"Idaho Falls, Bonneville, Idaho, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"spouses\":["
                        + "{"
                        + "\"relationship\":\"husband\","
                        + "\"id\":\"KWZP-8KG\","
                        + "\"name\":\"William Burriss Dial\""
                        + "}"
                        + "],"
                        + "\"children\":["
                        + "{"
                        + "\"id\":\"KWCB-HZV\","
                        + "\"name\":\"Willis Aaron Dial\","
                        + "\"relationship\":\"son\""
                        + "}"
                        + "]"
                        + "}";
            break;
        case "KWCB-HZ2":
            personJson = "{"
                        + "\"id\":\"KWCB-HZ2\","
                        + "\"name\":\"Ida Geneva Beckstrand\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"12 January 1901\","
                        + "\"place\":\"Santaquin, Utah, Utah, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"16 October 2000\","
                        + "\"place\":\"Logan, Cache, Utah, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"spouses\":["
                        + "{"
                        + "\"relationship\":\"husband\","
                        + "\"id\":\"KWCB-HZV\","
                        + "\"name\":\"Willis Aaron Dial\""
                        + "}"
                        + "]"
                        + "}";
            break;
        case "KWJJ-4XH":
            personJson = "{"
                        + "\"id\":\"KWJJ-4XH\","
                        + "\"name\":\"Merlin \\\"B\\\" Dial\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"29 December 1927\","
                        + "\"place\":\"Logan, Cache, Utah, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"2 January 1974\","
                        + "\"place\":\"Fresno, Fresno, California, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"parents\":["
                        + "{"
                        + "\"relationship\":\"father\","
                        + "\"id\":\"KWCB-HZV\","
                        + "\"name\":\"Willis Aaron Dial\""
                        + "},"
                        + "{"
                        + "\"relationship\":\"mother\","
                        + "\"id\":\"KWCB-HZ2\","
                        + "\"name\":\"Ida Geneva Beckstrand\""
                        + "}"
                        + "]"
                        + "}";
            break;
        case "KJWD-Z94":
            personJson = "{"
                        + "\"id\":\"KJWD-Z94\","
                        + "\"name\":\"Wanda Ann Dial\","
                        + "\"facts\":[{"
                        + "\"type\":\"birth\","
                        + "\"date\":\"21 March 1951\","
                        + "\"place\":\"Salt Lake City, Salt Lake, Utah, United States\""
                        + "},{"
                        + "\"type\":\"death\","
                        + "\"date\":\"4 April 1951\","
                        + "\"place\":\"Salt Lake City, Salt Lake, Utah, United States\""
                        + "}],"
                        + "\"living\":false,"
                        + "\"parents\":["
                        + "{"
                        + "\"relationship\":\"father\","
                        + "\"id\":\"KWC6-X7D\","
                        + "\"name\":\"Glen \\\"B\\\" Dial\""
                        + "},"
                        + "{"
                        + "\"relationship\":\"mother\","
                        + "\"id\":\"KWC6-X7C\","
                        + "\"name\":\"Beth Humphries\""
                        + "}"
                        + "]"
                        + "}";
            break;
        default:
            throw new NotFoundException("Person " + personId + " not found");
        }

        return GSON.fromJson(personJson, Person.class);
    }

    @Override
    public boolean has(String id) {
        return ImmutableSet.<String>builder()
                .add("KWCB-HZV")
                .add("KWC6-X7D")
                .add("KWZP-8K5")
                .add("KWZP-8KG")
                .add("KWCB-HZ2")
                .add("KWJJ-4XH")
                .add("KJWD-Z94")
                .build()
                .contains(id);
    }
}
