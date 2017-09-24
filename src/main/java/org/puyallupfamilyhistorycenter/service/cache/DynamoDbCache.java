/*
 * Copyright (c) 2017, tibbitts
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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.jena.atlas.lib.ActionKeyValue;
import org.apache.jena.atlas.lib.Cache;

/**
 *
 * @author tibbitts
 */
public class DynamoDbCache<V> implements Cache<String, V> {

    public static final String BODY_KEY = "body";
    public static final String TTL_KEY = "ttl";
    public static final Gson GSON = new Gson();

    private final Class<V> clazz;
    private final long ttl;
    private final String tableName;
    private final String primaryKey;
    private final AmazonDynamoDB ddb;

    public DynamoDbCache(Class<V> clazz, long ttl, String tableName, String primaryKey) {
        this(clazz, ttl, tableName, primaryKey, AmazonDynamoDBClientBuilder.defaultClient());
    }
    
    @VisibleForTesting
    DynamoDbCache(Class<V> clazz, long ttl, String tableName, String primaryKey, AmazonDynamoDB ddb) {
        this.clazz = clazz;
        this.ttl = ttl; //TODO: This is ignored
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        this.ddb = ddb;
    }

    @Override
    public boolean containsKey(String key) {
        return get(key) != null;
    }

    @Override
    public V get(String key) {
        GetItemRequest request = new GetItemRequest(tableName, ImmutableMap.of(primaryKey, new AttributeValue(key)))
                .withAttributesToGet(BODY_KEY);
        GetItemResult result = ddb.getItem(request);
        
        if (result.getItem() == null) {
            return null;
        }
        
        return GSON.fromJson(result.getItem().get(BODY_KEY).getS(), clazz);
    }

    @Override
    public V put(String key, V value) {
        ddb.putItem(tableName, ImmutableMap.of(
                primaryKey, new AttributeValue(key),
                BODY_KEY, new AttributeValue(GSON.toJson(value)),
                TTL_KEY, new AttributeValue().withN(
                        Long.toString(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() + ttl)))));
        return value;
    }

    @Override
    public boolean remove(String key) {
        DeleteItemResult result = ddb.deleteItem(tableName, ImmutableMap.of(primaryKey, new AttributeValue(key)));
        
        //TODO
        return false;
    }

    @Override
    public Iterator<String> keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDropHandler(ActionKeyValue<String, V> akv) {
        throw new UnsupportedOperationException();
    }
    
}
