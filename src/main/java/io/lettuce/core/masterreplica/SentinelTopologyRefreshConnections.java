/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterreplica;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.RedisException;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * @author Mark Paluch
 */
class SentinelTopologyRefreshConnections extends
        CompletableEventLatchSupport<StatefulRedisPubSubConnection<String, String>, SentinelTopologyRefreshConnections> {

    private final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

    private final AtomicInteger success = new AtomicInteger();

    /**
     * Construct a new {@link CompletableEventLatchSupport} class expecting {@code expectedCount} notifications.
     *
     * @param expectedCount
     */
    public SentinelTopologyRefreshConnections(int expectedCount) {
        super(expectedCount);
    }

    @Override
    protected void onAccept(StatefulRedisPubSubConnection<String, String> value) {
        success.incrementAndGet();
    }

    @Override
    protected void onError(Throwable value) {
        exceptions.add(value);
    }

    @Override
    protected void onEmit(Emission<SentinelTopologyRefreshConnections> emission) {

        if (success.get() == 0) {

            RedisException exception = new RedisException("Cannot attach to Redis Sentinel for topology refresh");
            exceptions.forEach(exception::addSuppressed);
            emission.error(exception);
        } else {
            emission.success(this);
        }
    }

}
