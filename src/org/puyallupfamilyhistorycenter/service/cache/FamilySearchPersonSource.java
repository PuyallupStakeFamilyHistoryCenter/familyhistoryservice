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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.familysearch.api.client.ft.FamilyTreePersonState;
import org.gedcomx.conclusion.Name;
import org.gedcomx.conclusion.Relationship;
import org.gedcomx.rs.client.PersonChildrenState;
import org.gedcomx.rs.client.PersonParentsState;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.SourceDescriptionsState;
import org.gedcomx.source.SourceDescription;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
            //TODO: See if we can cache this FamilySearchFamilyTree instance
            FamilySearchFamilyTree ft = FamilyHistoryFamilyTree.getInstance(accessToken);
            FamilyTreePersonState state = ft.readPersonById(personId);

            org.gedcomx.conclusion.Person originalPerson = state.getEntity().getPerson();
            PersonBuilder builder = new PersonBuilder();
            
            /* Get for living and for deceased people */
            builder.withId(personId);
            builder.withLiving(originalPerson.getLiving());
            builder.withGender(originalPerson.getGender().getKnownType().name());

            {
                PersonParentsState parentsState = state.readParents();
                builder.withParents(fsPersonsToPersonRefs(parentsState.getPersons(), null));
            }

            PersonSpousesState spousesState = state.readSpouses();
            List<org.gedcomx.conclusion.Person> spousePersons = spousesState.getPersons();
            if (spousePersons != null) {
                builder.withSpouses(fsPersonsToPersonRefs(spousePersons.subList(1, spousePersons.size()), spousesState.getRelationships()));
            }

            {
                PersonChildrenState childrenState = state.readChildren();
                builder.withChildren(fsPersonsToPersonRefs(childrenState.getPersons(), null));
            }
            
            if (originalPerson.getLiving()) {
                builder.withName("Living");
            } else {
                Name name = state.getName();
                if (name != null) {
                    builder.withName(name.getNameForm().getFullText());
                }

                {
                    List<org.gedcomx.conclusion.Fact> originalFacts = originalPerson.getFacts();
                    Fact[] facts = new Fact[originalFacts.size()];
                    int index = 0;
                    for (org.gedcomx.conclusion.Fact originalFact : originalFacts) {
                        String sortableDate = originalFact.getDate() == null ? null : originalFact.getDate().getFormal();
                        String place = originalFact.getPlace() == null ? null : originalFact.getPlace().getOriginal();
                        ParsedDate parsedDate = formatDate(sortableDate);
                        facts[index++] = new Fact(
                                originalFact.getKnownType().name(), 
                                parsedDate.formatted, 
                                sortableDate,
                                parsedDate.year,
                                place);
                    }

                    builder.withFacts(facts);
                }

                {
                    SourceDescriptionsState sourceState = state.readArtifacts();
                    List<SourceDescription> sources = sourceState.getSourceDescriptions();
                    if (sources != null) {
                        String[] imageUrls = new String[sources.size()];
                        int i = 0;
                        for (SourceDescription source : sources) {
                            //source.getAbout() is the url to the image
                            imageUrls[i++] = "/image-cache?ref=" + URLEncoder.encode(source.getAbout().toString() + "&access_token=" + accessToken, StandardCharsets.UTF_8.name());
                        }
                        builder.withImages(imageUrls);
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            throw new NotFoundException("Failed to load person for id " + personId, e);
        }
    }

    @Override
    public boolean has(String id) {
        return true;
    }

    private PersonReference[] fsPersonsToPersonRefs(List<org.gedcomx.conclusion.Person> persons, List<Relationship> relationships) {
        if (persons == null) {
            return null;
        }

        PersonReference[] refs = new PersonReference[persons.size()];
        for (int i = 0; i < persons.size(); i++) {
            org.gedcomx.conclusion.Person person = persons.get(i);
            
            if (person.getLiving()) {
                refs[i] = new PersonReference(person.getId(), "Living", null);
            } else {
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
                            String sortableDate = originalFact.getDate() == null ? null : originalFact.getDate().getFormal();
                            String place = originalFact.getPlace() == null ? null : originalFact.getPlace().getOriginal();
                            String type = originalFact.getKnownType() == null ? "UNKNOWN" : originalFact.getKnownType().name();
                            ParsedDate parsedDate = formatDate(sortableDate);
                            facts[index++] = new Fact(
                                    type, 
                                    parsedDate.formatted, 
                                    sortableDate,
                                    parsedDate.year,
                                    place);
                        }

                        refs[i].withFacts(facts);
                    }
                }
            }
            
            if (person.getGender() != null && person.getGender().getKnownType() != null) {
                refs[i].withGender(person.getGender().getKnownType().name());
            }
        }
        return refs;
    }

    private static FormatterCollection[] formatters = {
        new FormatterCollection(Pattern.compile("\\+\\d+\\-\\d+\\-\\d+"),
                DateTimeFormat.forPattern("+yyyy-M-d").withZoneUTC(), 
                DateTimeFormat.forPattern("d MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("\\+\\d+\\-\\d+"),
                DateTimeFormat.forPattern("+yyyy-M").withZoneUTC(), 
                DateTimeFormat.forPattern("MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("\\+\\d+"),
                DateTimeFormat.forPattern("+yyyy").withZoneUTC(), 
                DateTimeFormat.forPattern("yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("A\\+\\d+\\-\\d+\\-\\d+"),
                DateTimeFormat.forPattern("'A'+yyyy-M-d").withZoneUTC(), 
                DateTimeFormat.forPattern("'around' d MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("A\\+\\d+\\-\\d+"),
                DateTimeFormat.forPattern("'A'+yyyy-M").withZoneUTC(), 
                DateTimeFormat.forPattern("'around' MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("A\\+\\d+"),
                DateTimeFormat.forPattern("'A'+yyyy").withZoneUTC(), 
                DateTimeFormat.forPattern("'around' yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("/\\+\\d+\\-\\d+\\-\\d+"),
                DateTimeFormat.forPattern("/+yyyy-M-d").withZoneUTC(), 
                DateTimeFormat.forPattern("'after' d MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("/\\+\\d+\\-\\d+"),
                DateTimeFormat.forPattern("/+yyyy-M").withZoneUTC(), 
                DateTimeFormat.forPattern("'after' MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("/\\+\\d+"),
                DateTimeFormat.forPattern("/+yyyy").withZoneUTC(), 
                DateTimeFormat.forPattern("'after' yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("\\+\\d+\\-\\d+\\-\\d+/"),
                DateTimeFormat.forPattern("+yyyy-M-d/").withZoneUTC(), 
                DateTimeFormat.forPattern("'before' d MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("\\+\\d+\\-\\d+/"),
                DateTimeFormat.forPattern("+yyyy-M/").withZoneUTC(), 
                DateTimeFormat.forPattern("'before' MMMM yyy").withZoneUTC()),
        new FormatterCollection(Pattern.compile("\\+\\d+/"),
                DateTimeFormat.forPattern("+yyyy/").withZoneUTC(), 
                DateTimeFormat.forPattern("'before' yyy").withZoneUTC()),
    };
    protected static final ParsedDate nullDate = new ParsedDate(null, null);
    protected static ParsedDate formatDate(String sortableDate) {
        if (sortableDate == null) {
            return nullDate;
        }
        
        FormatterCollection collection = null;
        for (FormatterCollection c : formatters) {
            if (c.pattern.matcher(sortableDate).matches()) {
                collection = c;
                break;
            }
        }
        
        if (collection == null) {
            logger.warn(sortableDate + " doesn't match any known date format");
            return nullDate;
        }
        
        DateTime inputDate = collection.inputFormatter.parseDateTime(sortableDate);
        ParsedDate date = new ParsedDate(inputDate.getYear(), collection.outputFormatter.print(inputDate));
        
        return date;
    }
    
    private static class FormatterCollection {
        public final Pattern pattern;
        public final DateTimeFormatter inputFormatter;
        public final DateTimeFormatter outputFormatter;

        public FormatterCollection(Pattern pattern, DateTimeFormatter inputFormatter, DateTimeFormatter outputFormatter) {
            this.pattern = pattern;
            this.inputFormatter = inputFormatter;
            this.outputFormatter = outputFormatter;
        }
    }
    
    public static class ParsedDate {
        public final Integer year;
        public final String formatted;

        public ParsedDate(Integer year, String formatted) {
            this.year = year;
            this.formatted = formatted;
        }
    }
}
