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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.atlas.lib.ActionKeyValue;
import org.apache.jena.atlas.lib.Cache;

/**
 *
 * @author tibbitts
 */


public class InMemoryCache<K, V> implements Cache<K, V> {
    Map<K, SoftReference<V>> delegate = new HashMap<>();
    
    @Override
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    @Override
    public V get(K key) {
        SoftReference<V> ref = delegate.get(key);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    @Override
    public V put(K key, V thing) {
        SoftReference<V> ref = delegate.put(key, new SoftReference(thing));
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    @Override
    public boolean remove(K key) {
        if (delegate.containsKey(key)) {
            delegate.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public Iterator<K> keys() {
        return delegate.keySet().iterator();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public long size() {
        return delegate.size();
    }

    @Override
    public void setDropHandler(ActionKeyValue<K, V> dropHandler) {
        throw new UnsupportedOperationException();
    }
    
}
