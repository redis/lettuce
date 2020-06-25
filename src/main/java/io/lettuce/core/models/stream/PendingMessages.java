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

import java.util.Map;

import io.lettuce.core.Range;

/**
 * Value object representing the output of the Redis {@literal XPENDING} reporting a summary on pending messages.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class PendingMessages {

    private final long count;

    private final Range<String> messageIds;

    private final Map<String, Long> consumerMessageCount;

    public PendingMessages(long count, Range<String> messageIds, Map<String, Long> consumerMessageCount) {

        this.count = count;
        this.messageIds = messageIds;
        this.consumerMessageCount = consumerMessageCount;
    }

    public long getCount() {
        return count;
    }

    public Range<String> getMessageIds() {
        return messageIds;
    }

    public Map<String, Long> getConsumerMessageCount() {
        return consumerMessageCount;
    }

}
