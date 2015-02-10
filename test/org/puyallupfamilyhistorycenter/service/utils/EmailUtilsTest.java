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

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import org.puyallupfamilyhistorycenter.service.models.Person;
import org.puyallupfamilyhistorycenter.service.models.PersonBuilder;

/**
 *
 * @author tibbitts
 */
public class EmailUtilsTest {

    @Test
    public void testBuildFinalEmailBodyNoProspects() {
        String expected = "<p>Dear Graham Tibbitts,</p><p>Thank you for visiting the Puyallup Stake Family History Center Discovery Room. We hope that you have been inspired to learn more about your family and participate in family history work.</p><p>There are several classes and workshops available at the Center to teach to how to work in the different aspects of family history, like research or indexing. Check out the class schedule and sign up for any that interest you.</p><p>Sincerely,</p><p>The staff at the Puyallup Stake Family History Center</p>";
        
        String actual = EmailUtils.buildFinalEmailBody("Graham Tibbitts", null);
        
        System.out.println(actual);
        
        assertEquals(expected, actual);
    }
    
    @Test
    public void testBuildFinalEmailBodyEmptyProspects() {
        String expected = "<p>Dear Graham Tibbitts,</p><p>Thank you for visiting the Puyallup Stake Family History Center Discovery Room. We hope that you have been inspired to learn more about your family and participate in family history work.</p><p>There are several classes and workshops available at the Center to teach to how to work in the different aspects of family history, like research or indexing. Check out the class schedule and sign up for any that interest you.</p><p>Sincerely,</p><p>The staff at the Puyallup Stake Family History Center</p>";
        
        String actual = EmailUtils.buildFinalEmailBody("Graham Tibbitts", new ArrayList<Person>());
        
        System.out.println(actual);
        
        assertEquals(expected, actual);
    }
    
    @Test
    public void testBuildFinalEmailBodyWithProspects() {
        String expected = "<p>Dear Graham Tibbitts,</p><p>Thank you for visiting the Puyallup Stake Family History Center Discovery Room. We hope that you have been inspired to learn more about your family and participate in family history work.</p><p>There are several classes and workshops available at the Center to teach to how to work in the different aspects of family history, like research or indexing. Check out the class schedule and sign up for any that interest you.</p><p>While scanning through your family tree we noticed the following members of your family whose temple work does not appear to be finished. Please consider helping fix that.</p><ul><li><a href='https://familysearch.org/FDU-2NQQ'>Theodore Tarkin</a></li></ul><p>Sincerely,</p><p>The staff at the Puyallup Stake Family History Center</p>";
        
        String actual = EmailUtils.buildFinalEmailBody("Graham Tibbitts", Arrays.asList(new PersonBuilder().withName("Theodore Tarkin").withGender("MALE").withId("FDU-2NQQ").build()));
        
        System.out.println(actual);
        
        assertEquals(expected, actual);
    }
    
}
