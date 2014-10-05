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
import java.util.List;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.gedcomx.rs.client.PersonChildrenState;
import org.gedcomx.rs.client.PersonParentsState;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.PersonState;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonBuilder;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;
import org.puyallupfamilyhistorycenter.service.websocket.FamilyHistoryFamilyTree;

/**
 *
 * @author tibbitts
 */


public class FamilySearchPersonSource implements Source<Person> {
    private static final Gson GSON = new Gson();
    
    TokenProvider provider = new TokenProvider();

    @Override
    public Person get(String personId) {
        FamilySearchFamilyTree ft = new FamilyHistoryFamilyTree(true).authenticate(provider.getToken());
        PersonState state = ft.readPersonById(personId);
        PersonBuilder builder = new PersonBuilder();
        builder.withName(state.getName().getNameForm().getFullText());
        builder.withLiving(false); //TODO: Set is living
        
        {
            //TODO: set facts
        }
        
        {
            PersonParentsState parentsState = state.readParents();
            builder.withParents(fsPersonsToPersonRefs(parentsState.getPersons()));
        }
        
        {
            PersonSpousesState spousesState = state.readSpouses();
            builder.withSpouses(fsPersonsToPersonRefs(spousesState.getPersons()));
        }
        
        {
            PersonChildrenState childrenState = state.readChildren();
            builder.withChildren(fsPersonsToPersonRefs(childrenState.getPersons()));
        }
        
        return builder.build();
    }

    private PersonReference[] fsPersonsToPersonRefs(List<org.gedcomx.conclusion.Person> persons) {
        PersonReference[] refs = new PersonReference[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            org.gedcomx.conclusion.Person person = persons.get(i);
            refs[i] = new PersonReference(person.getId(), person.getName().getNameForm().getFullText(), null);
        }
        return refs;
    }
}
