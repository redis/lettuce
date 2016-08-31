package com.lambdaworks.redis.pubsub;

import java.util.Map;

import com.lambdaworks.redis.RedisReactiveCommandsImpl;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.pubsub.api.reactive.ChannelMessage;
import com.lambdaworks.redis.pubsub.api.reactive.PatternMessage;
import com.lambdaworks.redis.pubsub.api.reactive.RedisPubSubReactiveCommands;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * A reactive and thread-safe API for a Redis pub/sub connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisPubSubReactiveCommandsImpl<K, V> extends RedisReactiveCommandsImpl<K, V>
        implements RedisPubSubReactiveCommands<K, V> {

    private PubSubCommandBuilder<K, V> commandBuilder;

    /**
     * Initialize a new connection.
     *
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubReactiveCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.connection = connection;
        this.commandBuilder = new PubSubCommandBuilder<>(codec);
    }

    /**
     * Add a new listener.
     * 
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        getStatefulConnection().addListener(listener);
    }

    @Override
    public Flux<PatternMessage<K, V>> observePatterns() {
        return observePatterns(FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public Flux<PatternMessage<K, V>> observePatterns(FluxSink.OverflowStrategy overflowStrategy) {
        return Flux.create(sink -> {

            RedisPubSubAdapter<K, V> listener = new RedisPubSubAdapter<K, V>() {

                @Override
                public void message(K pattern, K channel, V message) {
                    sink.next(new PatternMessage<>(pattern, channel, message));
                }
            };

            addListener(listener);
            sink.setCancellation(() -> {
                removeListener(listener);
            });

        }, overflowStrategy);
    }

    @Override
    public Flux<ChannelMessage<K, V>> observeChannels() {
        return observeChannels(FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public Flux<ChannelMessage<K, V>> observeChannels(FluxSink.OverflowStrategy overflowStrategy) {

        return Flux.create(sink -> {

            RedisPubSubAdapter<K, V> listener = new RedisPubSubAdapter<K, V>() {

                @Override
                public void message(K channel, V message) {
                    sink.next(new ChannelMessage<>(channel, message));
                }
            };

            addListener(listener);

            sink.setCancellation(() -> {
                removeListener(listener);
            });

        }, overflowStrategy);
    }

    /**
     * Remove an existing listener.
     * 
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        getStatefulConnection().removeListener(listener);
    }

    @Override
    public Mono<Void> psubscribe(K... patterns) {
        return createMono(() -> commandBuilder.psubscribe(patterns)).then();
    }

    @Override
    public Mono<Void> punsubscribe(K... patterns) {
        return createFlux(() -> commandBuilder.punsubscribe(patterns)).then();
    }

    @Override
    public Mono<Void> subscribe(K... channels) {
        return createFlux(() -> commandBuilder.subscribe(channels)).then();
    }

    @Override
    public Mono<Void> unsubscribe(K... channels) {
        return createFlux(() -> commandBuilder.unsubscribe(channels)).then();
    }

    @Override
    public Mono<Long> publish(K channel, V message) {
        return createMono(() -> commandBuilder.publish(channel, message));
    }

    @Override
    public Flux<K> pubsubChannels(K channel) {
        return createDissolvingFlux(() -> commandBuilder.pubsubChannels(channel));
    }

    @Override
    public Mono<Map<K, Long>> pubsubNumsub(K... channels) {
        return createMono(() -> commandBuilder.pubsubNumsub(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }

}
