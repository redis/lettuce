/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.pubsub;

/**
 * Represents a Pub/Sub notification message.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface PubSubMessage<K, V> {

    /**
     * @return the {@link PubSubOutput.Type} message type.
     */
    PubSubOutput.Type type();

    /**
     * @return name of the channel to which this notification belongs to.
     */
    K channel();

    /**
     * @return pattern that applies if the message was received through a pattern subscription. Can be {@code null}.
     */
    K pattern();

    /**
     * @return the subscriber count if applicable.
     */
    long count();

    /**
     * @return the message body, if applicable. Can be {@code null}.
     */
    V body();

}
