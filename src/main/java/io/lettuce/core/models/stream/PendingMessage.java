/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.core.models.stream;

import java.time.Duration;

/**
 * Value object representing a pending message reported through XPENDING with range/limit.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class PendingMessage {

    private final String id;

    private final String consumer;

    private final long msSinceLastDelivery;

    private final long redeliveryCount;

    public PendingMessage(String id, String consumer, long msSinceLastDelivery, long redeliveryCount) {

        this.id = id;
        this.consumer = consumer;
        this.msSinceLastDelivery = msSinceLastDelivery;
        this.redeliveryCount = redeliveryCount;
    }

    public String getId() {
        return id;
    }

    public String getConsumer() {
        return consumer;
    }

    public long getMsSinceLastDelivery() {
        return msSinceLastDelivery;
    }

    public Duration getSinceLastDelivery() {
        return Duration.ofMillis(getMsSinceLastDelivery());
    }

    public long getRedeliveryCount() {
        return redeliveryCount;
    }

}
