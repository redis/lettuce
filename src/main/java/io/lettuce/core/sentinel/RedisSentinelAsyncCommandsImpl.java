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
import java.util.List;
import java.util.Map;

import io.lettuce.core.ClientListArgs;
import io.lettuce.core.KillArgs;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;

/**
 * An asynchronous and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisSentinelAsyncCommandsImpl<K, V> implements RedisSentinelAsyncCommands<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;

    private final StatefulConnection<K, V> connection;

    public RedisSentinelAsyncCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        commandBuilder = new SentinelCommandBuilder<K, V>(codec);
    }

    @Override
    public RedisFuture<SocketAddress> getMasterAddrByName(K key) {
        return dispatch(commandBuilder.getMasterAddrByKey(key));
    }

    @Override
    public RedisFuture<List<Map<K, V>>> masters() {
        return dispatch(commandBuilder.masters());
    }

    @Override
    public RedisFuture<Map<K, V>> master(K key) {
        return dispatch(commandBuilder.master(key));
    }

    @Override
    public RedisFuture<List<Map<K, V>>> slaves(K key) {
        return dispatch(commandBuilder.slaves(key));
    }

    @Override
    public RedisFuture<List<Map<K, V>>> replicas(K key) {
        return dispatch(commandBuilder.replicas(key));
    }

    @Override
    public RedisFuture<Long> reset(K key) {
        return dispatch(commandBuilder.reset(key));
    }

    @Override
    public RedisFuture<String> failover(K key) {
        return dispatch(commandBuilder.failover(key));
    }

    @Override
    public RedisFuture<String> monitor(K key, String ip, int port, int quorum) {
        return dispatch(commandBuilder.monitor(key, ip, port, quorum));
    }

    @Override
    public RedisFuture<String> set(K key, String option, V value) {
        return dispatch(commandBuilder.set(key, option, value));
    }

    @Override
    public RedisFuture<String> remove(K key) {
        return dispatch(commandBuilder.remove(key));
    }

    @Override
    public RedisFuture<String> ping() {
        return dispatch(commandBuilder.ping());
    }

    @Override
    public RedisFuture<K> clientGetname() {
        return dispatch(commandBuilder.clientGetname());
    }

    @Override
    public RedisFuture<String> clientSetname(K name) {
        return dispatch(commandBuilder.clientSetname(name));
    }

    @Override
    public RedisFuture<String> clientSetinfo(String key, String value) {
        return dispatch(commandBuilder.clientSetinfo(key, value));
    }

    @Override
    public RedisFuture<String> clientKill(String addr) {
        return dispatch(commandBuilder.clientKill(addr));
    }

    @Override
    public RedisFuture<Long> clientKill(KillArgs killArgs) {
        return dispatch(commandBuilder.clientKill(killArgs));
    }

    @Override
    public RedisFuture<String> clientPause(long timeout) {
        return dispatch(commandBuilder.clientPause(timeout));
    }

    @Override
    public RedisFuture<String> clientList() {
        return dispatch(commandBuilder.clientList());
    }

    @Override
    public RedisFuture<String> clientList(ClientListArgs clientListArgs) {
        return dispatch(commandBuilder.clientList(clientListArgs));
    }

    @Override
    public RedisFuture<String> clientInfo() {
        return dispatch(commandBuilder.clientInfo());
    }

    @Override
    public RedisFuture<String> info() {
        return dispatch(commandBuilder.info());
    }

    @Override
    public RedisFuture<String> info(String section) {
        return dispatch(commandBuilder.info(section));
    }

    @Override
    public <T> RedisFuture<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, T> output) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");

        return dispatch(new AsyncCommand<>(new Command<>(type, output)));
    }

    @Override
    public <T> RedisFuture<T> dispatch(ProtocolKeyword type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {

        LettuceAssert.notNull(type, "Command type must not be null");
        LettuceAssert.notNull(output, "CommandOutput type must not be null");
        LettuceAssert.notNull(args, "CommandArgs type must not be null");

        return dispatch(new AsyncCommand<>(new Command<>(type, output, args)));
    }

    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {

        AsyncCommand<K, V, T> asyncCommand = new AsyncCommand<>(cmd);
        RedisCommand<K, V, T> dispatched = connection.dispatch(asyncCommand);
        if (dispatched instanceof AsyncCommand) {
            return (AsyncCommand<K, V, T>) dispatched;
        }
        return asyncCommand;
    }

    @Override
    public StatefulRedisSentinelConnection<K, V> getStatefulConnection() {
        return (StatefulRedisSentinelConnection<K, V>) connection;
    }

}
