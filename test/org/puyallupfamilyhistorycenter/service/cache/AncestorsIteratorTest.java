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

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonBuilder;

/**
 *
 * @author tibbitts
 */
@RunWith(Parameterized.class)
public class AncestorsIteratorTest {
    
    @Parameterized.Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(
            new Object[][] {
                {0,"Female","self"},
                {0,"Male","self"},
                {1,"Female","mother"},
                {1,"Male","father"},
                {2,"Female","grandmother"},
                {2,"Male","grandfather"},
                {3,"Female","great-grandmother"},
                {3,"Male","great-grandfather"},
                {4,"Female","great-great-grandmother"},
                {4,"Male","great-great-grandfather"},
            }
        );
    }
    
    private final int depth;
    private final String gender;
    private final String expectedRelationship;
    public AncestorsIteratorTest(int depth, String gender, String expectedRelationship) {
        this.depth = depth;
        this.gender = gender;
        this.expectedRelationship = expectedRelationship;
    }

    /**
     * Test of addRelationshipToPerson method, of class AncestorsIterator.
     */
    @Test
    public void testAddRelationshipToPerson() {
        System.out.println("addRelationshipToPerson");
        Person person = new PersonBuilder().withGender(gender).build();
        AncestorsIterator.addRelationshipToPerson(person, depth);
        assertEquals(expectedRelationship, person.relationship);
    }
    
}
