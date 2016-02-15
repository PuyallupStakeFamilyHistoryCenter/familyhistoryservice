/*
 * Copyright (c) 2015, tibbitts
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
package org.puyallupfamilyhistorycenter.service.utils;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.WordUtils;

/**
 *
 * @author tibbitts
 */
public class CountryCanonicalization {
    static final Object[] countryToAbbreviationMapping = {
        "united states", new String[] {
            "usa", "virginia", "va", "connecticut", "maryland", "rhode island", "nc",
            "north carolina", "pa", "ct", "conn", "pennsylvania", "massachusetts",
            "new york", "conneticut", "md", "wyoming", "georgia", "ga", "ri", "delaware", 
            "r i", "mass", "utah", "ill", "vermont", "illinois", "vausa", "tennessee",
            "ut", "indiana", "vermont", "ohio", "ky", "tenn", "kentucky", "connecticutt",
            "ma", "sc", "nj", "new jersey", "wy", "ny", "no carolina", "il", "wyo",
            "kansas", "co", "tn", "united states of america", "north carolina usa",
            "south carolina", "california", "ca", "nevada", "nv", "arizona", "az",
            "idaho", "id", "washington", "wa", "dc", "washington dc", "iowa", "ia",
            "montana", "mt", "minnesota", "mn", "oregon", "or", "new mexico", "nm",
            "del", "illn", "colorado", "co", "us", "cn", "pocatello bannock idaho",
            "unted states", "wilkes county georgia", "new london county", "va or md",
            "new hampshire", "british colonial america"
        },
        "england", new String[] {
            "eng", "engl", "bristol"
        },
        "denmark", new String[] {
            "den", "dnmr", "danmark", "denm", "lindelse", "bodilsker", "odense", "viborg",
            "denmr", "dnemark"
        },
        "sweden", new String[] {
            "swed", "swdn", "j sweden", "swe", "skonberga ostergot sweden",
            "vastebyhuseby ostergotland sweden"
        },
        "germany", new String[] {
            "alemania", "deutschland", "preussen"
        },
        "canada", new String[] {
            "ontario", "nova scotia"
        },
        "switzerland", new String[] {
            "switz"
        },
        "norway", new String[] {
            "nor", "akershus"
        },
        "scotland", new String[] {
            "scot", "sctl"
        },
        "wales", new String[] {
            "wals", "so wales"
        },
        "ireland", new String[] {
            "northern ireland"
        }
    };
    
    static final Map<String, String> invertedMappings = invertMappings(countryToAbbreviationMapping);

    private static Map<String, String> invertMappings(Object[] mapping) {
        Map<String, String> inverted = new HashMap<>();
        for (int i = 0; i < mapping.length; i+=2) {
            String canonicalized = (String) mapping[i];
            String[] range = (String[]) mapping[i+1];
            
            for (String rangeValue : range) {
                inverted.put(rangeValue, canonicalized);
            }
        }
        return inverted;
    }
    
    public static String canonicalize(String country) {
        String sanitized = country.toLowerCase()
                .replaceAll("[^a-z ]|^from |^of ", "")
                .replaceAll("\\s+", " ");
        if (invertedMappings.containsKey(sanitized)) {
            sanitized = invertedMappings.get(sanitized);
        }
        return WordUtils.capitalize(sanitized);
    }
}
