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
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author tibbitts
 */

@RunWith(Parameterized.class)
public class PersonReferenceTest {
    
    private static final Gson gson = new Gson();
    private final PersonReference personRef;
    private final String json;
    
    @Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            { "{}", new PersonReference(null, null, null) },
            { "{\"id\":\"asdf-sdf\"}", new PersonReference("asdf-sdf", null, null) },
            { "{\"name\":\"Graham Trey Tibbitts\"}", new PersonReference(null, "Graham Trey Tibbitts", null) },
            { "{\"relationship\":\"son\"}", new PersonReference(null, null, "son") },
            { "{\"id\":\"asdf-sdf\",\"name\":\"Graham Trey Tibbitts\",\"relationship\":\"son\"}", new PersonReference("asdf-sdf", "Graham Trey Tibbitts", "son") },
        });
    }
    
    public PersonReferenceTest(String json, PersonReference personRef) {
        this.personRef = personRef;
        this.json = json;
    }
    
    @Test
    public void testSerialize() {
        String serialized = gson.toJson(personRef);
        assertEquals(json, serialized);
    }
    
    @Test
    public void testDeserialize() {
        PersonReference deserialized = gson.fromJson(json, PersonReference.class);
        assertEquals(personRef, deserialized);
    }
}
