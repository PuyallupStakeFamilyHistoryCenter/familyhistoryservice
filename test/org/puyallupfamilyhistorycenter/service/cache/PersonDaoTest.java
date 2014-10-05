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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junit.framework.AssertionFailedError;
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
//        InMemoryCache<String, Person> cache = new InMemoryCache<>();
//        CachingSource<Person> cachingSource = new CachingSource<>(source, cache);
        dao = new PersonDao(source);
        
        family = Arrays.asList(source.get("KWCB-HZV"), source.get("KWCB-HZ2"), source.get("KWC6-X7D"), source.get("KWJJ-4XH"));
        ancestors = Arrays.asList(source.get("KWZP-8K5"), source.get("KWZP-8KG"));
        descendants = Arrays.asList(source.get("KWC6-X7D"), source.get("KJWD-Z94"), source.get("KWJJ-4XH"));
    }

    /**
     * Test of getPerson method, of class PersonCache.
     */
    @Test
    public void testGetPerson() {
        Person person = dao.getPerson("KWCB-HZV");
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
        assertIteratorsEqual(dao.traverseImmediateFamily("KWCB-HZV", 10, null), family.iterator());
    }

    /**
     * Test of traverseAncestors method, of class PersonCache.
     */
    //@Test
    public void testTraverseAncestors() {
        System.out.println("testTraverseAncestors");
        assertIteratorsEqual(dao.traverseAncestors("KWCB-HZV", 10, null), ancestors.iterator());
    }

    /**
     * Test of traverseDescendants method, of class PersonCache.
     */
    //@Test
    public void testTraverseDescendants() {
        System.out.println("testTraverseDescendants");
        assertIteratorsEqual(dao.traverseDescendants("KWCB-HZV", 10, null), descendants.iterator());
    }
    
    
    
    <E> void assertIteratorsEqual(Iterator<E> it1, Iterator<E> it2) {
        try {
            int elementCount = 0;
            while (true) {
                if (it1.hasNext() != it2.hasNext()) {
                    throw new AssertionFailedError("Iterators don't have the same number of elements at " + elementCount);
                }
                if (!it1.hasNext()) break;

                E e1 = it1.next();
                E e2 = it2.next();
                
                System.out.println("Expected: " + GSON.toJson(e1));
                System.out.println("Actual: " + GSON.toJson(e2));

                if (!e1.equals(e2)) {
                    throw new AssertionFailedError("Iterators differ at element " + elementCount);
                }
                elementCount++;
            }
        } catch (AssertionFailedError ex) {
            String message = "\nExpected: " + dumpIterator(it1) + "\nActual: " + dumpIterator(it2);
            throw new AssertionFailedError(ex.getMessage() + message);
        }
        

    }
    
    private <E> String dumpIterator(Iterator<E> it) {
        StringBuilder builder = new StringBuilder();
        if (it.hasNext()) {
            boolean first = true;
            while (it.hasNext() && builder.length() < 1024 * 1024) {
                if (!first) {
                    builder.append(",\n");
                } else {
                    first = false;
                }
                builder.append(GSON.toJson(it.next()));
            }
        } else {
            builder.append("No elements remaining");
        }
        return builder.toString();
    }
}
