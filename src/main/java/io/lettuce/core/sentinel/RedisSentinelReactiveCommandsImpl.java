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
package io.lettuce.core.sentinel;

import java.net.SocketAddress;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.AbstractRedisReactiveCommands;
import io.lettuce.core.KillArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisSentinelReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V> implements
        RedisSentinelReactiveCommands<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;

    public RedisSentinelReactiveCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);
    }

    @Override
    public Mono<SocketAddress> getMasterAddrByName(K key) {
        return createMono(() -> commandBuilder.getMasterAddrByKey(key));
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

    @Override
    public Mono<K> clientGetname() {
        return createMono(commandBuilder::clientGetname);
    }

    @Override
    public Mono<String> clientSetname(K name) {
        return createMono(() -> commandBuilder.clientSetname(name));
    }

    @Override
    public Mono<String> clientKill(String addr) {
        return createMono(() -> commandBuilder.clientKill(addr));
    }

    @Override
    public Mono<Long> clientKill(KillArgs killArgs) {
        return createMono(() -> commandBuilder.clientKill(killArgs));
    }

    @Override
    public Mono<String> clientPause(long timeout) {
        return createMono(() -> commandBuilder.clientPause(timeout));
    }

    @Override
    public Mono<String> clientList() {
        return createMono(commandBuilder::clientList);
    }

    @Override
    public Mono<String> info() {
        return createMono(commandBuilder::info);
    }

    @Override
    public Mono<String> info(String section) {
        return createMono(() -> commandBuilder.info(section));
    }

    @Override
    public void close() {
        getStatefulConnection().close();
    }

    @Override
    public boolean isOpen() {
        return getStatefulConnection().isOpen();
    }

    @Override
    public StatefulRedisSentinelConnection<K, V> getStatefulConnection() {
        return (StatefulRedisSentinelConnection<K, V>) super.getConnection();
    }
}
