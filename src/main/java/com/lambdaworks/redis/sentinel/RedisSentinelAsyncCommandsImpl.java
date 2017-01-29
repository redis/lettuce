/*
 * Copyright 2011-2017 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import com.lambdaworks.redis.KillArgs;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;

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

        Command<K, V, List<V>> cmd = commandBuilder.getMasterAddrByKey(key);
        CompletionStage<List<V>> future = dispatch(cmd);
        AtomicReference<SocketAddress> ref = new AtomicReference<>();
        AsyncCommand<K, V, SocketAddress> convert = new AsyncCommand<K, V, SocketAddress>((RedisCommand) cmd) {
            @Override
            protected void completeResult() {
                complete(ref.get());
            }
        };

        future.whenComplete((list, t) -> {

            if (t != null) {
                convert.completeExceptionally(t);
                return;
            }

            if (!list.isEmpty()) {
                LettuceAssert.isTrue(list.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
                String hostname = (String) list.get(0);
                String port = (String) list.get(1);
                ref.set(new InetSocketAddress(hostname, Integer.parseInt(port)));
            }

            convert.complete();

        });

        return convert;
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
    public RedisFuture<String> info() {
        return dispatch(commandBuilder.info());
    }

    @Override
    public RedisFuture<String> info(String section) {
        return dispatch(commandBuilder.info(section));
    }

    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        return (AsyncCommand<K, V, T>) connection.dispatch(new AsyncCommand<>(cmd));
    }

    public void close() {
        connection.close();
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public StatefulRedisSentinelConnection<K, V> getStatefulConnection() {
        return (StatefulRedisSentinelConnection<K, V>) connection;
    }
}
