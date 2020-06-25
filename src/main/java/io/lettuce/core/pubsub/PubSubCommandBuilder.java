/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.pubsub;

import static io.lettuce.core.protocol.CommandKeyword.CHANNELS;
import static io.lettuce.core.protocol.CommandKeyword.NUMSUB;
import static io.lettuce.core.protocol.CommandType.*;

import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.MapOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

/**
 * Dedicated pub/sub command builder to build pub/sub commands.
 *
 * @author Mark Paluch
 * @since 4.2
 */
@SuppressWarnings("varargs")
class PubSubCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    static final String MUST_NOT_BE_EMPTY = "must not be empty";

    PubSubCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, Long> publish(K channel, V message) {
        CommandArgs<K, V> args = new PubSubCommandArgs<>(codec).addKey(channel).addValue(message);
        return createCommand(PUBLISH, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<K>> pubsubChannels(K pattern) {
        CommandArgs<K, V> args = new PubSubCommandArgs<>(codec).add(CHANNELS).addKey(pattern);
        return createCommand(PUBSUB, new KeyListOutput<>(codec), args);
    }

    @SafeVarargs
    final Command<K, V, Map<K, Long>> pubsubNumsub(K... patterns) {
        LettuceAssert.notEmpty(patterns, "patterns " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new PubSubCommandArgs<>(codec).add(NUMSUB).addKeys(patterns);
        return createCommand(PUBSUB, new MapOutput<>((RedisCodec) codec), args);
    }

    @SafeVarargs
    final Command<K, V, V> psubscribe(K... patterns) {
        LettuceAssert.notEmpty(patterns, "patterns " + MUST_NOT_BE_EMPTY);

        return pubSubCommand(PSUBSCRIBE, new PubSubOutput<>(codec), patterns);
    }

    @SafeVarargs
    final Command<K, V, V> punsubscribe(K... patterns) {
        return pubSubCommand(PUNSUBSCRIBE, new PubSubOutput<>(codec), patterns);
    }

    @SafeVarargs
    final Command<K, V, V> subscribe(K... channels) {
        LettuceAssert.notEmpty(channels, "channels " + MUST_NOT_BE_EMPTY);

        return pubSubCommand(SUBSCRIBE, new PubSubOutput<>(codec), channels);
    }

    @SafeVarargs
    final Command<K, V, V> unsubscribe(K... channels) {
        return pubSubCommand(UNSUBSCRIBE, new PubSubOutput<>(codec), channels);
    }

    <T> Command<K, V, T> pubSubCommand(CommandType type, CommandOutput<K, V, T> output, K... keys) {
        return new Command<>(type, output, new PubSubCommandArgs<>(codec).addKeys(keys));
    }

}
