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
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.puyallupfamilyhistorycenter.service.models.Fact;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;

/**
 *
 * @author tibbitts
 */


public class PersonDaoTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final PersonDao dao;
    private final List<Person> family;
    private final List<Person> ancestors;
    private final List<Person> descendants;
    
    public PersonDaoTest() {
        MockPersonSource source = new MockPersonSource();
        dao = new PersonDao(source);
        
        family = Arrays.asList(source.get("KWCB-HZV", ""), source.get("KWCB-HZ2", ""), source.get("KWC6-X7D", ""), source.get("KWJJ-4XH", ""));
        ancestors = Arrays.asList(source.get("KWCB-HZV", ""), source.get("KWZP-8K5", ""), source.get("KWZP-8KG", ""));
        descendants = Arrays.asList(source.get("KWCB-HZV", ""), source.get("KWC6-X7D", ""), source.get("KWJJ-4XH", ""), source.get("KJWD-Z94", ""));
    }

    /**
     * Test of getPerson method, of class PersonCache.
     */
    @Test
    public void testGetPerson() {
        Person person = dao.getPerson("KWCB-HZV", "");
        assertEquals("KWCB-HZV", person.id);
        assertEquals("Willis Aaron Dial", person.name);
        assertFalse(person.living);
        
        assertArrayEquals("Facts don't match expected: " + Arrays.deepToString(person.facts),
                new Fact[] { 
                    new Fact("birth", "23 December 1897", null, "Hooper, Weber, Utah, United States"),
                    new Fact("death", "19 January 1985", null, "Logan, Cache, Utah, United States") },
                person.facts);
        assertArrayEquals("Parents don't match expected: " + Arrays.deepToString(person.parents),
                new PersonReference[] {
                    new PersonReference("KWZP-8K5", "William Burris Dial", "father"),
                    new PersonReference("KWZP-8KG", "Temperance Lavina Moore", "mother")
                },
                person.parents);
        assertArrayEquals("Spouses don't match expected: " + Arrays.deepToString(person.spouses),
                new PersonReference[] {
                    new PersonReference("KWCB-HZ2", "Ida Lovisa Beckstrand", "wife")
                },
                person.spouses);
        assertArrayEquals("Children don't match expected: " + Arrays.deepToString(person.children),
                new PersonReference[] {
                    new PersonReference("KWC6-X7D", "Glen \"B\" Dial", "son"),
                    new PersonReference("KWJJ-4XH", "Merlin \"B\" Dial", "son")
                },
                person.children);
    }
    
    @Test
    public void testTraverseFamily() {
        System.out.println("testTraverseFamily");
        List<Person> people = dao.listImmediateFamily("KWCB-HZV", "");
        assertIdsEqual(family, people);
    }

    /**
     * Test of traverseAncestors method, of class PersonCache.
     */
    @Test
    public void testTraverseAncestors() {
        System.out.println("testTraverseAncestors");
        List<Person> people = dao.listAncestors("KWCB-HZV", 10, "");
        assertIdsEqual(ancestors, people);
    }

    /**
     * Test of traverseDescendants method, of class PersonCache.
     */
    @Test
    public void testTraverseDescendants() {
        System.out.println("testTraverseDescendants");
        List<Person> people = dao.listDescendants("KWCB-HZV", 10, "");
        assertIdsEqual(descendants, people);
    }
    
    private void assertIdsEqual(List<Person> expected, List<Person> actual) {
        List<String> expectedIds = new ArrayList<>(expected.size());
        List<String> actualIds = new ArrayList<>(actual.size());
        
        for (Person person : expected) {
            expectedIds.add(person.id);
        }
        
        for (Person person : actual) {
            actualIds.add(person.id);
        }
        
        assertEquals(expectedIds, actualIds);
    }
}
