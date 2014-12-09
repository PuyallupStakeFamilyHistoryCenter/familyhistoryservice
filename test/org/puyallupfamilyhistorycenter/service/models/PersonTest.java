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
package org.puyallupfamilyhistorycenter.service.models;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author tibbitts
 */

@RunWith(Parameterized.class)
public class PersonTest {
    
    @Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            { "{\"living\":true}", new PersonBuilder().build() },
            { "{\"id\":\"asdf-sdf\",\"living\":true}", new PersonBuilder().withId("asdf-sdf").build() },
            { "{\"name\":\"Graham Trey Tibbitts\",\"living\":true}", new PersonBuilder().withName("Graham Trey Tibbitts").build() },
            { "{\"gender\":\"male\",\"living\":true}", new PersonBuilder().withGender("male").build() },
            { 
                "{\"living\":true,\"facts\":[{\"type\":\"birth\",\"date\":\"23 December 1897\",\"sortableDate\":\"18971223\",\"place\":\"San Jacinto, Riverside, California, United States\"}]}", 
                new PersonBuilder().withFacts(new Fact[] {new Fact("birth", "23 December 1897", "+18971223", 1897, "San Jacinto, Riverside, California, United States")}).build() 
            },
            { 
                "{\"living\":true,\"parents\":[{\"id\":\"asdf-sdf\",\"name\":\"William Datus Tibbitts\",\"relationship\":\"father\"}]}", 
                new PersonBuilder().withParents(new PersonReference[] {new PersonReference("asdf-sdf", "William Datus Tibbitts", "father")}).build() 
            },
            { 
                "{\"living\":true,\"spouses\":[{\"id\":\"asdf-sdf\",\"name\":\"Katrina Kay Huffman\",\"relationship\":\"wife\"}]}", 
                new PersonBuilder().withSpouses(new PersonReference[] {new PersonReference("asdf-sdf", "Katrina Kay Huffman", "wife")}).build() 
            },
            { 
                "{\"living\":true,\"children\":[{\"id\":\"asdf-sdf\",\"name\":\"Allison Kay Huffman\",\"relationship\":\"daughter\"}]}", 
                new PersonBuilder().withChildren(new PersonReference[] {new PersonReference("asdf-sdf", "Allison Kay Huffman", "daughter")}).build() 
            },
            { "{\"id\":\"asdf-sdf\","
                    + "\"name\":\"Graham Trey Tibbitts\","
                    + "\"gender\":\"male\","
                    + "\"living\":true,"
                    + "\"facts\":[{\"type\":\"birth\",\"date\":\"23 December 1897\",\"sortableDate\":\"18971223\",\"place\":\"San Jacinto, Riverside, California, United States\"}],"
                    + "\"parents\":[{\"id\":\"asdf-sdf\",\"name\":\"William Datus Tibbitts\",\"relationship\":\"father\"}],"
                    + "\"spouses\":[{\"id\":\"asdf-sdf\",\"name\":\"Katrina Kay Huffman\",\"relationship\":\"wife\"}],"
                    + "\"children\":[{\"id\":\"asdf-sdf\",\"name\":\"Allison Kay Huffman\",\"relationship\":\"daughter\"}]}", 
                new PersonBuilder().withId("asdf-sdf")
                        .withName("Graham Trey Tibbitts")
                        .withGender("male")
                        .withFacts(new Fact[] {new Fact("birth", "23 December 1897", "+18971223", 1897, "San Jacinto, Riverside, California, United States")})
                        .withParents(new PersonReference[] {new PersonReference("asdf-sdf", "William Datus Tibbitts", "father")})
                        .withSpouses(new PersonReference[] {new PersonReference("asdf-sdf", "Katrina Kay Huffman", "wife")})
                        .withChildren(new PersonReference[] {new PersonReference("asdf-sdf", "Allison Kay Huffman", "daughter")})
                        .build() 
            },
        });
    }
    
    private static final Gson GSON = new Gson();
    private final String json;
    private final Person person;
    public PersonTest(String json, Person person) {
        this.person = person;
        this.json = json;
    }
    
    @Test
    public void testSerialize() {
        String serialized = GSON.toJson(person);
        assertEquals(json, serialized);
    }
    
    @Test
    public void testDeserialize() {
        Person deserialized = GSON.fromJson(json, Person.class);
        assertEquals(person, deserialized);
    }
    
}
