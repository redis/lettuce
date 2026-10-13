/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(UNIT_TEST)
class AsyncCommandQueueLatencyUnitTests {

    @Test
    void recordsFirstSubmissionAcrossDecorators() {
        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.GET, null));
        RedisCommand<String, String, String> wrapped = new CommandWrapper<>(new CommandWrapper<>(async));

        AsyncCommand.markEnqueued(wrapped, 123L);
        AsyncCommand.markEnqueued(wrapped, 456L); // reconnect must not reset the start

        assertThat(AsyncCommand.queuedAt(wrapped)).isEqualTo(123L);
        assertThat(AsyncCommand.queuedAt(async)).isEqualTo(123L);
    }

    @Test
    void recordsDirectCommands() {
        RedisCommand<String, String, String> direct = new Command<>(CommandType.GET, null);
        AsyncCommand.markEnqueued(direct, 123L);
        assertThat(AsyncCommand.queuedAt(direct)).isEqualTo(123L);
    }

    @Test
    void preservesFirstSendAcrossReplay() {
        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.GET, null));
        AsyncCommand.markEnqueued(async, -100);
        AsyncCommand.markSent(async, -50);
        AsyncCommand.markSent(new CommandWrapper<>(async), 200);

        assertThat(AsyncCommand.queuedAt(async)).isEqualTo(-100);
        assertThat(AsyncCommand.firstSent(async)).isEqualTo(-50);
    }

    @Test
    void readsThroughNewDecoratorsOnReplay() {
        Command<String, String, String> command = new Command<>(CommandType.GET, null);
        AsyncCommand.markEnqueued(new CommandWrapper<>(command), 123);
        AsyncCommand.markSent(command, 150);
        RedisCommand<String, String, String> replayed = new CommandWrapper<>(new CommandWrapper<>(command));
        AsyncCommand.markEnqueued(replayed, 200);
        AsyncCommand.markSent(replayed, 250);

        assertThat(AsyncCommand.queuedAt(replayed)).isEqualTo(123);
        assertThat(AsyncCommand.firstSent(replayed)).isEqualTo(150);
    }

}
