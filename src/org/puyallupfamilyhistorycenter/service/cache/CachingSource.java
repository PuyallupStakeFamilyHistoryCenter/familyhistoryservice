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

import org.apache.jena.atlas.lib.Cache;

/**
 *
 * @author tibbitts
 */

/**
 *
 * @author tibbitts
 */
public class CachingSource<E> implements Source<E> {
    
    private final Source<E> source;
    private final Cache<String, E> cache;
    private ShouldCacheDecider<E> decider;
    public CachingSource(Source<E> source, Cache<String, E> cache) {
        this.source = source;
        this.cache = cache;
    }

    @Override
    public E get(String id, String accessToken) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        
        E value;
        try {
            value = source.get(id, accessToken);
            if (decider == null || decider.shouldCache(value)) {
                cache.put(id, value);
            }
        } catch (Exception e) {
            value = cache.get(id);
            if (value == null) {
                throw e;
            }
        }
        return value;
    }

    public void setDecider(ShouldCacheDecider<E> decider) {
        this.decider = decider;
    }
    
    public static interface ShouldCacheDecider<E> {
        boolean shouldCache(E value);
    }
}
