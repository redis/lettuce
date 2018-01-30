/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.cluster.topology;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
class RefreshFutures {

    /**
     * Await for either future completion or to reach the timeout. Successful/exceptional future completion is not substantial.
     *
     * @param timeout the timeout value.
     * @param timeUnit timeout unit.
     * @param futures {@link Collection} of {@literal Future}s.
     * @return time awaited in {@link TimeUnit#NANOSECONDS}.
     * @throws InterruptedException
     */
    static long awaitAll(long timeout, TimeUnit timeUnit, Collection<? extends Future<?>> futures) throws InterruptedException {

        long waitTime = 0;

        for (Future<?> future : futures) {

            long timeoutLeft = timeUnit.toNanos(timeout) - waitTime;

            if (timeoutLeft <= 0) {
                break;
            }

            long startWait = System.nanoTime();

            try {
                future.get(timeoutLeft, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                continue;
            } finally {
                waitTime += System.nanoTime() - startWait;
            }

        }
        return waitTime;
    }
}
