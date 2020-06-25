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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.Range;

/**
 * @author Mark Paluch
 */
class PendingParserUnitTests {

    @Test
    void shouldParseXpendingWithRangeOutput() {

        List<PendingMessage> result = PendingParser
                .parseRange(Collections.singletonList(Arrays.asList("foo", "consumer", 1L, 2L)));

        assertThat(result).hasSize(1);

        PendingMessage message = result.get(0);

        assertThat(message.getId()).isEqualTo("foo");
        assertThat(message.getConsumer()).isEqualTo("consumer");
        assertThat(message.getMsSinceLastDelivery()).isEqualTo(1);
        assertThat(message.getSinceLastDelivery()).isEqualTo(Duration.ofMillis(1));
        assertThat(message.getRedeliveryCount()).isEqualTo(2);
    }

    @Test
    void shouldParseXpendingOutput() {

        PendingMessages result = PendingParser
                .parse(Arrays.asList(16L, "from", "to", Collections.singletonList(Arrays.asList("consumer", 17L))));

        assertThat(result.getCount()).isEqualTo(16);
        assertThat(result.getMessageIds()).isEqualTo(Range.create("from", "to"));
        assertThat(result.getConsumerMessageCount()).containsEntry("consumer", 17L);
    }

}
