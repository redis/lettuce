/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.commands.reactive;

import static io.lettuce.core.BitFieldArgs.offset;
import static io.lettuce.core.BitFieldArgs.signed;
import static io.lettuce.core.BitFieldArgs.typeWidthBasedOffset;
import static io.lettuce.core.BitFieldArgs.OverflowType.FAIL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.BitCommandTest;
import io.lettuce.core.commands.BitStringCodec;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class BitReactiveCommandTest extends BitCommandTest {

    private RedisStringReactiveCommands<String, String> reactive;

    @Override
    public RedisCommands<String, String> connect() {
        bitstring = ReactiveSyncInvocationHandler.sync(client.connect(new BitStringCodec()));

        StatefulRedisConnection<String, String> connection = client.connect();
        reactive = connection.reactive();
        return ReactiveSyncInvocationHandler.sync(connection);
    }

    @Test
    void bitfield() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).set(5, 1).incrBy(2, 3).get().get(2);

        StepVerifier.create(reactive.bitfield(key, bitFieldArgs))
                .expectNext(Value.just(0L), Value.just(32L), Value.just(3L), Value.just(0L), Value.just(3L)).verifyComplete();

        assertThat(bitstring.get(key)).isEqualTo("0000000000010011");
    }

    @Test
    void bitfieldGetWithOffset() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 1).get(signed(2), typeWidthBasedOffset(1));

        StepVerifier.create(reactive.bitfield(key, bitFieldArgs)).expectNext(Value.just(0L), Value.just(0L)).verifyComplete();

        assertThat(bitstring.get(key)).isEqualTo("10000000");
    }

    @Test
    void bitfieldSet() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 5).set(5);

        StepVerifier.create(reactive.bitfield(key, bitFieldArgs)).expectNext(Value.just(0L), Value.just(5L)).verifyComplete();

        assertThat(bitstring.get(key)).isEqualTo("10100000");
    }

    @Test
    void bitfieldWithOffsetSet() {

        StepVerifier.create(reactive.bitfield(key, BitFieldArgs.Builder.set(signed(8), typeWidthBasedOffset(2), 5)))
                .expectNextCount(1).verifyComplete();

        assertThat(bitstring.get(key)).isEqualTo("000000000000000010100000");

        redis.del(key);
        StepVerifier.create(reactive.bitfield(key, BitFieldArgs.Builder.set(signed(8), offset(2), 5))).expectNextCount(1)
                .verifyComplete();
        assertThat(bitstring.get(key)).isEqualTo("1000000000000010");
    }

    @Test
    void bitfieldIncrBy() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.set(signed(8), 0, 5).incrBy(1);

        StepVerifier.create(reactive.bitfield(key, bitFieldArgs)).expectNext(Value.just(0L), Value.just(6L)).verifyComplete();

        assertThat(bitstring.get(key)).isEqualTo("01100000");
    }

    @Test
    void bitfieldOverflow() {

        BitFieldArgs bitFieldArgs = BitFieldArgs.Builder.overflow(FAIL).set(signed(8), 9, 5)
                .incrBy(signed(8), Integer.MAX_VALUE);

        StepVerifier.create(reactive.bitfield(key, bitFieldArgs)).expectNext(Value.just(0L)).expectNext(Value.empty())
                .verifyComplete();
    }
}
