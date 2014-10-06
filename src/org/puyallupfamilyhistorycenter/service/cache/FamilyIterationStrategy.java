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
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.gedcomx.rs.client.options.Preconditions;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;

/**
 *
 * @author tibbitts
 */


public class FamilyIterationStrategy implements IterationStrategy<Person> {

    private static enum State {
        PARENTS("SPOUSES") {
            @Override
            public Iterator<PersonReference> iterate(Person root) {
                if (root.parents == null) return null;
                return Arrays.asList(root.parents).iterator();
            }
        },
        SPOUSES("CHILDREN") {

            @Override
            public Iterator<PersonReference> iterate(Person root) {
                if (root.spouses == null) return null;
                return Arrays.asList(root.spouses).iterator();
            }
        },
        CHILDREN(null) {

            @Override
            public Iterator<PersonReference> iterate(Person root) {
                if (root.children == null) return null;
                return Arrays.asList(root.children).iterator();
            }
        };

        private final String nextStateString;
        private State(String nextStateString) {
            this.nextStateString = nextStateString;
        }
        
        public State next() {
            if (nextStateString == null) return null;
            return valueOf(nextStateString);
        }
        
        public abstract Iterator<PersonReference> iterate(Person root);
    }
    
    private final Person root;
    private final Source<Person> source;
    private final String accessToken;
    private final Iterator<PersonReference> iterator;
    private PersonReference innerCurrent;
    
    public FamilyIterationStrategy(final Person root, Source<Person> source, String accessToken) {
        if (root == null || source == null) {
            throw new IllegalArgumentException("Root and source are required");
        }
        
        this.root = root;
        this.source = source;
        this.accessToken = accessToken;
        
        iterator = new Iterator<PersonReference>() {
            PersonReference next = new PersonReference(root.id, root.name, "self");
            State state = State.PARENTS;
            Iterator<PersonReference> innerIt = state.iterate(root);
            
            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public PersonReference next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                
                PersonReference current = next;
                if (innerIt != null && innerIt.hasNext()) {
                    next = innerIt.next();
                } else {
                    next = null;
                    while (state != null && (state = state.next()) != null && next == null) {
                        innerIt = state.iterate(root);
                        if (innerIt != null) {
                            next = innerIt.next();
                        }
                    }
                }
                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    @Override
    public Person next(Person current) {
        if (current != null) {
            while (iterator.hasNext() && (innerCurrent == null && !current.id.equals(innerCurrent.getId()))) {
                innerCurrent = iterator.next();
            }
        }
        
        if (iterator.hasNext()) {
            innerCurrent = iterator.next();
        } else {
            innerCurrent = null;
        }
        
        if (innerCurrent != null) {
            return source.get(innerCurrent.getId(), accessToken);
        }
        
        return null;
    }
    
}
