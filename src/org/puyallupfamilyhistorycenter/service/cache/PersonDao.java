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

import java.io.IOException;
import java.util.Iterator;
import org.puyallupfamilyhistorycenter.service.models.Person;

/**
 *
 * @author tibbitts
 */


public class PersonDao {
    public final Source<Person> source;

    public PersonDao(Source<Person> source) {
        this.source = source;
    }
    
    /**
     * Gets a person by id from the underlying cache
     * @param personId
     * @return
     */
    public Person getPerson(String personId) {
        return source.get(personId);
    }
    
    public Iterator<Person> traverseImmediateFamily(String personId, int pageSize, String lastPageEndId) {
        return traverse(personId, pageSize, lastPageEndId, "IMMEDIATE_FAMILY");
    }
    
    /**
     * Get an input stream of ancestor records for the given person
     * @param personId The person whose ancestry to get
     * @param pageSize The number of records to return wit each request
     * @param lastPageEndId The id of the final record in the previous request
     *                      no previous request exists)
     * @return an input
     */
    public Iterator<Person> traverseAncestors(String personId, int pageSize, String lastPageEndId) {
        return traverse(personId, pageSize, lastPageEndId, "ANCESTORS");
    }
    
    public Iterator<Person> traverseDescendants(String personId, int pageSize, String lastPageEndId) {
        return traverse(personId, pageSize, lastPageEndId, "DESCENDANTS");
    }
    
    private Iterator<Person> traverse(String personId, int pageSize, final String lastPageEndId, String strategy) {
        return new Iterator<Person>() {
            int currentPageIndex = 0;
            String currentId = lastPageEndId;
            
            @Override
            public boolean hasNext() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Person next() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
}
