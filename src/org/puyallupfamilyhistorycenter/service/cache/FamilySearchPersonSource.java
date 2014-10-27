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
import com.sun.istack.logging.Logger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.familysearch.api.client.ft.FamilyTreePersonState;
import org.gedcomx.conclusion.Name;
import org.gedcomx.conclusion.Relationship;
import org.gedcomx.rs.client.PersonChildrenState;
import org.gedcomx.rs.client.PersonParentsState;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.source.SourceDescription;
import org.puyallupfamilyhistorycenter.service.models.Fact;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonBuilder;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;
import org.puyallupfamilyhistorycenter.service.websocket.FamilyHistoryFamilyTree;

/**
 *
 * @author tibbitts
 */
public class FamilySearchPersonSource implements Source<Person> {

    private static final Logger logger = Logger.getLogger(FamilySearchPersonSource.class);
    private static final Gson GSON = new Gson();

    @Override
    public Person get(String personId, String accessToken) {
        try {
            FamilySearchFamilyTree ft = FamilyHistoryFamilyTree.getInstance(accessToken);
            FamilyTreePersonState state = ft.readPersonById(personId);

            org.gedcomx.conclusion.Person originalPerson = state.getEntity().getPerson();
            PersonBuilder builder = new PersonBuilder();
            Name name = state.getName();
            builder.withId(personId);
            if (name != null) {
                builder.withName(name.getNameForm().getFullText());
            }
            builder.withLiving(originalPerson.getLiving());
            builder.withGender(originalPerson.getGender().getKnownType().name());

            {
                List<org.gedcomx.conclusion.Fact> originalFacts = originalPerson.getFacts();
                Fact[] facts = new Fact[originalFacts.size()];
                int index = 0;
                for (org.gedcomx.conclusion.Fact originalFact : originalFacts) {
                    String date = originalFact.getDate() == null ? null : originalFact.getDate().getOriginal();
                    String sortableDate = originalFact.getDate() == null ? null : originalFact.getDate().getFormal();
                    String place = originalFact.getPlace() == null ? null : originalFact.getPlace().getOriginal();
                    facts[index++] = new Fact(
                            originalFact.getKnownType().name(), 
                            date, 
                            sortableDate,
                            place);
                }
                
                builder.withFacts(facts);
            }

            {
                PersonParentsState parentsState = state.readParents();
                builder.withParents(fsPersonsToPersonRefs(parentsState.getPersons(), null));
            }

            {
                PersonSpousesState spousesState = state.readSpouses();
                builder.withSpouses(fsPersonsToPersonRefs(spousesState.getPersons(), spousesState.getRelationships()));
            }

            {
                PersonChildrenState childrenState = state.readChildren();
                builder.withChildren(fsPersonsToPersonRefs(childrenState.getPersons(), null));
            }

            {
                SourceDescriptionsState sourceState = state.readArtifacts();
                List<SourceDescription> sources = sourceState.getSourceDescriptions();
                if (sources != null) {
                    String[] imageUrls = new String[sources.size()];
                    int i = 0;
                    for (SourceDescription source : sources) {
                        //source.getAbout() is the url to the image
                        imageUrls[i++] = "/image-cache?ref=" + URLEncoder.encode(source.getAbout().toString(), StandardCharsets.UTF_8.name());
                    }
                    builder.withImages(imageUrls);
                }
            }

            return builder.build();
        } catch (Exception e) {
            throw new NotFoundException("Failed to load person for id " + personId, e);
        }
    }

    private PersonReference[] fsPersonsToPersonRefs(List<org.gedcomx.conclusion.Person> persons, List<Relationship> relationships) {
        if (persons == null) {
            return null;
        }

        PersonReference[] refs = new PersonReference[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            org.gedcomx.conclusion.Person person = persons.get(i);
            Name name = person.getName();
            String stringName = name == null ? null : name.getNameForm().getFullText();
            refs[i] = new PersonReference(person.getId(), stringName, null);
            
            if (relationships != null && relationships.size() > i) {
                Relationship relationship = relationships.get(i);
                if (relationship.getFacts() != null) {
                    List<org.gedcomx.conclusion.Fact> originalFacts = relationship.getFacts();
                    Fact[] facts = new Fact[originalFacts.size()];
                    int index = 0;
                    for (org.gedcomx.conclusion.Fact originalFact : originalFacts) {
                        String date = originalFact.getDate() == null ? null : originalFact.getDate().getOriginal();
                        String place = originalFact.getPlace() == null ? null : originalFact.getPlace().getOriginal();
                        facts[index++] = new Fact(
                                originalFact.getKnownType().name(), 
                                date, 
                                null, //TODO: Extract real timestamp from this
                                place);
                    }

                    refs[i].withFacts(facts);
                }
            }
        }
        return refs;
    }
}
