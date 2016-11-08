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

import static com.google.common.base.Throwables.propagate;
import static java.lang.Thread.sleep;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.puyallupfamilyhistorycenter.service.cache.Source;

/**
 *
 * @author tibbitts
 */
public class RetryingSource<T> implements Source<T> {
    
    private static final Logger logger = Logger.getLogger(RetryingSource.class);
    
    private final Source<T> delegate;
    private final RetryStrategy strategy;

    public RetryingSource(Source<T> delegate, RetryStrategy strategy) {
        this.delegate = delegate;
        this.strategy = strategy;
    }

    @Override
    public boolean has(String id) {
        try {
            return doRetry(id, delegate::has);
        } catch (Exception ex) {
            throw propagate(ex);
        }
    }

    @Override
    public T get(String id, String accessToken) {
        try {
            return doRetry(ImmutablePair.of(id, accessToken), p -> delegate.get(p.getLeft(), p.getRight()));
        } catch (Exception e) {
            throw propagate(e);
        }
    }
    
    private <I, O> O doRetry(I in, Function<I, O> f) throws Exception {
        int currentTry = 0;
        Exception exception;
        Long msToWait;
        do {
            try {
                return f.apply(in);
            } catch (Exception t) {
                exception = t;
                msToWait = strategy.getMillisToWait(t, currentTry++);
            }
            if (msToWait != null) {
                logger.warn("Failed to execute operation on input " + in, exception);
                sleep(msToWait);
            }
        } while (msToWait != null);
        throw exception;
    }
 
    public static interface RetryStrategy {
        /**
         * 
         * 
         * @param exceptionClass class of exception caught on current try
         * @param currentTryNumber zero-based index of current try
         * @return the number of milliseconds to wait until the next retry attempt
         *   or {@code null} if no retry should be attempted
         */
        Long getMillisToWait(Throwable exception, int currentTryNumber);
    }
}
