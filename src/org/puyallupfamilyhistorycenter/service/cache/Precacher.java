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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.PersonState;
import org.puyallupfamilyhistorycenter.service.ApplicationProperties;
import org.puyallupfamilyhistorycenter.service.SpringContextInitializer;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;
import org.puyallupfamilyhistorycenter.service.models.PersonTemple;
import org.puyallupfamilyhistorycenter.service.websocket.FamilyHistoryFamilyTree;

/**
 *
 * @author tibbitts
 */
public class Precacher {
    private static final Logger logger = Logger.getLogger(Precacher.class);
    private static final Source<Person> source;
    private static final Source<PersonTemple> templeSource;
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final ExecutorService imageCacheExecutor = Executors.newFixedThreadPool(1);

    static {
        source = (Source<Person>) SpringContextInitializer.getContext().getBean("in-memory-source");
        templeSource = (Source<PersonTemple>) SpringContextInitializer.getContext().getBean("temple-source");
    }

    private static class PrecacheObject {

        public final String id;
        public final int depth;

        public PrecacheObject(String id, int depth) {
            this.id = id;
            this.depth = depth;
        }
    }

    private final String userId;
    private final String accessToken;
    private final int maxDepth;
    private final Set<Future> futures;
    private final Set<PrecacheListener> listeners;
//    private final List<PersonTemple> prospects;

    public Precacher(String userId, String accessToken, int maxDepth) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.maxDepth = maxDepth;
        this.futures = new HashSet<>();
        this.listeners = new HashSet<>();
//        this.prospects = new LinkedList<>();
    }

    public void precache() {
        final FamilySearchFamilyTree tree = FamilyHistoryFamilyTree.getInstance(accessToken);
        final Queue<PrecacheObject> frontier = new ConcurrentLinkedQueue<>();
        final Queue<PrecacheObject> leafNodes = new ConcurrentLinkedQueue<>();
        final Set<String> currentLeafs = Collections.synchronizedSet(new HashSet<String>());
        final AtomicInteger totalPrecached = new AtomicInteger();
        
        PersonState person = tree.readPersonForCurrentUser();
        frontier.add(new PrecacheObject(person.getPerson().getId(), 0));

        PersonSpousesState spouses = person.readSpouses();
        if (spouses != null && spouses.getPersons() != null) {
            for (org.gedcomx.conclusion.Person spouse : spouses.getPersons()) {
                frontier.add(new PrecacheObject(spouse.getId(), 0));
            }
        }
        
        String guestPersonId = ApplicationProperties.getGuestPersonId();
        frontier.add(new PrecacheObject(guestPersonId, 0));
        
        final AtomicInteger minEstimatedUnvistited = new AtomicInteger(frontier.size() * (int) Math.pow(2, maxDepth));
        
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(new Runnable() {

                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        while (!frontier.isEmpty()) {
                            PrecacheObject precacheObject = frontier.remove();
                            try {
                                Person person = source.get(precacheObject.id, accessToken);
                                if (person.parents != null && precacheObject.depth < maxDepth) {
                                    for (PersonReference parent : person.parents) {
                                        logger.info(Thread.currentThread().getName() + ": Adding parent " + parent.getName() + " to frontier level " + (precacheObject.depth + 1));
                                        frontier.add(new PrecacheObject(parent.getId(), precacheObject.depth + 1));
                                    }
                                } else {
                                    logger.info(Thread.currentThread().getName() + ": Adding self " + person.name + " to leaf nodes");
                                    leafNodes.add(precacheObject);
                                }
                                
//                                if (person.images != null) {
//                                    for (final String imageKey : person.images) {
//                                        imageCacheExecutor.submit(new Runnable() {
//
//                                            @Override
//                                            public void run() {
//                                                imageSource.get(new KeyAndHeaders(imageKey, null), accessToken);
//                                            }
//                                        });
//                                    }
//                                }
//                                PersonTemple personTemple = templeSource.get(precacheObject.id, accessToken);
//                                if (personTemple != null && personTemple.hasOrdinancesReady()) {
//                                    prospects.add(personTemple);
//                                } 
                                
                                int totalPrecachedValue = totalPrecached.incrementAndGet();
                                int queueSize = frontier.size();
                                int currentGeneration = precacheObject.depth;
                                
                                int estimatedUnvisited = queueSize * (int) Math.pow(2, maxDepth - currentGeneration) - queueSize;
                                if (estimatedUnvisited < minEstimatedUnvistited.get()) {
                                    minEstimatedUnvistited.set(estimatedUnvisited);
                                }
                                
                                
                                PrecacheEvent event = new PrecacheEvent(userId, totalPrecachedValue, queueSize, minEstimatedUnvistited.get(), currentGeneration);
                                for (PrecacheListener listener : listeners) {
                                    listener.onPrecache(event);
                                }
                            } catch (Exception ex) {
                                logger.warn("Failed to get person " + precacheObject.id + "; skipping", ex);
                            }
                        }
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            Thread.interrupted();
                        }
                    }

                    //TODO: Use depth-first search to find people with unfinished ordinances
    //                    while(!leafNodes.isEmpty()) {
    //                        PrecacheObject precacheObject = leafNodes.remove();
    //                        currentLeafs.remove(precacheObject.id);
    //                        Person person = source.get(precacheObject.id, accessToken);
    //                        if (person.children != null) {
    //                            for (PersonReference child : person.children) {
    //                                if (!currentLeafs.contains(child.getId())) {
    //                                    logger.info("Adding child " + child.getName() + " to frontier");
    //                                    leafNodes.add(new PrecacheObject(child.getId(), precacheObject.depth - 1));
    //                                    currentLeafs.add(child.getId());
    //                                }
    //                            }
    //                        } else {
    //                            logger.info("Person " + person.name + " has no children on record");
    //                        }
    //                    }
                    
                    for (PrecacheListener listener : listeners) {
                        listener.onFinish();
                    }
                }
            }));
        }
    }

    public void cancel() {
        for (Future future : futures) {
            future.cancel(true);
        }
        for (PrecacheListener listener : listeners) {
            listener.onCancel();
        }
        logger.info("Cancelling precaching");
    }
    
    public void addPrecacheListener(PrecacheListener listener) {
        listeners.add(listener);
    }
    
    public static interface PrecacheListener {
        void onPrecache(PrecacheEvent event);
        void onFinish();
        void onCancel();
    }
    
    public static class PrecacheEvent {
        public final String userId;
        public final String responseType = "precacheEvent";
        public final int totalCached;
        public final int totalQueueSize;
        public final int estimatedUnvisited;
        
        public final int currentGeneration;

        public PrecacheEvent(String userId, int totalCached, int totalQueueSize, int estimatedUnvisited, int currentGeneration) {
            this.userId = userId;
            this.totalCached = totalCached;
            this.totalQueueSize = totalQueueSize;
            this.estimatedUnvisited = estimatedUnvisited;
            this.currentGeneration = currentGeneration;
        }
    }
    
    public List<PersonTemple> getProspects() {
        return null; //Collections.unmodifiableList(prospects);
    }
}
