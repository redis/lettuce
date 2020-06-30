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
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
class ReplayOutputUnitTests {

    @Test
    void shouldReplaySimpleCompletion() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ValueOutput<String, String> target = new ValueOutput<>(StringCodec.ASCII);

        replay.multi(1);
        replay.set(ByteBuffer.wrap("foo".getBytes()));
        replay.complete(1);

        replay.replay(target);

        assertThat(target.get()).isEqualTo("foo");
    }

    @Test
    void shouldReplayNestedCompletion() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ArrayOutput<String, String> target = new ArrayOutput<>(StringCodec.ASCII);

        replay.multi(1);
        replay.multi(1);
        replay.set(ByteBuffer.wrap("foo".getBytes()));
        replay.complete(2);

        replay.multi(1);
        replay.set(ByteBuffer.wrap("bar".getBytes()));
        replay.complete(2);
        replay.complete(1);

        replay.replay(target);

        assertThat(target.get().get(0)).isEqualTo(Arrays.asList("foo", Collections.singletonList("bar")));
    }

    @Test
    void shouldDecodeErrorResponse() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ValueOutput<String, String> target = new ValueOutput<>(StringCodec.ASCII);

        replay.setError(ByteBuffer.wrap("foo".getBytes()));

        replay.replay(target);

        assertThat(replay.getError()).isEqualTo("foo");
        assertThat(target.getError()).isEqualTo("foo");
    }
}
