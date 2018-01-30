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

import static io.lettuce.core.protocol.CommandKeyword.*;
import static io.lettuce.core.protocol.CommandType.*;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import io.lettuce.core.KillArgs;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class SentinelCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    public SentinelCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    public Command<K, V, SocketAddress> getMasterAddrByKey(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add("get-master-addr-by-name").addKey(key);
        return createCommand(SENTINEL, new SocketAddressOutput<>(codec), args);
    }

    public Command<K, V, List<Map<K, V>>> masters() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add("masters");
        return createCommand(SENTINEL, new ListOfMapsOutput<>(codec), args);
    }

    public Command<K, V, Map<K, V>> master(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add("master").addKey(key);
        return createCommand(SENTINEL, new MapOutput<>(codec), args);
    }

    public Command<K, V, List<Map<K, V>>> slaves(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SLAVES).addKey(key);
        return createCommand(SENTINEL, new ListOfMapsOutput<>(codec), args);
    }

    public Command<K, V, Long> reset(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(RESET).addKey(key);
        return createCommand(SENTINEL, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, String> failover(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FAILOVER).addKey(key);
        return createCommand(SENTINEL, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> monitor(K key, String ip, int port, int quorum) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(MONITOR).addKey(key).add(ip).add(port).add(quorum);
        return createCommand(SENTINEL, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> set(K key, String option, V value) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SET).addKey(key).add(option).addValue(value);
        return createCommand(SENTINEL, new StatusOutput<>(codec), args);
    }

    public Command<K, V, K> clientGetname() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GETNAME);
        return createCommand(CLIENT, new KeyOutput<>(codec), args);
    }

    public Command<K, V, String> clientSetname(K name) {
        LettuceAssert.notNull(name, "Name must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETNAME).addKey(name);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> clientKill(String addr) {
        LettuceAssert.notNull(addr, "Addr must not be null");
        LettuceAssert.notEmpty(addr, "Addr must not be empty");

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KILL).add(addr);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    public Command<K, V, Long> clientKill(KillArgs killArgs) {
        LettuceAssert.notNull(killArgs, "KillArgs must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KILL);
        killArgs.build(args);
        return createCommand(CLIENT, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, String> clientPause(long timeout) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(PAUSE).add(timeout);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> clientList() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LIST);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> info() {
        return createCommand(INFO, new StatusOutput<>(codec));
    }

    public Command<K, V, String> info(String section) {
        LettuceAssert.notNull(section, "Section must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(section);
        return createCommand(INFO, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> ping() {
        return createCommand(PING, new StatusOutput<>(codec));
    }

    public Command<K, V, String> remove(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.REMOVE).addKey(key);
        return createCommand(SENTINEL, new StatusOutput<>(codec), args);
    }

}
