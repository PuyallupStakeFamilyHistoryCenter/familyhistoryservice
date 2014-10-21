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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.PersonState;
import org.puyallupfamilyhistorycenter.service.SpringContextInitializer;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;
import org.puyallupfamilyhistorycenter.service.websocket.FamilyHistoryFamilyTree;

/**
 *
 * @author tibbitts
 */
public class Precacher {

    private static final Source<Person> source;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    static {
        source = (Source<Person>) SpringContextInitializer.getContext().getBean("in-memory-source");
    }

    private static class PrecacheObject {

        public final String id;
        public final int depth;

        public PrecacheObject(String id, int depth) {
            this.id = id;
            this.depth = depth;
        }
    }

    private final String accessToken;
    private final int maxDepth;
    private Future future;

    public Precacher(String accessToken, int maxDepth) {
        this.accessToken = accessToken;
        this.maxDepth = maxDepth;
    }

    public void precache() {
        future = executor.submit(new Runnable() {
            FamilySearchFamilyTree tree = FamilyHistoryFamilyTree.getInstance(accessToken);
            Queue<PrecacheObject> frontier = new LinkedList<>();
            Queue<PrecacheObject> leafNodes = new LinkedList<>();
            Set<String> currentLeafs = new HashSet<>();

            {
                PersonState person = tree.readPersonForCurrentUser();
                frontier.add(new PrecacheObject(person.getPerson().getId(), 0));
                
                PersonSpousesState spouses = person.readSpouses();
                for (org.gedcomx.conclusion.Person spouse : spouses.getPersons()) {
                    frontier.add(new PrecacheObject(spouse.getId(), 0));
                }
            }

            @Override
            public void run() {
                while (!frontier.isEmpty()) {
                    try {
                        PrecacheObject precacheObject = frontier.remove();
                        Person person = source.get(precacheObject.id, accessToken);
                        if (person.parents != null && precacheObject.depth < maxDepth) {
                            for (PersonReference parent : person.parents) {
                                System.out.println("Adding parent " + parent.getName() + " to frontier level " + (precacheObject.depth + 1));
                                frontier.add(new PrecacheObject(parent.getId(), precacheObject.depth + 1));
                            }
                        } else {
                            System.out.println("Adding self " + person.name + " to leaf nodes");
                            leafNodes.add(precacheObject);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                    }
                }

//                    while(!leafNodes.isEmpty()) {
//                        PrecacheObject precacheObject = leafNodes.remove();
//                        currentLeafs.remove(precacheObject.id);
//                        Person person = source.get(precacheObject.id, accessToken);
//                        if (person.children != null) {
//                            for (PersonReference child : person.children) {
//                                if (!currentLeafs.contains(child.getId())) {
//                                    System.out.println("Adding child " + child.getName() + " to frontier");
//                                    leafNodes.add(new PrecacheObject(child.getId(), precacheObject.depth - 1));
//                                    currentLeafs.add(child.getId());
//                                }
//                            }
//                        } else {
//                            System.out.println("Person " + person.name + " has no children on record");
//                        }
//                    }
            }
        });
    }

    public void cancel() {
        future.cancel(true);
        System.out.println("Cancelling precaching");
    }
}