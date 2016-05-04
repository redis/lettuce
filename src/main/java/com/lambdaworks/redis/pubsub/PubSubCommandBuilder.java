package com.lambdaworks.redis.pubsub;

import static com.lambdaworks.redis.protocol.CommandKeyword.CHANNELS;
import static com.lambdaworks.redis.protocol.CommandKeyword.NUMSUB;
import static com.lambdaworks.redis.protocol.CommandType.*;

import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.CommandOutput;
import com.lambdaworks.redis.output.IntegerOutput;
import com.lambdaworks.redis.output.KeyListOutput;
import com.lambdaworks.redis.output.MapOutput;
import com.lambdaworks.redis.protocol.BaseRedisCommandBuilder;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * Dedicated pub/sub command builder to build pub/sub commands.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
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
        return createCommand(PUBSUB, (MapOutput) new MapOutput<>((RedisCodec) codec), args);
    }

    @SafeVarargs
    final Command<K, V, K> psubscribe(K... patterns) {
        LettuceAssert.notEmpty(patterns, "patterns " + MUST_NOT_BE_EMPTY);

        return pubSubCommand(PSUBSCRIBE, new PubSubOutput<>(codec), patterns);
    }

    @SafeVarargs
    final Command<K, V, K> punsubscribe(K... patterns) {
        return pubSubCommand(PUNSUBSCRIBE, new PubSubOutput<>(codec), patterns);
    }

    @SafeVarargs
    final Command<K, V, K> subscribe(K... channels) {
        LettuceAssert.notEmpty(channels, "channels " + MUST_NOT_BE_EMPTY);

        return pubSubCommand(SUBSCRIBE, new PubSubOutput<>(codec), channels);
    }

    @SafeVarargs
    final Command<K, V, K> unsubscribe(K... channels) {
        return pubSubCommand(UNSUBSCRIBE, new PubSubOutput<>(codec), channels);
    }

    <T> Command<K, V, T> pubSubCommand(CommandType type, CommandOutput<K, V, T> output, K... keys) {
        return new Command<>(type, output, new PubSubCommandArgs<>(codec).addKeys(keys));
    }
}
