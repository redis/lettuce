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

import java.util.*;

import io.lettuce.core.Range;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Parser for redis <a href="http://redis.io/commands/xpending">XPENDING</a> command output.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class PendingParser {

    /**
     * Utility constructor.
     */
    private PendingParser() {
    }

    /**
     * Parse the output of the Redis {@literal XPENDING} command with {@link Range}.
     *
     * @param xpendingOutput output of the Redis {@literal XPENDING}.
     * @return list of {@link PendingMessage}s.
     */
    @SuppressWarnings("unchecked")
    public static List<PendingMessage> parseRange(List<?> xpendingOutput) {

        LettuceAssert.notNull(xpendingOutput, "XPENDING output must not be null");

        List<PendingMessage> result = new ArrayList<>();

        for (Object element : xpendingOutput) {

            LettuceAssert.isTrue(element instanceof List, "Output elements must be a List");

            List<Object> message = (List) element;

            String messageId = (String) message.get(0);
            String consumer = (String) message.get(1);
            Long msSinceLastDelivery = (Long) message.get(2);
            Long deliveryCount = (Long) message.get(3);

            result.add(new PendingMessage(messageId, consumer, msSinceLastDelivery, deliveryCount));
        }

        return result;
    }

    /**
     * Parse the output of the Redis {@literal XPENDING} reporting a summary on pending messages.
     *
     * @param xpendingOutput output of the Redis {@literal XPENDING}.
     * @return {@link PendingMessages}.
     */
    @SuppressWarnings("unchecked")
    public static PendingMessages parse(List<?> xpendingOutput) {

        LettuceAssert.notNull(xpendingOutput, "XPENDING output must not be null");
        LettuceAssert.isTrue(xpendingOutput.size() == 4, "XPENDING output must have exactly four output elements");

        Long count = (Long) xpendingOutput.get(0);
        String from = (String) xpendingOutput.get(1);
        String to = (String) xpendingOutput.get(2);

        Range<String> messageIdRange = Range.create(from, to);

        Collection<Object> consumerMessageCounts = (Collection) xpendingOutput.get(3);

        Map<String, Long> counts = new LinkedHashMap<>();

        for (Object element : consumerMessageCounts) {

            LettuceAssert.isTrue(element instanceof List, "Consumer message counts must be a List");
            List<Object> messageCount = (List) element;

            counts.put((String) messageCount.get(0), (Long) messageCount.get(1));
        }

        return new PendingMessages(count, messageIdRange, Collections.unmodifiableMap(counts));
    }

}
