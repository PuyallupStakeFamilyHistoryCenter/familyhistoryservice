/*
 * Copyright (c) 2015, tibbitts
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
package org.puyallupfamilyhistorycenter.service.utils.managedconcurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author tibbitts
 */
public class TagThrottlingExecutorService implements ExecutorService {
    
    private final int partitions;
    private final Map<Integer, ExecutorService> executorServiceMap;
    private final Map<String, Integer> tagPoolMap;
    private final AtomicInteger nextPoolCounter;
    private final ExecutorServiceFactory factory;
    
    private boolean isShutdown = false;
    private boolean isTerminated = false;

    public TagThrottlingExecutorService(int partitions, ExecutorServiceFactory factory) {
        executorServiceMap = new HashMap<>(partitions);
        tagPoolMap = new HashMap<>();
        nextPoolCounter = new AtomicInteger();
        this.partitions = partitions;
        this.factory = factory;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        //TODO: This is currently unsafe because a 
        for (ExecutorService ex : executorServiceMap.values()) {
            ex.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> runnables = new ArrayList<>();
        for (ExecutorService ex : executorServiceMap.values()) {
            runnables.addAll(ex.shutdownNow());
        }
        isTerminated = true;
        return runnables;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isTerminated;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        boolean terminated = true;
        for (ExecutorService ex : executorServiceMap.values()) {
            if (!ex.isTerminated()) {
                terminated &= ex.awaitTermination(timeout, unit);
            }
        }
        isTerminated = terminated;
        return terminated;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return getPool(task).submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return getPool(task).submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return getPool(task).submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getPool(tasks.iterator().next()).invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return getPool(tasks.iterator().next()).invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getPool(tasks.iterator().next()).invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getPool(tasks.iterator().next()).invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        getPool(command).execute(command);
    }
    
    private ExecutorService getPool(Object o) {
        if (o instanceof Integer) {
            int index = Math.abs((int) o) % partitions;
            ExecutorService ex = executorServiceMap.get(index);
            if (ex == null) {
                synchronized (executorServiceMap) {
                    if (!executorServiceMap.containsKey(index)) {
                        //TODO: Handle case where closed or closing
                        executorServiceMap.put(index, factory.create());
                    }
                }
                ex = executorServiceMap.get(index);
            }
            return ex;
        } else if (o instanceof Tagged) {
            String tag = ((Tagged) o).getTag();
            Integer poolId = tagPoolMap.get(tag);
            if (poolId == null) {
                synchronized (tagPoolMap) {
                    if (!tagPoolMap.containsKey(tag)) {
                        poolId = nextPoolCounter.getAndIncrement() % partitions;
                        tagPoolMap.put(tag, poolId);
                    }
                }
                poolId = tagPoolMap.get(tag);
            }
            
            return getPool(poolId);
        } else {
            return getPool(o.hashCode());
        }
    }
    
    public static interface Tagged {
        String getTag();
    }
    
    public static interface TaggedCallable<T> extends Callable<T>, Tagged {
    }
    
    public static interface TaggedRunnable extends Runnable, Tagged {
    }
}
