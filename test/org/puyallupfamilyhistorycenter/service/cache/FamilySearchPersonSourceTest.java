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

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author tibbitts
 */
@RunWith(Parameterized.class)
public class FamilySearchPersonSourceTest {
    
    @Parameterized.Parameters
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][]
            {
                {"+2010-10-09","9 October 2010"},
                {"+1899-01-04","4 January 1899"},
                {"+1704-03-21","21 March 1704"},
                {"+431-12-01","1 December 431"},
                {"+1980", "1980"},
                {"+1980-10", "October 1980"},
                {"A+2010-10-09","around 9 October 2010"},
                {"A+1899-01-04","around 4 January 1899"},
                {"A+1704-03-21","around 21 March 1704"},
                {"A+431-12-01","around 1 December 431"},
                {"A+1980", "around 1980"},
                {"A+1980-10", "around October 1980"},
                {"/+2010-10-09","after 9 October 2010"},
                {"/+1899-01-04","after 4 January 1899"},
                {"/+1704-03-21","after 21 March 1704"},
                {"/+431-12-01","after 1 December 431"},
                {"/+1980", "after 1980"},
                {"/+1980-10", "after October 1980"},
                {"+2010-10-09/","before 9 October 2010"},
                {"+1899-01-04/","before 4 January 1899"},
                {"+1704-03-21/","before 21 March 1704"},
                {"+431-12-01/","before 1 December 431"},
                {"+1980/", "before 1980"},
                {"+1980-10/", "before October 1980"},
                {null,null},
            }
        );
    }
    
    private final String input;
    private final String expected;
    public FamilySearchPersonSourceTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    /**
     * Test of get method, of class FamilySearchPersonSource.
     */
    @Test
    public void testFormatDate() {
        assertEquals(expected, FamilySearchPersonSource.formatDate(input));
    }
    
}
