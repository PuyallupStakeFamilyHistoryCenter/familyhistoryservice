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

import java.util.Date;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonTemple;

/**
 *
 * @author tibbitts
 */
public class EmailUtils {
    private static final Properties props = new Properties();
    private static final Session session;
    static {
//        props.put("mail.smtp.host", "smtp.zoho.com");
        props.put("mail.smtp.host", "localhost");
        props.put("mail.from", "admin@puyallupfamilyhistorycenter.org");
        Authenticator auth = new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("admin@puyallupfamilyhistorycenter.org", "Brigham47");
            }
        };
        session = Session.getInstance(props);
    }

    
    public static void sendFinalEmail(String personName, String emailAddress, Iterable<PersonTemple> prospects) {
        
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom();
            msg.setRecipients(Message.RecipientType.TO,
                              emailAddress);
            msg.setSubject("Thanks for visiting!");
            msg.setSentDate(new Date());
            msg.setContent(buildFinalEmailBody(personName, prospects), "text/html");
            Transport.send(msg);
        } catch (MessagingException mex) {
            System.out.println("send failed, exception: " + mex);
        }
    }
    
    protected static String buildFinalEmailBody(String personName, Iterable<PersonTemple> prospects) {
        StringBuilder builder = new StringBuilder();
        //TODO: Make this configurable
        builder.append("<p>Dear ")
                .append(personName)
                .append(",</p>"
                + "<p>Thank you for visiting the Puyallup Stake Family History Center Discovery Room. "
                + "We hope that you have been inspired to learn more about your family and participate "
                + "in family history work.</p>"
                + "<p>There are several classes and workshops available at the Center to teach to how to "
                + "work in the different aspects of family history, like research or indexing. Check out "
                + "the class schedule and sign up for any that interest you.</p>");
        
        if (prospects != null) {
            boolean firstProspect = true;
            
            for (PersonTemple prospect : prospects) {
                if (firstProspect) {
                    builder.append("<p>While scanning through your family tree we noticed the following members "
                            + "of your family whose temple work does not appear to be finished. Please consider "
                            + "helping fix that.</p><ul>");
                    firstProspect = false;
                }
                builder.append("<li><a href='https://familysearch.org/tree/#view=ancestor&person=")
                        .append(prospect.id)
                        .append("'>")
                        .append(prospect.name)
                        .append("</a></li>");
            }
            
            if (!firstProspect) {
                builder.append("</ul>");
            }
        }
        
        builder.append("<p>Sincerely,</p><p>The staff at the Puyallup Stake Family History Center</p>");
        
        return builder.toString();
    }
}
