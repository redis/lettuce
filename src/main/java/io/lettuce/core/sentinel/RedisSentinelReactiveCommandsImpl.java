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
package io.lettuce.core.sentinel;

import java.net.SocketAddress;
import java.util.Map;
import java.util.function.Supplier;

import io.lettuce.core.AbstractRedisReactiveCommands;
import io.lettuce.core.ClientListArgs;
import io.lettuce.core.KillArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
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

    public RedisSentinelReactiveCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec,
            Supplier<JsonParser> parser) {
        super(connection, codec, parser);
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
    public Flux<Map<K, V>> replicas(K key) {
        return createDissolvingFlux(() -> commandBuilder.replicas(key));
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
    public Mono<String> clientSetinfo(String key, String value) {
        return createMono(() -> commandBuilder.clientSetinfo(key, value));
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
    public Mono<String> clientList(ClientListArgs clientListArgs) {
        return createMono(() -> commandBuilder.clientList(clientListArgs));
    }

    @Override
    public Mono<String> clientInfo() {
        return createMono(commandBuilder::clientInfo);
    }

    @Override
    public Mono<String> info() {
        return createMono(commandBuilder::info);
    }

    @Override
    public Mono<String> info(String section) {
        return createMono(() -> commandBuilder.info(section));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> Flux<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");

        return (Flux) createFlux(() -> new Command<>(type, output));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> Flux<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, ?> output, CommandArgs<K, V> args) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");
        LettuceAssert.notNull(args, "CommandArgs type must not be null");

        return (Flux) createFlux(() -> new Command<>(type, output, args));
    }

    @Override
    public StatefulRedisSentinelConnection<K, V> getStatefulConnection() {
        return (StatefulRedisSentinelConnection<K, V>) super.getConnection();
    }

}
