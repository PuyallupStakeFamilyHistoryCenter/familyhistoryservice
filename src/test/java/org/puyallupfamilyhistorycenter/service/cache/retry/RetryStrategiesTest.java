/*
 * Copyright (c) 2016, tibbitts
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
package org.puyallupfamilyhistorycenter.service.cache.retry;

import com.google.common.collect.ImmutableSet;
import static org.hamcrest.core.Is.is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tibbitts
 */
public class RetryStrategiesTest {

    /**
     * Test of exponential method, of class RetryStrategies.
     */
    @Test
    public void testExponential() {
        RetryingSource.RetryStrategy s = RetryStrategies.exponential(3, 3l, 2.0, ImmutableSet.of(NullPointerException.class));
        
        assertThat(s.getMillisToWait(new NullPointerException(), -1), IsNull.nullValue(Long.class));
        assertThat(s.getMillisToWait(new NullPointerException(), 0), is(3l));
        assertThat(s.getMillisToWait(new NullPointerException(), 1), is(6l));
        assertThat(s.getMillisToWait(new NullPointerException(), 2), is(12l));
        assertThat(s.getMillisToWait(new NullPointerException(), 3), IsNull.nullValue(Long.class));
        assertThat(s.getMillisToWait(new RuntimeException(), 0), IsNull.nullValue(Long.class));
    }
    
}
