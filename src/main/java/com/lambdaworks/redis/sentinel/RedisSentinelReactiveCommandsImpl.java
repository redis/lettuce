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
import java.util.function.Supplier;

import rx.Observable;

import com.lambdaworks.redis.KillArgs;
import com.lambdaworks.redis.ReactiveCommandDispatcher;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.rx.RedisSentinelReactiveCommands;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisSentinelReactiveCommandsImpl<K, V> implements RedisSentinelReactiveCommands<K, V> {

    private final SentinelCommandBuilder<K, V> commandBuilder;
    private final StatefulConnection<K, V> connection;

    public RedisSentinelReactiveCommandsImpl(StatefulConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        commandBuilder = new SentinelCommandBuilder<>(codec);
    }

    @Override
    public Observable<SocketAddress> getMasterAddrByName(K key) {

        Observable<V> observable = createDissolvingObservable(() -> commandBuilder.getMasterAddrByKey(key));
        return observable.buffer(2).map(list -> {
            if (list.isEmpty()) {
                return null;
            }

            LettuceAssert.isTrue(list.size() == 2, "List must contain exact 2 entries (Hostname, Port)");
            String hostname = (String) list.get(0);
            String port = (String) list.get(1);
            return new InetSocketAddress(hostname, Integer.parseInt(port));
        });
    }

    @Override
    public Observable<Map<K, V>> masters() {
        return createDissolvingObservable(() -> commandBuilder.masters());
    }

    @Override
    public Observable<Map<K, V>> master(K key) {
        return createObservable(() -> commandBuilder.master(key));
    }

    @Override
    public Observable<Map<K, V>> slaves(K key) {
        return createDissolvingObservable(() -> commandBuilder.slaves(key));
    }

    @Override
    public Observable<Long> reset(K key) {
        return createObservable(() -> commandBuilder.reset(key));
    }

    @Override
    public Observable<String> failover(K key) {
        return createObservable(() -> commandBuilder.failover(key));
    }

    @Override
    public Observable<String> monitor(K key, String ip, int port, int quorum) {
        return createObservable(() -> commandBuilder.monitor(key, ip, port, quorum));
    }

    @Override
    public Observable<String> set(K key, String option, V value) {
        return createObservable(() -> commandBuilder.set(key, option, value));
    }

    @Override
    public Observable<String> remove(K key) {
        return createObservable(() -> commandBuilder.remove(key));
    }

    @Override
    public Observable<String> ping() {
        return createObservable(() -> commandBuilder.ping());
    }

    @Override
    public Observable<K> clientGetname() {
        return createObservable(commandBuilder::clientGetname);
    }

    @Override
    public Observable<String> clientSetname(K name) {
        return createObservable(() -> commandBuilder.clientSetname(name));
    }

    @Override
    public Observable<String> clientKill(String addr) {
        return createObservable(() -> commandBuilder.clientKill(addr));
    }

    @Override
    public Observable<Long> clientKill(KillArgs killArgs) {
        return createObservable(() -> commandBuilder.clientKill(killArgs));
    }

    @Override
    public Observable<String> clientPause(long timeout) {
        return createObservable(() -> commandBuilder.clientPause(timeout));
    }

    @Override
    public Observable<String> clientList() {
        return createObservable(commandBuilder::clientList);
    }

    @Override
    public Observable<String> info() {
        return createObservable(commandBuilder::info);
    }

    @Override
    public Observable<String> info(String section) {
        return createObservable(() -> commandBuilder.info(section));
    }

    @Override
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

    public <T> Observable<T> createObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return Observable.create(new ReactiveCommandDispatcher<>(commandSupplier, connection, false));
    }

    @SuppressWarnings("unchecked")
    public <T, R> R createDissolvingObservable(Supplier<RedisCommand<K, V, T>> commandSupplier) {
        return (R) Observable.create(new ReactiveCommandDispatcher<>(commandSupplier, connection, true));
    }
}
