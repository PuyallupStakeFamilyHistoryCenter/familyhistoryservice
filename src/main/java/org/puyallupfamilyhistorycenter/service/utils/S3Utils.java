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
package org.puyallupfamilyhistorycenter.service.utils;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.net.URL;
import org.joda.time.DateTime;
import org.puyallupfamilyhistorycenter.service.ApplicationProperties;

/**
 *
 * @author tibbitts
 */
public class S3Utils {
    private static AmazonS3 client;
    
    private synchronized static AmazonS3 getClient() {
        if (client != null) {
            return client;
        }
        
        AWSCredentials creds = new BasicAWSCredentials(ApplicationProperties.getAWSAccessKey(), ApplicationProperties.getAWSSecretKey());
        return new AmazonS3Client(creds);
    }
    
    public static URL getSignedPutUrl(String bucket, String key, String contentType) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        request.setMethod(HttpMethod.PUT);
        request.setExpiration(new DateTime().plusHours(12).toDate());
        request.setContentType(contentType);
        return getClient().generatePresignedUrl(request);
    }
    
    public static URL getSignedPostUrl(String bucket, String key, String contentType) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        request.setMethod(HttpMethod.POST);
        request.setExpiration(new DateTime().plusHours(12).toDate());
        request.setContentType(contentType);
        return getClient().generatePresignedUrl(request);
    }

    public static URL getSignedGetUrl(String bucket, String key) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        request.setMethod(HttpMethod.GET);
        request.setExpiration(new DateTime().plusDays(30).toDate());
        return getClient().generatePresignedUrl(request);
    }
}
