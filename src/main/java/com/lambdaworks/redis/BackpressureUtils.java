/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis;

import java.util.concurrent.atomic.AtomicLong;

enum BackpressureUtils {
    ;

    /**
     * Cap a substraction to 0
     *
     * @param a left operand
     * @param b right operand
     * @return Subscription result or 0 if overflow
     */
    public static long subOrZero(long a, long b) {
        long res = a - b;
        if (res < 0L) {
            return 0;
        }
        return res;
    }

    /**
     * Concurrent substraction bound to 0 and Long.MAX_VALUE. Any concurrent write will "happen" before this operation.
     *
     * @param sequence current atomic to update
     * @param toSub delta to sub
     * @return value before subscription, 0 or Long.MAX_VALUE
     */
    public static long getAndSub(AtomicLong sequence, long toSub) {
        long r, u;
        do {
            r = sequence.get();
            if (r == 0 || r == Long.MAX_VALUE) {
                return r;
            }
            u = subOrZero(r, toSub);
        } while (!sequence.compareAndSet(r, u));

        return r;
    }
}
