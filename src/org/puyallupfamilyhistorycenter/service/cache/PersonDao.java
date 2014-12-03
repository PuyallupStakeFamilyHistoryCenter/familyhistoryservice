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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.IteratorUtils;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonImage;

/**
 *
 * @author tibbitts
 */


public class PersonDao {
    private final Source<Person> source;

    public PersonDao(Source<Person> source) {
        this.source = source;
    }
    
    /**
     * Gets a person by id from the underlying cache
     * @param personId
     * @param accessToken
     * @return
     */
    public Person getPerson(String personId, String accessToken) {
        return source.get(personId, accessToken);
    }
    
    /**
     * Get an iterator over family records for the given person
     * @param personId The person whose family to get
     * @param accessToken 
     * @return an iterator over the family of the given person
     */
    public List<Person> listImmediateFamily(String personId, String accessToken) {
        return traverse(new FamilyIterator(personId, source, accessToken));
    }
    
    /**
     * Get an iterator over ancestor records for the given person
     * @param personId The person whose ancestry to get
     * @param maxDepth The number of levels to descend into the tree
     * @param accessToken 
     * @return an iterator over the ancestors of the given person
     */
    public List<Person> listAncestors(String personId, int maxDepth, String accessToken) {
        return traverse(new AncestorsIterator(personId, maxDepth, source, accessToken));
    }
    
    public List<Person> listAncestorsWithImages(String personId, int maxDepth, String accessToken) {
        return extractPeopleWithImages(new AncestorsIterator(personId, maxDepth, source, accessToken));
    }
    
    /**
     * Get an iterator over descendant records for the given person
     * @param personId The person whose descendants to get
     * @param maxDepth The number of levels to descend into the tree
     * @param accessToken 
     * @return an iterator over the descendants of the given person
     */
    public List<Person> listDescendants(String personId, int maxDepth, String accessToken) {
        return traverse(new DescendantsIterator(personId, maxDepth, source, accessToken));
    }
    
    private List<Person> traverse(final Iterator<Person> iterator) {
        return IteratorUtils.toList(iterator);
    }
    
    private List<Person> extractPeopleWithImages(final Iterator<Person> it) {
        List<Person> people = new ArrayList<>();
        while (it.hasNext()) {
            Person p = it.next();
            if (p.images != null && p.images.length > 0) {
                people.add(p);
            }
        }
        return people;
    } 
}
