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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
import org.gedcomx.rs.client.PersonSpousesState;
import org.gedcomx.rs.client.PersonState;
import org.puyallupfamilyhistorycenter.service.SpringContextInitializer;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonReference;
import org.puyallupfamilyhistorycenter.service.models.PersonTemple;
import org.puyallupfamilyhistorycenter.service.utils.managedconcurrency.ExecutorServiceFactory;
import org.puyallupfamilyhistorycenter.service.utils.managedconcurrency.TagThrottlingExecutorService;
import org.puyallupfamilyhistorycenter.service.websocket.FamilyHistoryFamilyTree;

/**
 *
 * @author tibbitts
 */
public class Precacher {
    private static final int THREAD_POOL_SIZE = 3;
    private static final int TOTAL_POOLS = 4;
    
    private static final Logger logger = Logger.getLogger(Precacher.class);
    private static final Source<Person> source;
    private static final ExecutorService executor = new TagThrottlingExecutorService(TOTAL_POOLS, new ExecutorServiceFactory() {
        @Override
        public ExecutorService create() {
            return new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        }
    });   

    static {
        source = (Source<Person>) SpringContextInitializer.getContext().getBean("in-memory-source");
    }

    private static class PrecacheObject {
        public final String rootId;
        public final String id;
        public final int depth;

        public PrecacheObject(String rootId, String id, int depth) {
            this.rootId = rootId;
            this.id = id;
            this.depth = depth;
        }
    }

    private final String userId;
    private final String accessToken;
    private final int maxDepth;
    private final Set<PrecacheListener> listeners;
//    private final List<PersonTemple> prospects;

    public Precacher(String userId, String accessToken, int maxDepth) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.maxDepth = maxDepth;
        this.listeners = new HashSet<>();
//        this.prospects = new LinkedList<>();Os
    }

    public void precache() {
        final FamilySearchFamilyTree tree = FamilyHistoryFamilyTree.getInstance(accessToken);
        final AtomicInteger totalPrecached = new AtomicInteger();
        final AtomicInteger totalInFlight = new AtomicInteger();
        final AtomicInteger minEstimatedUnvisited = new AtomicInteger((int) Math.pow(2, maxDepth + 1));
        
        PersonState person = tree.readPersonForCurrentUser();
        totalInFlight.incrementAndGet();
        executor.submit(new PrecacheTask(new PrecacheObject(person.getPerson().getId(), person.getPerson().getId(), 0), totalPrecached, totalInFlight, minEstimatedUnvisited));
        
//        PersonSpousesState spouses = person.readSpouses();
//        if (spouses != null && spouses.getPersons() != null) {
//            for (org.gedcomx.conclusion.Person spouse : spouses.getPersons()) {
//                totalInFlight.incrementAndGet();
//                executor.submit(new PrecacheTask(new PrecacheObject(person.getPerson().getId(), spouse.getId(), 0), totalPrecached, totalInFlight, minEstimatedUnvisited));
//            }
//        }
        
        //String guestPersonId = ApplicationProperties.getGuestPersonId();
        //frontier.add(new PrecacheObject(guestPersonId, 0));
    }

    public void cancel() {
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
    
    private class PrecacheTask implements TagThrottlingExecutorService.TaggedRunnable {
        
        private final PrecacheObject precacheObject;
        //private String userId;
        private final AtomicInteger totalPrecached;
        private final AtomicInteger totalInFlight;
        private final AtomicInteger minEstimatedUnvisited;
        
        public PrecacheTask(PrecacheObject precacheObject, AtomicInteger totalPrecached, AtomicInteger totalInFlight, AtomicInteger minEstimatedUnvisited) {
            this.precacheObject = precacheObject;
            this.totalPrecached = totalPrecached;
            this.totalInFlight = totalInFlight;
            this.minEstimatedUnvisited = minEstimatedUnvisited;
        }

        @Override
        public String getTag() {
            return precacheObject.rootId;
        }

        @Override
        public void run() {
            try {
                Person person = source.get(precacheObject.id, accessToken);
                if (person != null && person.parents != null && precacheObject.depth < maxDepth) {
                    for (PersonReference parent : person.parents) {
                        logger.info(Thread.currentThread().getName() + ": Adding parent " + parent.getName() + " to frontier level " + (precacheObject.depth + 1));
                        executor.submit(new PrecacheTask(new PrecacheObject(precacheObject.rootId, parent.getId(), precacheObject.depth + 1), totalPrecached, totalInFlight, minEstimatedUnvisited));
                        totalInFlight.incrementAndGet();
                    }
                }

                int totalPrecachedValue = totalPrecached.incrementAndGet();
                int queueSize = totalInFlight.decrementAndGet();
                int currentGeneration = precacheObject.depth;

                int estimatedUnvisited = queueSize * (int) Math.pow(2, maxDepth - currentGeneration) - queueSize;
                if (estimatedUnvisited < minEstimatedUnvisited.get()) {
                    minEstimatedUnvisited.set(estimatedUnvisited);
                }


                PrecacheEvent event = new PrecacheEvent(userId, totalPrecachedValue, queueSize, minEstimatedUnvisited.get(), currentGeneration);
                for (PrecacheListener listener : listeners) {
                    listener.onPrecache(event);
                }
            } catch (NotFoundException ex) {
                logger.warn("Failed to get person " + precacheObject.id + "; skipping");
            } catch (Exception ex) {
                logger.warn("Failed to get person " + precacheObject.id + "; skipping", ex);
            }
        }
        
    }
}
