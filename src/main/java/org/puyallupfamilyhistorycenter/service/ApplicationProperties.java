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
package org.puyallupfamilyhistorycenter.service;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author tibbitts
 */
public class ApplicationProperties {
    private static final String ENABLE_EMAIL = "enableEmail";
    private static final String EMAIL_WHITELIST = "emailWhitelist";
    private static final String EMAIL_SALUTATION = "emailSalutation";
    private static final String EMAIL_BODY = "emailBody";
    private static final String EMAIL_SUBJECT = "emailSubject";
    private static final String EMAIL_PROSPECTS_EXPL = "emailProspectsExpl";
    private static final String EMAIL_SIGNATURE = "emailSignature";
    private static final String GUEST_USER_ID = "guestUserId";
    private static final String AWS_ACCESS_KEY = "awsAccessKey";
    private static final String AWS_SECRET_KEY = "awsSecretKey";
    private static final String EMAIL_AWS_ACCESS_KEY = "emailAwsAccessKey";
    private static final String EMAIL_AWS_SECRET_KEY = "emailAwsSecretKey";
    private static final String VIDEO_S3_BUCKET = "videoS3Bucket";
    private static final String VIDEO_S3_KEY_PREFIX = "videoS3KeyPrefix";
    private static final String INTEREST_PREFIX = "interest_";
    private static final String WARD_CONTACT_PREFIX = "wardContact_";
    private static final String RESTART_SCRIPT = "restart_script";
    private static final String PATH = "path";

    private static final Properties props;
    static {
        try {
            props = new Properties();
            props.load(new FileReader("common.properties"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load properties", ex);
        }
    }
    
    public static boolean enableEmail() {
        return Boolean.parseBoolean(props.getProperty(ENABLE_EMAIL));
    }
    
    public static Set<String> getEmailWhitelist() {
        return ImmutableSet
                .<String>builder()
                .add(props.getProperty(EMAIL_WHITELIST).split(";"))
                .build();
    }
    
    public static String getEmailSalutation() {
        return props.getProperty(EMAIL_SALUTATION);
    }
    
    public static String getEmailBody() {
        return props.getProperty(EMAIL_BODY);
    }
    
    public static String getEmailSubject() {
        return props.getProperty(EMAIL_SUBJECT, "Thanks for visiting!");
    }
    
    public static String getEmailProspectsExplanation() {
        return props.getProperty(EMAIL_PROSPECTS_EXPL);
    }
    
    public static String getEmailSignature() {
        return props.getProperty(EMAIL_SIGNATURE);
    }
    
    public static String getEmailAWSAccessKey() {
        return props.getProperty(EMAIL_AWS_ACCESS_KEY);
    }
    
    public static String getEmailAWSSecretKey() {
        return props.getProperty(EMAIL_AWS_SECRET_KEY);
    }

    public static String getGuestPersonId() {
        return props.getProperty(GUEST_USER_ID);
    }
    
    public static String getAWSAccessKey() {
        return props.getProperty(AWS_ACCESS_KEY);
    }
    
    public static String getAWSSecretKey() {
        return props.getProperty(AWS_SECRET_KEY);
    }
    
    public static String getVideoS3Bucket() {
        return props.getProperty(VIDEO_S3_BUCKET);
    }
    
    public static String getVideoS3KeyPrefix() {
        return props.getProperty(VIDEO_S3_KEY_PREFIX);
    }
    
    public static Contact getWardContact(String stakeName, String wardName) {
        return new Gson().fromJson(props.getProperty(WARD_CONTACT_PREFIX + stakeName + "_" + wardName), Contact.class);
    }
    
    public static String getInterest(String interestId) {
        return props.getProperty(INTEREST_PREFIX + interestId);
    }

    public static List<String> getInterests(String[] interestIds) {
        List<String> interests = new ArrayList<>(interestIds.length);
        for (String id : interestIds) {
            interests.add(getInterest(id));
        }
        return interests;
    }
    
    public static String getRestartScript() {
        return props.getProperty(RESTART_SCRIPT);
    }
    
    public static String getPath() {
        return props.getProperty(PATH);
    }
}
