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

/**
 * Value object representing an entry of the Pending Entry List retrieved via {@literal XPENDING}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class PendingEntry {

    private final String messageId;

    private final String consumer;

    private final long millisSinceDelivery;

    private final long deliveryCount;

    public PendingEntry(String messageId, String consumer, long millisSinceDelivery, long deliveryCount) {

        this.messageId = messageId;
        this.consumer = consumer;
        this.millisSinceDelivery = millisSinceDelivery;
        this.deliveryCount = deliveryCount;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConsumer() {
        return consumer;
    }

    public long getMillisSinceDelivery() {
        return millisSinceDelivery;
    }

    public long getDeliveryCount() {
        return deliveryCount;
    }

    @Override
    public String toString() {

        return String.format("%s [messageId='%s', consumer='%s', millisSinceDelivery=%d, deliveryCount=%d]",
                getClass().getSimpleName(), messageId, consumer, millisSinceDelivery, deliveryCount);
    }

}
