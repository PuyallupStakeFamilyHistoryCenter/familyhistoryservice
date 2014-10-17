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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.familysearch.api.client.ft.FamilySearchFamilyTree;
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
    
    private final String accessToken;
    private Future future;
    
    public Precacher(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public void precache() {
        future = executor.submit(new Runnable() {
            FamilySearchFamilyTree tree = FamilyHistoryFamilyTree.getInstance(accessToken);
            Queue<String> frontier = new LinkedList<>();
            Queue<String> leafNodes = new LinkedList<>();
            
            {
                frontier.add(tree.readPersonForCurrentUser().getPerson().getId());
            }
            
            @Override
            public void run() {
                try {
                    while (!frontier.isEmpty()) {
                        String id = frontier.remove();
                        Person person = source.get(id, accessToken);
                        if (person.parents != null) {
                            for (PersonReference parent : person.parents) {
                                System.out.println("Adding parent " + parent.getName() + " to frontier");
                                frontier.add(parent.getId());
                            }
                        } else {
                            leafNodes.add(id);
                        }
                    }

                    while(!leafNodes.isEmpty()) {
                        String id = leafNodes.remove();
                        Person person = source.get(id, accessToken);
                        if (person.children != null) {
                            for (PersonReference child : person.children) {
                                System.out.println("Adding child " + child.getName() + " to frontier");
                                frontier.add(child.getId());
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            }
        });
    }
    
    public void cancel() {
        future.cancel(true);
        System.out.println("Cancelling precaching");
    }
}
