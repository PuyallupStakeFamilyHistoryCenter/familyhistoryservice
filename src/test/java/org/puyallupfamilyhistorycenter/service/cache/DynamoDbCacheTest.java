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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;
import static org.puyallupfamilyhistorycenter.service.cache.DynamoDbCache.BODY_KEY;

/**
 *
 * @author tibbitts
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamoDbCacheTest {
    
    @Mock
    AmazonDynamoDB ddb;
    
    DynamoDbCache<String> cache;
    
    @Before
    public void setup() {
        cache = new DynamoDbCache<>(String.class, 300, "test-table", "primary-key", ddb);
    }

    @Test
    public void testGetWhenNotPresent() {
        when((ddb.getItem(any()))).thenReturn(new GetItemResult());

        String actual = cache.get("test-key");

        assertThat(actual, nullValue(String.class));
    }

    @Test
    public void testGetWhenPresent() {
        when((ddb.getItem(any()))).thenReturn(new GetItemResult()
                .withItem(ImmutableMap.of(BODY_KEY, new AttributeValue("\"hi!\""))));

        String actual = cache.get("test-key");

        assertThat(actual, is("hi!"));
    }

    @Test
    public void testContainsKeyWhenNotPresent() {
        when((ddb.getItem(any()))).thenReturn(new GetItemResult());

        boolean actual = cache.containsKey("test-key");

        assertThat(actual, is(false));
    }

    @Test
    public void testContainsKeyWhenPresent() {
        when((ddb.getItem(any()))).thenReturn(new GetItemResult()
                .withItem(ImmutableMap.of(BODY_KEY, new AttributeValue("\"hi!\""))));

        boolean actual = cache.containsKey("test-key");

        assertThat(actual, is(true));
    }
    
    @Test
    public void testPut() {
        cache.put("test-key", "hi!");
        
        verify(ddb).putItem(eq("test-table"), any(Map.class));
    }
    
}
