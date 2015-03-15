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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import org.jboss.logging.Logger;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonBuilder;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;

/**
 *
 * @author tibbitts
 */


public class DescendantsIterator implements Iterator<Person> {
    private static final Logger logger = Logger.getLogger(AncestorsIterator.class);

    private final String personId;
    private final int maxDepth;
    private final Source<String, Person> source;
    private final String accessToken;
    private final Queue<PersonReference> frontier = new LinkedList<>();
    
    DescendantsIterator(String personId, int maxDepth, Source<String, Person> source, String accessToken) {
        this.personId = personId;
        this.maxDepth = maxDepth;
        this.source = source;
        this.accessToken = accessToken;
        
        Person root = source.get(personId, accessToken);
        if (root != null) {
            frontier.add(new PersonReference(personId, root.name, 0));
        }
    }

    @Override
    public boolean hasNext() {
        return !frontier.isEmpty();
    }

    @Override
    public Person next() {
        PersonReference next = null;
        Person person = null;
        while (person == null) {
            try {
                next = frontier.remove();
                person = source.get(next.getId(), accessToken);
            } catch (Exception e) {
                logger.warn("Failed to get person " + next.getId() + "; attempting to recover", e);
                person = new PersonBuilder().withName(next.getName()).withId(next.getId()).withGender(next.getGender()).build();
            }
        }
        
        if (next.getDepth() < maxDepth) {
            if (person.children != null) {
                for (PersonReference child : person.children) {
                    frontier.add(child.withDepth(next.getDepth() + 1));
                }
            }
        }
        
        return person;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }
    
}
