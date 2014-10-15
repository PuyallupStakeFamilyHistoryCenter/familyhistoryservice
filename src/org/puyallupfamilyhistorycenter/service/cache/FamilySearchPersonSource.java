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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.gedcomx.conclusion.Fact;
import org.gedcomx.conclusion.Name;
import org.gedcomx.rs.client.PersonChildrenState;
import org.gedcomx.rs.client.PersonParentsState;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.PersonState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.source.SourceDescription;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonBuilder;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;
import org.puyallupfamilyhistorycenter.service.websocket.FamilyHistoryFamilyTree;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author tibbitts
 */


public class FamilySearchPersonSource implements Source<Person> {
    private static final Gson GSON = new Gson();
    private URI uri;
    
    @Value("${environment}")
    String environment;

    public FamilySearchPersonSource() {
    }
    
    private URI getURI() {
        if (uri == null) {
            try {
                uri = new URI("https://" + environment + ".familysearch.org/org/platform/collections/tree");
            } catch (URISyntaxException ex) {
                throw new IllegalStateException("Failed to create FamilySearch URI", ex);
            }
        }
        
        return uri;
    }

    @Override
    public Person get(String personId, String accessToken) {
        FamilySearchFamilyTree ft = new FamilyHistoryFamilyTree(getURI()).authenticate(accessToken);
        PersonState state = ft.readPersonById(personId);
        
        PersonBuilder builder = new PersonBuilder();
        Name name = state.getName();
        builder.withId(personId);
        builder.withName(name.getNameForm().getFullText());
        builder.withLiving(false); //TODO: Set is living
        builder.withGender(state.getGender().getKnownType().name());
        
        {
            //TODO: set facts
            List<Fact> facts = null;
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
        
        {
            SourceDescriptionsState sourceState = state.readArtifacts();
            List<SourceDescription> sources = sourceState.getSourceDescriptions();
            if (sources != null) {
                String[] imageUrls = new String[sources.size()];
                int i = 0;
                for (SourceDescription source : sources) {
                    //source.getAbout() is the url to the image
                    imageUrls[i++] = source.getAbout().toString();
                }
                builder.withImages(imageUrls);
            }
        }
        
        return builder.build();
    }

    private PersonReference[] fsPersonsToPersonRefs(List<org.gedcomx.conclusion.Person> persons) {
        if (persons == null) {
            return null;
        }
        
        PersonReference[] refs = new PersonReference[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            org.gedcomx.conclusion.Person person = persons.get(i);
            refs[i] = new PersonReference(person.getId(), person.getName().getNameForm().getFullText(), null);
        }
        return refs;
    }
}
