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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import org.apache.jena.atlas.lib.ActionKeyValue;
import org.apache.jena.atlas.lib.Cache;

/**
 *
 * @author tibbitts
 */

/**
 *
 * @author tibbitts
 * @param <V> the type of mapped values
 */
public class FileCache<K, V> implements Cache<K, V> {
    public static final Gson GSON = new Gson();

    private final Class<V> clazz;
    private final File dir;
    private final long ttl;
    public FileCache(Class<V> clazz, File dir, long ttl) {
        this.clazz = clazz;
        this.dir = dir;
        dir.mkdirs();
        this.ttl = ttl;
    }
    

    @Override
    public long size() {
        return dir.list().length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(K key) {
        File file = new File(dir, key.toString());
        return file.exists() && 
                file.lastModified() + ttl > System.currentTimeMillis();
    }

    @Override
    public V get(K key) {
        File file = new File(dir, key.toString());
        if (!file.exists()) {
            return null;
        }
        
        try (Reader reader = new FileReader(file)) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read from cache file " + file, ex);
        }
    }

    @Override
    public V put(K key, V value) {
        V previousValue = get(key);
        
        File file = new File(dir, key.toString());
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(value, writer);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write to cache file " + file, ex);
        }
        
        return previousValue;
    }

    @Override
    public boolean remove(K key) {
        File file = new File(dir, key.toString());
        if (file.exists()) {
            file.delete();
            return true;
        }
        
        return false;
    }

    @Override
    public void clear() {
        for (File child : dir.listFiles()) {
            child.delete();
        }
    }

    @Override
    public Iterator<K> keys() {
//        final File[] files = dir.listFiles();
//        return new Iterator<K>() {
//            int currentIndex = 0;
//            
//            @Override
//            public boolean hasNext() {
//                return currentIndex < files.length;
//            }
//
//            @Override
//            public K next() {
//                return files[currentIndex++].getName();
//            }
//
//            @Override
//            public void remove() {
//                files[currentIndex-1].delete();
//            }
//        };
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDropHandler(ActionKeyValue<K, V> dropHandler) {
        throw new UnsupportedOperationException();
    }
    
}
