/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.sentinel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import com.lambdaworks.redis.AbstractRedisReactiveCommands;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.reactive.RedisSentinelReactiveCommands;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisSentinelReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V>
        implements RedisSentinelReactiveCommands<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;
    private final StatefulConnection<K, V> connection;

    public RedisSentinelReactiveCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.connection = connection;
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);
    }

    @Override
    public Mono<SocketAddress> getMasterAddrByName(K key) {

        Flux<V> flux = createDissolvingFlux(() -> commandBuilder.getMasterAddrByKey(key));

        return flux.collectList().flatMap(list -> {

            if (list.isEmpty()) {
                return Flux.empty();
            }

            LettuceAssert.isTrue(list.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
            String hostname = (String) list.get(0);
            String port = (String) list.get(1);
            return Mono.just(new InetSocketAddress(hostname, Integer.parseInt(port)));
        }).cast(SocketAddress.class).next();
    }

    @Override
    public Flux<Map<K, V>> masters() {
        return createDissolvingFlux(commandBuilder::masters);
    }

    @Override
    public Mono<Map<K, V>> master(K key) {
        return createMono(() -> commandBuilder.master(key));
    }

    @Override
    public Flux<Map<K, V>> slaves(K key) {
        return createDissolvingFlux(() -> commandBuilder.slaves(key));
    }

    @Override
    public Mono<Long> reset(K key) {
        return createMono(() -> commandBuilder.reset(key));
    }

    @Override
    public Mono<String> failover(K key) {
        return createMono(() -> commandBuilder.failover(key));
    }

    @Override
    public Mono<String> monitor(K key, String ip, int port, int quorum) {
        return createMono(() -> commandBuilder.monitor(key, ip, port, quorum));
    }

    @Override
    public Mono<String> set(K key, String option, V value) {
        return createMono(() -> commandBuilder.set(key, option, value));
    }

    @Override
    public Mono<String> remove(K key) {
        return createMono(() -> commandBuilder.remove(key));
    }

    @Override
    public Mono<String> ping() {
        return createMono(commandBuilder::ping);
    }

    // @Override
    public void close() {
        connection.close();
    }

    // @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public StatefulRedisSentinelConnection<K, V> getStatefulConnection() {
        return (StatefulRedisSentinelConnection<K, V>) connection;
    }
}
