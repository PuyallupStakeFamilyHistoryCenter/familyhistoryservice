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
package org.puyallupfamilyhistorycenter.service.utils;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.core.Is.is;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author tibbitts
 */
@RunWith(MockitoJUnitRunner.class)
public class SqsUtilsTest {
    
    private static final String QUEUE_NAME = "test-queue";
    
    @Mock
    AmazonSQS client;
    
    @Before
    public void setup() {
        SqsUtils.setClient(client);
    }

    @AfterClass
    public static void shutdown() {
        SqsUtils.stopAll();
    }

    @Test
    public void testSend() {
        SqsUtils.send(QUEUE_NAME, "message");
        
        verify(client).sendMessage(QUEUE_NAME, "\"message\"");
    }
    
    @Test
    public void testListen() throws InterruptedException {
        when(client.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(new ReceiveMessageResult()
                        .withMessages(ImmutableList.of(new Message().withBody("\"message\"").withReceiptHandle("handle"))));
        SqsUtils.listen(QUEUE_NAME, m -> {
                    assertThat(m, is("message"));
                    SqsUtils.stopListener(QUEUE_NAME);
                }, String.class);
        TimeUnit.SECONDS.sleep(1);
        
        verify(client).createQueue(QUEUE_NAME);
        verify(client).receiveMessage(any(ReceiveMessageRequest.class));
        verify(client).deleteMessage(QUEUE_NAME, "handle");
        verify(client).deleteQueue(QUEUE_NAME);
    }
    
}
