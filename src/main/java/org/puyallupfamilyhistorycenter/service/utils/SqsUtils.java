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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * 
 * @author tibbitts
 */
public class SqsUtils {

    private static AmazonSQS client;
    private static final Gson GSON = new Gson();
    private static ExecutorService executor;
    private static final Set<String> CANCELLED_QUEUES = new HashSet<>();

    private synchronized static AmazonSQS getClient() {
        if (client != null) {
            return client;
        }

        return AmazonSQSClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(Regions.US_WEST_2)
                .build();
    }
    
    private synchronized static ExecutorService getExecutorService() {
        if (executor != null) {
            return executor;
        }
        
        return Executors.newCachedThreadPool();
    }
    
    @VisibleForTesting
    static void setClient(AmazonSQS testClient) {
        client = testClient;
    }
    
    /**
     * 
     * @param <T> type of the message being sent
     * @param message
     * @param queue 
     */
    public static <T> void send(String queue, T message) {
        String jsonMessage = GSON.toJson(message);
        getClient().sendMessage(queue, jsonMessage);
    }
    
    public static <T> void listen(String queue, MessageHandler<T> handler, Class<T> clazz) {
        CANCELLED_QUEUES.remove(queue);
        
        ReceiveMessageRequest request = new ReceiveMessageRequest(queue)
                .withMaxNumberOfMessages(10)
                .withWaitTimeSeconds(20);
        
        getExecutorService().submit(() -> {
            try {
                getClient().createQueue(queue);

                while (!CANCELLED_QUEUES.remove(queue)) {
                    ReceiveMessageResult result = getClient().receiveMessage(request);
                    for (Message message : result.getMessages()) {
                        handler.handle(GSON.fromJson(message.getBody(), clazz));
                        getClient().deleteMessage(queue, message.getReceiptHandle());
                    }
                }

                getClient().deleteQueue(queue);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
    
    public static void stopListener(String queue) {
        CANCELLED_QUEUES.add(queue);
    }
    
    public static void stopAll() {
        getExecutorService().shutdownNow();
    }
    
    public static interface MessageHandler<T> {
        void handle(T message);
    }
}
