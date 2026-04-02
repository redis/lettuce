package io.lettuce.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.ReactiveTransactionBuilder;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.json.JsonParser;

/**
 * A reactive and thread-safe API for a Redis connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V> {

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on.
     * @param codec the codec for command encoding.
     * @param parser the implementation of the {@link JsonParser} to use
     */
    public RedisReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
    }

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on.
     * @param codec the codec for command encoding.
     */
    public RedisReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection<K, V>) super.getConnection();
    }

    @Override
    public ReactiveTransactionBuilder<K, V> transaction() {
        return new TransactionBuilderImpl<>(getStatefulConnection(), getCodec());
    }

    @Override
    @SafeVarargs
    public final ReactiveTransactionBuilder<K, V> transaction(K... watchKeys) {
        return new TransactionBuilderImpl<>(getStatefulConnection(), getCodec(), watchKeys);
    }

    @Override
    public Mono<TransactionResult> transactional(Consumer<RedisAsyncCommands<K, V>> transactionBody) {
        return transactional(transactionBody, (K[]) null);
    }

    @Override
    @SafeVarargs
    public final Mono<TransactionResult> transactional(Consumer<RedisAsyncCommands<K, V>> transactionBody, K... watchKeys) {
        return Mono.defer(() -> {
            ReactiveTransactionBuilder<K, V> builder = (watchKeys != null && watchKeys.length > 0) ? transaction(watchKeys)
                    : transaction();
            transactionBody.accept(builder.commands());
            return builder.executeReactive();
        });
    }

}
