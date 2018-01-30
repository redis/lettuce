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
package io.lettuce.core;

import static io.lettuce.core.LettuceStrings.string;
import static io.lettuce.core.protocol.CommandKeyword.*;
import static io.lettuce.core.protocol.CommandType.*;

import java.nio.ByteBuffer;
import java.util.*;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;
import io.lettuce.core.protocol.*;

/**
 * @param <K>
 * @param <V>
 * @author Mark Paluch
 */
@SuppressWarnings({ "unchecked", "varargs" })
class RedisCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    private static final String MUST_NOT_CONTAIN_NULL_ELEMENTS = "must not contain null elements";
    private static final String MUST_NOT_BE_EMPTY = "must not be empty";
    private static final String MUST_NOT_BE_NULL = "must not be null";

    private static final byte[] MINUS_BYTES = { '-' };
    private static final byte[] PLUS_BYTES = { '+' };

    private final RedisCodec<String, String> STRING_CODEC = new Utf8StringCodec();

    RedisCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, Long> append(K key, V value) {
        notNullKey(key);

        return createCommand(APPEND, new IntegerOutput<>(codec), key, value);
    }

    Command<K, V, String> asking() {

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        return createCommand(ASKING, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> auth(String password) {
        LettuceAssert.notNull(password, "Password " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(password, "Password " + MUST_NOT_BE_EMPTY);

        return auth(password.toCharArray());
    }

    Command<K, V, String> auth(char[] password) {
        LettuceAssert.notNull(password, "Password " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(password.length > 0, "Password " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(password);
        return createCommand(AUTH, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> bgrewriteaof() {
        return createCommand(BGREWRITEAOF, new StatusOutput<>(codec));
    }

    Command<K, V, String> bgsave() {
        return createCommand(BGSAVE, new StatusOutput<>(codec));
    }

    Command<K, V, Long> bitcount(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(BITCOUNT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitcount(K key, long start, long end) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(start).add(end);
        return createCommand(BITCOUNT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Long>> bitfield(K key, BitFieldArgs bitFieldArgs) {
        notNullKey(key);
        LettuceAssert.notNull(bitFieldArgs, "BitFieldArgs must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        bitFieldArgs.build(args);

        return createCommand(BITFIELD, (CommandOutput) new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Value<Long>>> bitfieldValue(K key, BitFieldArgs bitFieldArgs) {
        notNullKey(key);
        LettuceAssert.notNull(bitFieldArgs, "BitFieldArgs must not be null");

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        bitFieldArgs.build(args);

        return createCommand(BITFIELD, (CommandOutput) new ValueValueListOutput<>(codec), args);
    }

    Command<K, V, Long> bitopAnd(K destination, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(AND).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitopNot(K destination, K source) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(NOT).addKey(destination).addKey(source);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitopOr(K destination, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(OR).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitopXor(K destination, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(XOR).addKey(destination).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitpos(K key, boolean state) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(state ? 1 : 0);
        return createCommand(BITPOS, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitpos(K key, boolean state, long start) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(state ? 1 : 0).add(start);
        return createCommand(BITPOS, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitpos(K key, boolean state, long start, long end) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(state ? 1 : 0).add(start).add(end);
        return createCommand(BITPOS, new IntegerOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> blpop(long timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);
        return createCommand(BLPOP, new KeyValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> brpop(long timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);
        return createCommand(BRPOP, new KeyValueOutput<>(codec), args);
    }

    Command<K, V, V> brpoplpush(long timeout, K source, K destination) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(source).addKey(destination).add(timeout);
        return createCommand(BRPOPLPUSH, new ValueOutput<>(codec), args);
    }

    Command<K, V, K> clientGetname() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GETNAME);
        return createCommand(CLIENT, new KeyOutput<>(codec), args);
    }

    Command<K, V, String> clientKill(String addr) {
        LettuceAssert.notNull(addr, "Addr " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(addr, "Addr " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KILL).add(addr);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> clientKill(KillArgs killArgs) {
        LettuceAssert.notNull(killArgs, "KillArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KILL);
        killArgs.build(args);
        return createCommand(CLIENT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> clientList() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LIST);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clientPause(long timeout) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(PAUSE).add(timeout);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clientSetname(K name) {
        LettuceAssert.notNull(name, "Name " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETNAME).addKey(name);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterAddslots(int[] slots) {
        notEmptySlots(slots);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(ADDSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterBumpepoch() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(BUMPEPOCH);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> clusterCountFailureReports(String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add("COUNT-FAILURE-REPORTS").add(nodeId);
        return createCommand(CLUSTER, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> clusterCountKeysInSlot(int slot) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(COUNTKEYSINSLOT).add(slot);
        return createCommand(CLUSTER, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> clusterDelslots(int[] slots) {
        notEmptySlots(slots);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(DELSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterFailover(boolean force) {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FAILOVER);
        if (force) {
            args.add(FORCE);
        }
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterFlushslots() {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FLUSHSLOTS);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterForget(String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FORGET).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<K>> clusterGetKeysInSlot(int slot, int count) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GETKEYSINSLOT).add(slot).add(count);
        return createCommand(CLUSTER, new KeyListOutput<>(codec), args);
    }

    Command<K, V, String> clusterInfo() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(INFO);

        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> clusterKeyslot(K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KEYSLOT).addKey(key);
        return createCommand(CLUSTER, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> clusterMeet(String ip, int port) {
        LettuceAssert.notNull(ip, "IP " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(ip, "IP " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(MEET).add(ip).add(port);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterMyId() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(MYID);

        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterNodes() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(NODES);

        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterReplicate(String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(REPLICATE).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterReset(boolean hard) {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(RESET);
        if (hard) {
            args.add(HARD);
        } else {
            args.add(SOFT);
        }
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterSaveconfig() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SAVECONFIG);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterSetConfigEpoch(long configEpoch) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add("SET-CONFIG-EPOCH").add(configEpoch);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterSetSlotImporting(int slot, String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETSLOT).add(slot).add(IMPORTING).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterSetSlotMigrating(int slot, String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETSLOT).add(slot).add(MIGRATING).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterSetSlotNode(int slot, String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETSLOT).add(slot).add(NODE).add(nodeId);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterSetSlotStable(int slot) {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETSLOT).add(slot).add(STABLE);
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<String>> clusterSlaves(String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SLAVES).add(nodeId);
        return createCommand(CLUSTER, new StringListOutput<>(codec), args);
    }

    Command<K, V, List<Object>> clusterSlots() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SLOTS);
        return createCommand(CLUSTER, new ArrayOutput<>(codec), args);
    }

    Command<K, V, List<Object>> command() {
        CommandArgs<K, V> args = new CommandArgs<K, V>((RedisCodec) STRING_CODEC);
        return createCommand(COMMAND, new ArrayOutput<K, V>((RedisCodec) STRING_CODEC), args);
    }

    Command<K, V, Long> commandCount() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(COUNT);
        return createCommand(COMMAND, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Object>> commandInfo(String... commands) {
        LettuceAssert.notNull(commands, "Commands " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(commands, "Commands " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(commands, "Commands " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<K, V>((RedisCodec) STRING_CODEC);
        args.add(INFO);

        for (String command : commands) {
            args.add(command);
        }

        return createCommand(COMMAND, new ArrayOutput<K, V>((RedisCodec) STRING_CODEC), args);
    }

    Command<K, V, Map<String, String>> configGet(String parameter) {
        LettuceAssert.notNull(parameter, "Parameter " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(parameter, "Parameter " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>((RedisCodec) STRING_CODEC).add(GET).add(parameter);
        return createCommand(CONFIG, new MapOutput<>((RedisCodec) STRING_CODEC), args);
    }

    Command<K, V, String> configResetstat() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(RESETSTAT);
        return createCommand(CONFIG, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> configRewrite() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(REWRITE);
        return createCommand(CONFIG, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> configSet(String parameter, String value) {
        LettuceAssert.notNull(parameter, "Parameter " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(parameter, "Parameter " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(value, "Value " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SET).add(parameter).add(value);
        return createCommand(CONFIG, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> dbsize() {
        return createCommand(DBSIZE, new IntegerOutput<>(codec));
    }

    Command<K, V, String> debugCrashAndRecover(Long delay) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add("CRASH-AND-RECOVER");
        if (delay != null) {
            args.add(delay);
        }
        return createCommand(DEBUG, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> debugHtstats(int db) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(HTSTATS).add(db);
        return createCommand(DEBUG, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> debugObject(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(OBJECT).addKey(key);
        return createCommand(DEBUG, new StatusOutput<>(codec), args);
    }

    Command<K, V, Void> debugOom() {
        return createCommand(DEBUG, null, new CommandArgs<>(codec).add("OOM"));
    }

    Command<K, V, String> debugReload() {
        return createCommand(DEBUG, new StatusOutput<>(codec), new CommandArgs<>(codec).add(RELOAD));
    }

    Command<K, V, String> debugRestart(Long delay) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(RESTART);
        if (delay != null) {
            args.add(delay);
        }
        return createCommand(DEBUG, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> debugSdslen(K key) {
        notNullKey(key);

        return createCommand(DEBUG, new StatusOutput<>(codec), new CommandArgs<>(codec).add("SDSLEN").addKey(key));
    }

    Command<K, V, Void> debugSegfault() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SEGFAULT);
        return createCommand(DEBUG, null, args);
    }

    Command<K, V, Long> decr(K key) {
        notNullKey(key);

        return createCommand(DECR, new IntegerOutput<>(codec), key);
    }

    Command<K, V, Long> decrby(K key, long amount) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(amount);
        return createCommand(DECRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> del(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> del(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(DEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> discard() {
        return createCommand(DISCARD, new StatusOutput<>(codec));
    }

    Command<K, V, byte[]> dump(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(DUMP, new ByteArrayOutput<>(codec), args);
    }

    Command<K, V, V> echo(V msg) {
        LettuceAssert.notNull(msg, "message " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addValue(msg);
        return createCommand(ECHO, new ValueOutput<>(codec), args);
    }

    <T> Command<K, V, T> eval(String script, ScriptOutputType type, K... keys) {
        LettuceAssert.notNull(script, "Script " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(script, "Script " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(script).add(keys.length).addKeys(keys);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVAL, output, args);
    }

    <T> Command<K, V, T> eval(String script, ScriptOutputType type, K[] keys, V... values) {
        LettuceAssert.notNull(script, "Script " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(script, "Script " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(script).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVAL, output, args);
    }

    <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K... keys) {
        LettuceAssert.notNull(digest, "Digest " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(digest, "Digest " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(digest).add(keys.length).addKeys(keys);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVALSHA, output, args);
    }

    <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        LettuceAssert.notNull(digest, "Digest " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(digest, "Digest " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(digest).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);
        return createCommand(EVALSHA, output, args);
    }

    Command<K, V, Boolean> exists(K key) {
        notNullKey(key);

        return createCommand(EXISTS, new BooleanOutput<>(codec), key);
    }

    Command<K, V, Long> exists(K... keys) {
        notEmpty(keys);

        return createCommand(EXISTS, new IntegerOutput<>(codec), new CommandArgs<>(codec).addKeys(keys));
    }

    Command<K, V, Long> exists(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        return createCommand(EXISTS, new IntegerOutput<>(codec), new CommandArgs<>(codec).addKeys(keys));
    }

    Command<K, V, Boolean> expire(K key, long seconds) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(seconds);
        return createCommand(EXPIRE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Boolean> expireat(K key, long timestamp) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp);
        return createCommand(EXPIREAT, new BooleanOutput<>(codec), args);
    }

    Command<K, V, String> flushall() {
        return createCommand(FLUSHALL, new StatusOutput<>(codec));
    }

    Command<K, V, String> flushallAsync() {
        return createCommand(FLUSHALL, new StatusOutput<>(codec), new CommandArgs<>(codec).add(ASYNC));
    }

    Command<K, V, String> flushdb() {
        return createCommand(FLUSHDB, new StatusOutput<>(codec));
    }

    Command<K, V, String> flushdbAsync() {
        return createCommand(FLUSHDB, new StatusOutput<>(codec), new CommandArgs<>(codec).add(ASYNC));
    }

    Command<K, V, Long> geoadd(K key, double longitude, double latitude, V member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(longitude).add(latitude).addValue(member);
        return createCommand(GEOADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> geoadd(K key, Object[] lngLatMember) {

        notNullKey(key);
        LettuceAssert.notNull(lngLatMember, "LngLatMember " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(lngLatMember, "LngLatMember " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(lngLatMember, "LngLatMember " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
        LettuceAssert
                .isTrue(lngLatMember.length % 3 == 0,
                        "LngLatMember.length must be a multiple of 3 and contain a "
                                + "sequence of longitude1, latitude1, member1, longitude2, latitude2, member2, ... longitudeN, latitudeN, memberN");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        for (int i = 0; i < lngLatMember.length; i += 3) {
            args.add((Double) lngLatMember[i]);
            args.add((Double) lngLatMember[i + 1]);
            args.addValue((V) lngLatMember[i + 2]);
        }

        return createCommand(GEOADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Double> geodist(K key, V from, V to, GeoArgs.Unit unit) {
        notNullKey(key);
        LettuceAssert.notNull(from, "From " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(from, "To " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(from).addValue(to);

        if (unit != null) {
            args.add(unit.name());
        }

        return createCommand(GEODIST, new DoubleOutput<>(codec), args);
    }

    Command<K, V, List<Value<String>>> geohash(K key, V... members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(members);
        return createCommand(GEOHASH, new StringValueListOutput<>(codec), args);
    }

    Command<K, V, List<GeoCoordinates>> geopos(K key, V[] members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(members);

        return createCommand(GEOPOS, new GeoCoordinatesListOutput<>(codec), args);
    }

    Command<K, V, List<Value<GeoCoordinates>>> geoposValues(K key, V[] members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(members);

        return createCommand(GEOPOS, new GeoCoordinatesValueListOutput<>(codec), args);
    }

    Command<K, V, Set<V>> georadius(CommandType commandType, K key, double longitude, double latitude, double distance,
            String unit) {
        notNullKey(key);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(longitude).add(latitude).add(distance).add(unit);
        return createCommand(commandType, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, List<GeoWithin<V>>> georadius(CommandType commandType, K key, double longitude, double latitude,
            double distance, String unit, GeoArgs geoArgs) {

        notNullKey(key);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(geoArgs, "GeoArgs " + MUST_NOT_BE_NULL);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(longitude).add(latitude).add(distance).add(unit);
        geoArgs.build(args);

        return createCommand(commandType, new GeoWithinListOutput<>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(),
                geoArgs.isWithCoordinates()), args);
    }

    Command<K, V, Long> georadius(K key, double longitude, double latitude, double distance, String unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {

        notNullKey(key);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(geoRadiusStoreArgs, "GeoRadiusStoreArgs " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(geoRadiusStoreArgs.getStoreKey() != null || geoRadiusStoreArgs.getStoreDistKey() != null,
                "At least STORE key or STORDIST key is required");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(longitude).add(latitude).add(distance).add(unit);
        geoRadiusStoreArgs.build(args);

        return createCommand(GEORADIUS, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Set<V>> georadiusbymember(CommandType commandType, K key, V member, double distance, String unit) {

        notNullKey(key);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(member).add(distance).add(unit);
        return createCommand(commandType, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, List<GeoWithin<V>>> georadiusbymember(CommandType commandType, K key, V member, double distance, String unit,
            GeoArgs geoArgs) {

        notNullKey(key);
        LettuceAssert.notNull(geoArgs, "GeoArgs " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(member).add(distance).add(unit);
        geoArgs.build(args);

        return createCommand(commandType, new GeoWithinListOutput<>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(),
                geoArgs.isWithCoordinates()), args);
    }

    Command<K, V, Long> georadiusbymember(K key, V member, double distance, String unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {

        notNullKey(key);
        LettuceAssert.notNull(geoRadiusStoreArgs, "GeoRadiusStoreArgs " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);
        LettuceAssert.isTrue(geoRadiusStoreArgs.getStoreKey() != null || geoRadiusStoreArgs.getStoreDistKey() != null,
                "At least STORE key or STORDIST key is required");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(member).add(distance).add(unit);
        geoRadiusStoreArgs.build(args);

        return createCommand(GEORADIUSBYMEMBER, new IntegerOutput<>(codec), args);
    }

    Command<K, V, V> get(K key) {
        notNullKey(key);

        return createCommand(GET, new ValueOutput<>(codec), key);
    }

    Command<K, V, Long> getbit(K key, long offset) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(offset);
        return createCommand(GETBIT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, V> getrange(K key, long start, long end) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(end);
        return createCommand(GETRANGE, new ValueOutput<>(codec), args);
    }

    Command<K, V, V> getset(K key, V value) {
        notNullKey(key);

        return createCommand(GETSET, new ValueOutput<>(codec), key, value);
    }

    Command<K, V, Long> hdel(K key, K... fields) {
        notNullKey(key);
        LettuceAssert.notNull(fields, "Fields " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(fields, "Fields " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HDEL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Boolean> hexists(K key, K field) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field);
        return createCommand(HEXISTS, new BooleanOutput<>(codec), args);
    }

    Command<K, V, V> hget(K key, K field) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field);
        return createCommand(HGET, new ValueOutput<>(codec), args);
    }

    Command<K, V, Map<K, V>> hgetall(K key) {
        notNullKey(key);

        return createCommand(HGETALL, new MapOutput<>(codec), key);
    }

    Command<K, V, Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return createCommand(HGETALL, new KeyValueStreamingOutput<>(codec, channel), key);
    }

    Command<K, V, Long> hincrby(K key, K field, long amount) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field).add(amount);
        return createCommand(HINCRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Double> hincrbyfloat(K key, K field, double amount) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field).add(amount);
        return createCommand(HINCRBYFLOAT, new DoubleOutput<>(codec), args);
    }

    Command<K, V, List<K>> hkeys(K key) {
        notNullKey(key);

        return createCommand(HKEYS, new KeyListOutput<>(codec), key);
    }

    Command<K, V, Long> hkeys(KeyStreamingChannel<K> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return createCommand(HKEYS, new KeyStreamingOutput<>(codec, channel), key);
    }

    Command<K, V, Long> hlen(K key) {
        notNullKey(key);

        return createCommand(HLEN, new IntegerOutput<>(codec), key);
    }

    Command<K, V, List<V>> hmget(K key, K... fields) {
        notNullKey(key);
        LettuceAssert.notNull(fields, "Fields " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(fields, "Fields " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields) {
        notNullKey(key);
        LettuceAssert.notNull(fields, "Fields " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(fields, "Fields " + MUST_NOT_BE_EMPTY);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> hmget(KeyValueStreamingChannel<K, V> channel, K key, K... fields) {
        notNullKey(key);
        LettuceAssert.notNull(fields, "Fields " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(fields, "Fields " + MUST_NOT_BE_EMPTY);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new KeyValueStreamingOutput<>(codec, channel, Arrays.asList(fields)), args);
    }

    Command<K, V, List<KeyValue<K, V>>> hmgetKeyValue(K key, K... fields) {
        notNullKey(key);
        LettuceAssert.notNull(fields, "Fields " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(fields, "Fields " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new KeyValueListOutput<>(codec, Arrays.asList(fields)), args);
    }

    Command<K, V, String> hmset(K key, Map<K, V> map) {
        notNullKey(key);
        LettuceAssert.notNull(map, "Map " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!map.isEmpty(), "Map " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(map);
        return createCommand(HMSET, new StatusOutput<>(codec), args);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key) {
        notNullKey(key);

        return hscan(key, ScanCursor.INITIAL, null);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        notNullKey(key);

        return hscan(key, scanCursor, null);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        notNullKey(key);

        return hscan(key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        MapScanOutput<K, V> output = new MapScanOutput<>(codec);
        return createCommand(HSCAN, output, args);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return hscanStreaming(channel, key, ScanCursor.INITIAL, null);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        notNullKey(key);
        notNull(channel);

        return hscanStreaming(channel, key, scanCursor, null);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        return hscanStreaming(channel, key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        KeyValueScanStreamingOutput<K, V> output = new KeyValueScanStreamingOutput<>(codec, channel);
        return createCommand(HSCAN, output, args);
    }

    Command<K, V, Boolean> hset(K key, K field, V value) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field).addValue(value);
        return createCommand(HSET, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Boolean> hsetnx(K key, K field, V value) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field).addValue(value);
        return createCommand(HSETNX, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Long> hstrlen(K key, K field) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field);
        return createCommand(HSTRLEN, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<V>> hvals(K key) {
        notNullKey(key);

        return createCommand(HVALS, new ValueListOutput<>(codec), key);
    }

    Command<K, V, Long> hvals(ValueStreamingChannel<V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return createCommand(HVALS, new ValueStreamingOutput<>(codec, channel), key);
    }

    Command<K, V, Long> incr(K key) {
        notNullKey(key);

        return createCommand(INCR, new IntegerOutput<>(codec), key);
    }

    Command<K, V, Long> incrby(K key, long amount) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(amount);
        return createCommand(INCRBY, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Double> incrbyfloat(K key, double amount) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(amount);
        return createCommand(INCRBYFLOAT, new DoubleOutput<>(codec), args);
    }

    Command<K, V, String> info() {
        return createCommand(INFO, new StatusOutput<>(codec));
    }

    Command<K, V, String> info(String section) {
        LettuceAssert.notNull(section, "Section " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(section);
        return createCommand(INFO, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<K>> keys(K pattern) {
        LettuceAssert.notNull(pattern, "Pattern " + MUST_NOT_BE_NULL);

        return createCommand(KEYS, new KeyListOutput<>(codec), pattern);
    }

    Command<K, V, Long> keys(KeyStreamingChannel<K> channel, K pattern) {
        LettuceAssert.notNull(pattern, "Pattern " + MUST_NOT_BE_NULL);
        notNull(channel);

        return createCommand(KEYS, new KeyStreamingOutput<>(codec, channel), pattern);
    }

    Command<K, V, Date> lastsave() {
        return createCommand(LASTSAVE, new DateOutput<>(codec));
    }

    Command<K, V, V> lindex(K key, long index) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index);
        return createCommand(LINDEX, new ValueOutput<>(codec), args);
    }

    Command<K, V, Long> linsert(K key, boolean before, V pivot, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(before ? BEFORE : AFTER).addValue(pivot).addValue(value);
        return createCommand(LINSERT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> llen(K key) {
        notNullKey(key);

        return createCommand(LLEN, new IntegerOutput<>(codec), key);
    }

    Command<K, V, V> lpop(K key) {
        notNullKey(key);

        return createCommand(LPOP, new ValueOutput<>(codec), key);
    }

    Command<K, V, Long> lpush(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        return createCommand(LPUSH, new IntegerOutput<>(codec), key, values);
    }

    Command<K, V, Long> lpushx(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);

        return createCommand(LPUSHX, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<V>> lrange(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(LRANGE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(LRANGE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> lrem(K key, long count, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count).addValue(value);
        return createCommand(LREM, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> lset(K key, long index, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(index).addValue(value);
        return createCommand(LSET, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> ltrim(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(LTRIM, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<V>> mget(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> mget(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> mget(ValueStreamingChannel<V> channel, K... keys) {
        notEmpty(keys);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> mget(KeyValueStreamingChannel<K, V> channel, K... keys) {
        notEmpty(keys);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new KeyValueStreamingOutput<>(codec, channel, Arrays.asList(keys)), args);
    }

    Command<K, V, Long> mget(ValueStreamingChannel<V> channel, Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> mget(KeyValueStreamingChannel<K, V> channel, Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new KeyValueStreamingOutput<>(codec, channel, keys), args);
    }

    Command<K, V, List<KeyValue<K, V>>> mgetKeyValue(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new KeyValueListOutput<>(codec, Arrays.asList(keys)), args);
    }

    Command<K, V, List<KeyValue<K, V>>> mgetKeyValue(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(MGET, new KeyValueListOutput<>(codec, keys), args);
    }

    Command<K, V, String> migrate(String host, int port, K key, int db, long timeout) {
        LettuceAssert.notNull(host, "Host " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(host, "Host " + MUST_NOT_BE_EMPTY);
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(host).add(port).addKey(key).add(db).add(timeout);
        return createCommand(MIGRATE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> migrate(String host, int port, int db, long timeout, MigrateArgs<K> migrateArgs) {
        LettuceAssert.notNull(host, "Host " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(host, "Host " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(migrateArgs, "migrateArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        args.add(host).add(port);

        if (migrateArgs.keys.size() == 1) {
            args.addKey(migrateArgs.keys.get(0));
        } else {
            args.add("");
        }

        args.add(db).add(timeout);
        migrateArgs.build(args);

        return createCommand(MIGRATE, new StatusOutput<>(codec), args);
    }

    Command<K, V, Boolean> move(K key, int db) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(db);
        return createCommand(MOVE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, String> mset(Map<K, V> map) {
        LettuceAssert.notNull(map, "Map " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!map.isEmpty(), "Map " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(map);
        return createCommand(MSET, new StatusOutput<>(codec), args);
    }

    Command<K, V, Boolean> msetnx(Map<K, V> map) {
        LettuceAssert.notNull(map, "Map " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!map.isEmpty(), "Map " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(map);
        return createCommand(MSETNX, new BooleanOutput<>(codec), args);
    }

    Command<K, V, String> multi() {
        return createCommand(MULTI, new StatusOutput<>(codec));
    }

    Command<K, V, String> objectEncoding(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(ENCODING).addKey(key);
        return createCommand(OBJECT, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> objectIdletime(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(IDLETIME).addKey(key);
        return createCommand(OBJECT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> objectRefcount(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(REFCOUNT).addKey(key);
        return createCommand(OBJECT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Boolean> persist(K key) {
        notNullKey(key);

        return createCommand(PERSIST, new BooleanOutput<>(codec), key);
    }

    Command<K, V, Boolean> pexpire(K key, long milliseconds) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(milliseconds);
        return createCommand(PEXPIRE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Boolean> pexpireat(K key, long timestamp) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp);
        return createCommand(PEXPIREAT, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Long> pfadd(K key, V value, V... moreValues) {
        notNullKey(key);
        LettuceAssert.notNull(value, "Value " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(moreValues, "MoreValues " + MUST_NOT_BE_NULL);
        LettuceAssert.noNullElements(moreValues, "MoreValues " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value).addValues(moreValues);
        return createCommand(PFADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> pfadd(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);
        LettuceAssert.noNullElements(values, "Values " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);
        return createCommand(PFADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> pfcount(K key, K... moreKeys) {
        notNullKey(key);
        LettuceAssert.notNull(moreKeys, "MoreKeys " + MUST_NOT_BE_NULL);
        LettuceAssert.noNullElements(moreKeys, "MoreKeys " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(moreKeys);
        return createCommand(PFCOUNT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> pfcount(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(PFCOUNT, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    Command<K, V, String> pfmerge(K destkey, K sourcekey, K... moreSourceKeys) {
        LettuceAssert.notNull(destkey, "Destkey " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(sourcekey, "Sourcekey " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(moreSourceKeys, "MoreSourceKeys " + MUST_NOT_BE_NULL);
        LettuceAssert.noNullElements(moreSourceKeys, "MoreSourceKeys " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(destkey).addKey(sourcekey).addKeys(moreSourceKeys);
        return createCommand(PFMERGE, new StatusOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    Command<K, V, String> pfmerge(K destkey, K... sourcekeys) {
        LettuceAssert.notNull(destkey, "Destkey " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(sourcekeys, "Sourcekeys " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(sourcekeys, "Sourcekeys " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(sourcekeys, "Sourcekeys " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(destkey).addKeys(sourcekeys);
        return createCommand(PFMERGE, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> ping() {
        return createCommand(PING, new StatusOutput<>(codec));
    }

    Command<K, V, String> psetex(K key, long milliseconds, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(milliseconds).addValue(value);
        return createCommand(PSETEX, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> pttl(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(PTTL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> publish(K channel, V message) {
        LettuceAssert.notNull(channel, "Channel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(channel).addValue(message);
        return createCommand(PUBLISH, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<K>> pubsubChannels() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CHANNELS);
        return createCommand(PUBSUB, new KeyListOutput<>(codec), args);
    }

    Command<K, V, List<K>> pubsubChannels(K pattern) {
        LettuceAssert.notNull(pattern, "Pattern " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CHANNELS).addKey(pattern);
        return createCommand(PUBSUB, new KeyListOutput<>(codec), args);
    }

    Command<K, V, Long> pubsubNumpat() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(NUMPAT);
        return createCommand(PUBSUB, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    Command<K, V, Map<K, Long>> pubsubNumsub(K... pattern) {
        LettuceAssert.notNull(pattern, "Pattern " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(pattern, "Pattern " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(NUMSUB).addKeys(pattern);
        return createCommand(PUBSUB, (MapOutput) new MapOutput<K, Long>((RedisCodec) codec), args);
    }

    Command<K, V, String> quit() {
        return createCommand(QUIT, new StatusOutput<>(codec));
    }

    Command<K, V, V> randomkey() {
        return createCommand(RANDOMKEY, new ValueOutput<>(codec));
    }

    Command<K, V, String> readOnly() {
        return createCommand(READONLY, new StatusOutput<>(codec));
    }

    Command<K, V, String> readWrite() {
        return createCommand(READWRITE, new StatusOutput<>(codec));
    }

    Command<K, V, String> rename(K key, K newKey) {
        notNullKey(key);
        LettuceAssert.notNull(newKey, "NewKey " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(newKey);
        return createCommand(RENAME, new StatusOutput<>(codec), args);
    }

    Command<K, V, Boolean> renamenx(K key, K newKey) {
        notNullKey(key);
        LettuceAssert.notNull(newKey, "NewKey " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(newKey);
        return createCommand(RENAMENX, new BooleanOutput<>(codec), args);
    }

    Command<K, V, String> restore(K key, long ttl, byte[] value) {
        notNullKey(key);
        LettuceAssert.notNull(value, "Value " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(ttl).add(value);
        return createCommand(RESTORE, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Object>> role() {
        return createCommand(ROLE, new ArrayOutput<>(codec));
    }

    Command<K, V, V> rpop(K key) {
        notNullKey(key);

        return createCommand(RPOP, new ValueOutput<>(codec), key);
    }

    Command<K, V, V> rpoplpush(K source, K destination) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(source).addKey(destination);
        return createCommand(RPOPLPUSH, new ValueOutput<>(codec), args);
    }

    Command<K, V, Long> rpush(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        return createCommand(RPUSH, new IntegerOutput<>(codec), key, values);
    }

    Command<K, V, Long> rpushx(K key, V... values) {
        notNullKey(key);
        notEmptyValues(values);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);
        return createCommand(RPUSHX, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> sadd(K key, V... members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);

        return createCommand(SADD, new IntegerOutput<>(codec), key, members);
    }

    Command<K, V, String> save() {
        return createCommand(SAVE, new StatusOutput<>(codec));
    }

    Command<K, V, KeyScanCursor<K>> scan() {
        return scan(ScanCursor.INITIAL, null);
    }

    Command<K, V, KeyScanCursor<K>> scan(ScanCursor scanCursor) {
        return scan(scanCursor, null);
    }

    Command<K, V, KeyScanCursor<K>> scan(ScanArgs scanArgs) {
        return scan(ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs) {
        CommandArgs<K, V> args = new CommandArgs<>(codec);

        scanArgs(scanCursor, scanArgs, args);

        KeyScanOutput<K, V> output = new KeyScanOutput<>(codec);
        return createCommand(SCAN, output, args);
    }

    protected void scanArgs(ScanCursor scanCursor, ScanArgs scanArgs, CommandArgs<K, V> args) {
        LettuceAssert.notNull(scanCursor, "ScanCursor " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!scanCursor.isFinished(), "ScanCursor must not be finished");

        args.add(scanCursor.getCursor());

        if (scanArgs != null) {
            scanArgs.build(args);
        }
    }

    Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel) {
        notNull(channel);
        LettuceAssert.notNull(channel, "KeyStreamingChannel " + MUST_NOT_BE_NULL);

        return scanStreaming(channel, ScanCursor.INITIAL, null);
    }

    Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel, ScanCursor scanCursor) {
        notNull(channel);
        LettuceAssert.notNull(channel, "KeyStreamingChannel " + MUST_NOT_BE_NULL);

        return scanStreaming(channel, scanCursor, null);
    }

    Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel, ScanArgs scanArgs) {
        notNull(channel);
        LettuceAssert.notNull(channel, "KeyStreamingChannel " + MUST_NOT_BE_NULL);

        return scanStreaming(channel, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, StreamScanCursor> scanStreaming(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs) {
        notNull(channel);
        LettuceAssert.notNull(channel, "KeyStreamingChannel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        scanArgs(scanCursor, scanArgs, args);

        KeyScanStreamingOutput<K, V> output = new KeyScanStreamingOutput<>(codec, channel);
        return createCommand(SCAN, output, args);
    }

    Command<K, V, Long> scard(K key) {
        notNullKey(key);

        return createCommand(SCARD, new IntegerOutput<>(codec), key);
    }

    Command<K, V, List<Boolean>> scriptExists(String... digests) {
        LettuceAssert.notNull(digests, "Digests " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(digests, "Digests " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(digests, "Digests " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(EXISTS);
        for (String sha : digests) {
            args.add(sha);
        }
        return createCommand(SCRIPT, new BooleanListOutput<>(codec), args);
    }

    Command<K, V, String> scriptFlush() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FLUSH);
        return createCommand(SCRIPT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> scriptKill() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KILL);
        return createCommand(SCRIPT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> scriptLoad(V script) {
        LettuceAssert.notNull(script, "Script " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LOAD).addValue(script);
        return createCommand(SCRIPT, new StatusOutput<>(codec), args);
    }

    Command<K, V, Set<V>> sdiff(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(SDIFF, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, Long> sdiff(ValueStreamingChannel<V> channel, K... keys) {
        notEmpty(keys);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(SDIFF, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> sdiffstore(K destination, K... keys) {
        notEmpty(keys);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).addKeys(keys);
        return createCommand(SDIFFSTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> select(int db) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(db);
        return createCommand(SELECT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> set(K key, V value) {
        notNullKey(key);

        return createCommand(SET, new StatusOutput<>(codec), key, value);
    }

    Command<K, V, String> set(K key, V value, SetArgs setArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);
        setArgs.build(args);
        return createCommand(SET, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> setbit(K key, long offset, int value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(offset).add(value);
        return createCommand(SETBIT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> setex(K key, long seconds, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(seconds).addValue(value);
        return createCommand(SETEX, new StatusOutput<>(codec), args);
    }

    Command<K, V, Boolean> setnx(K key, V value) {
        notNullKey(key);
        return createCommand(SETNX, new BooleanOutput<>(codec), key, value);
    }

    Command<K, V, Long> setrange(K key, long offset, V value) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(offset).addValue(value);
        return createCommand(SETRANGE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> shutdown(boolean save) {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        return createCommand(SHUTDOWN, new StatusOutput<>(codec), save ? args.add(SAVE) : args.add(NOSAVE));
    }

    Command<K, V, Set<V>> sinter(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(SINTER, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, Long> sinter(ValueStreamingChannel<V> channel, K... keys) {
        notEmpty(keys);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(SINTER, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> sinterstore(K destination, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).addKeys(keys);
        return createCommand(SINTERSTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Boolean> sismember(K key, V member) {
        notNullKey(key);
        return createCommand(SISMEMBER, new BooleanOutput<>(codec), key, member);
    }

    Command<K, V, String> slaveof(String host, int port) {
        LettuceAssert.notNull(host, "Host " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(host, "Host " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(host).add(port);
        return createCommand(SLAVEOF, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> slaveofNoOne() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(NO).add(ONE);
        return createCommand(SLAVEOF, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Object>> slowlogGet() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GET);
        return createCommand(SLOWLOG, new NestedMultiOutput<>(codec), args);
    }

    Command<K, V, List<Object>> slowlogGet(int count) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GET).add(count);
        return createCommand(SLOWLOG, new NestedMultiOutput<>(codec), args);
    }

    Command<K, V, Long> slowlogLen() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LEN);
        return createCommand(SLOWLOG, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> slowlogReset() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(RESET);
        return createCommand(SLOWLOG, new StatusOutput<>(codec), args);
    }

    Command<K, V, Set<V>> smembers(K key) {
        notNullKey(key);

        return createCommand(SMEMBERS, new ValueSetOutput<>(codec), key);
    }

    Command<K, V, Long> smembers(ValueStreamingChannel<V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return createCommand(SMEMBERS, new ValueStreamingOutput<>(codec, channel), key);
    }

    Command<K, V, Boolean> smove(K source, K destination, V member) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(source).addKey(destination).addValue(member);
        return createCommand(SMOVE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, List<V>> sort(K key) {
        notNullKey(key);

        return createCommand(SORT, new ValueListOutput<>(codec), key);
    }

    Command<K, V, List<V>> sort(K key, SortArgs sortArgs) {
        notNullKey(key);
        LettuceAssert.notNull(sortArgs, "SortArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        sortArgs.build(args, null);
        return createCommand(SORT, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> sort(ValueStreamingChannel<V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return createCommand(SORT, new ValueStreamingOutput<>(codec, channel), key);
    }

    Command<K, V, Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        notNullKey(key);
        notNull(channel);
        LettuceAssert.notNull(sortArgs, "SortArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        sortArgs.build(args, null);
        return createCommand(SORT, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> sortStore(K key, SortArgs sortArgs, K destination) {
        notNullKey(key);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(sortArgs, "SortArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        sortArgs.build(args, destination);
        return createCommand(SORT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, V> spop(K key) {
        notNullKey(key);

        return createCommand(SPOP, new ValueOutput<>(codec), key);
    }

    Command<K, V, Set<V>> spop(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        return createCommand(SPOP, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, V> srandmember(K key) {
        notNullKey(key);

        return createCommand(SRANDMEMBER, new ValueOutput<>(codec), key);
    }

    Command<K, V, List<V>> srandmember(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        return createCommand(SRANDMEMBER, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> srandmember(ValueStreamingChannel<V> channel, K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        return createCommand(SRANDMEMBER, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> srem(K key, V... members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);

        return createCommand(SREM, new IntegerOutput<>(codec), key, members);
    }

    Command<K, V, ValueScanCursor<V>> sscan(K key) {
        notNullKey(key);

        return sscan(key, ScanCursor.INITIAL, null);
    }

    Command<K, V, ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor) {
        notNullKey(key);

        return sscan(key, scanCursor, null);
    }

    Command<K, V, ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs) {
        notNullKey(key);

        return sscan(key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        ValueScanOutput<K, V> output = new ValueScanOutput<>(codec);
        return createCommand(SSCAN, output, args);
    }

    Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return sscanStreaming(channel, key, ScanCursor.INITIAL, null);
    }

    Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        notNullKey(key);
        notNull(channel);

        return sscanStreaming(channel, key, scanCursor, null);
    }

    Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        return sscanStreaming(channel, key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, StreamScanCursor> sscanStreaming(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        ValueScanStreamingOutput<K, V> output = new ValueScanStreamingOutput<>(codec, channel);
        return createCommand(SSCAN, output, args);
    }

    Command<K, V, Long> strlen(K key) {
        notNullKey(key);

        return createCommand(STRLEN, new IntegerOutput<>(codec), key);
    }

    Command<K, V, Set<V>> sunion(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(SUNION, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, Long> sunion(ValueStreamingChannel<V> channel, K... keys) {
        notEmpty(keys);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(SUNION, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> sunionstore(K destination, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).addKeys(keys);
        return createCommand(SUNIONSTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> swapdb(int db1, int db2) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(db1).add(db2);
        return createCommand(SWAPDB, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> sync() {
        return createCommand(SYNC, new StatusOutput<>(codec));
    }

    Command<K, V, List<V>> time() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        return createCommand(TIME, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> touch(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(TOUCH, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> touch(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(TOUCH, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> ttl(K key) {
        notNullKey(key);

        return createCommand(TTL, new IntegerOutput<>(codec), key);
    }

    Command<K, V, String> type(K key) {
        notNullKey(key);

        return createCommand(TYPE, new StatusOutput<>(codec), key);
    }

    Command<K, V, Long> unlink(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(UNLINK, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> unlink(Iterable<K> keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(UNLINK, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> unwatch() {
        return createCommand(UNWATCH, new StatusOutput<>(codec));
    }

    Command<K, V, Long> wait(int replicas, long timeout) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(replicas).add(timeout);

        return createCommand(WAIT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> watch(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys);
        return createCommand(WATCH, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> zadd(K key, ZAddArgs zAddArgs, double score, V member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (zAddArgs != null) {
            zAddArgs.build(args);
        }
        args.add(score).addValue(member);

        return createCommand(ZADD, new IntegerOutput<>(codec), args);
    }

    @SuppressWarnings("unchecked")
    Command<K, V, Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        notNullKey(key);
        LettuceAssert.notNull(scoresAndValues, "ScoresAndValues " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(scoresAndValues, "ScoresAndValues " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(scoresAndValues, "ScoresAndValues " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (zAddArgs != null) {
            zAddArgs.build(args);
        }

        if (allElementsInstanceOf(scoresAndValues, ScoredValue.class)) {

            for (Object o : scoresAndValues) {
                ScoredValue<V> scoredValue = (ScoredValue<V>) o;

                args.add(scoredValue.getScore());
                args.addValue(scoredValue.getValue());
            }

        } else {
            LettuceAssert.isTrue(scoresAndValues.length % 2 == 0,
                    "ScoresAndValues.length must be a multiple of 2 and contain a "
                            + "sequence of score1, value1, score2, value2, scoreN,valueN");

            for (int i = 0; i < scoresAndValues.length; i += 2) {
                args.add((Double) scoresAndValues[i]);
                args.addValue((V) scoresAndValues[i + 1]);
            }
        }

        return createCommand(ZADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Double> zaddincr(K key, ZAddArgs zAddArgs, double score, V member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (zAddArgs != null) {
            zAddArgs.build(args);
        }

        args.add(INCR);
        args.add(score).addValue(member);

        return createCommand(ZADD, new DoubleOutput<>(codec), args);
    }

    Command<K, V, Long> zcard(K key) {
        notNullKey(key);

        return createCommand(ZCARD, new IntegerOutput<>(codec), key);
    }

    Command<K, V, Long> zcount(K key, double min, double max) {
        return zcount(key, string(min), string(max));
    }

    Command<K, V, Long> zcount(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(min).add(max);
        return createCommand(ZCOUNT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zcount(K key, Range<? extends Number> range) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(min(range)).add(max(range));
        return createCommand(ZCOUNT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Double> zincrby(K key, double amount, K member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(amount).addKey(member);
        return createCommand(ZINCRBY, new DoubleOutput<>(codec), args);
    }

    Command<K, V, Long> zinterstore(K destination, K... keys) {
        notEmpty(keys);

        return zinterstore(destination, new ZStoreArgs(), keys);
    }

    Command<K, V, Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(storeArgs, "ZStoreArgs " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        return createCommand(ZINTERSTORE, new IntegerOutput<>(codec), args);
    }

    RedisCommand<K, V, Long> zlexcount(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min).add(max);
        return createCommand(ZLEXCOUNT, new IntegerOutput<>(codec), args);
    }

    RedisCommand<K, V, Long> zlexcount(K key, Range<? extends V> range) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(minValue(range)).add(maxValue(range));
        return createCommand(ZLEXCOUNT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrange(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZRANGE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZRANGE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZRANGE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZRANGE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    RedisCommand<K, V, List<V>> zrangebylex(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min).add(max);
        return createCommand(ZRANGEBYLEX, new ValueListOutput<>(codec), args);
    }

    RedisCommand<K, V, List<V>> zrangebylex(K key, String min, String max, long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min).add(max), Limit.create(offset, count));
        return createCommand(ZRANGEBYLEX, new ValueListOutput<>(codec), args);
    }

    RedisCommand<K, V, List<V>> zrangebylex(K key, Range<? extends V> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(minValue(range)).add(maxValue(range)), limit);
        return createCommand(ZRANGEBYLEX, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrangebyscore(K key, double min, double max) {
        return zrangebyscore(key, string(min), string(max));
    }

    Command<K, V, List<V>> zrangebyscore(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(min).add(max);
        return createCommand(ZRANGEBYSCORE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrangebyscore(K key, double min, double max, long offset, long count) {
        return zrangebyscore(key, string(min), string(max), offset, count);
    }

    Command<K, V, List<V>> zrangebyscore(K key, String min, String max, long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min).add(max).add(LIMIT).add(offset).add(count);
        return createCommand(ZRANGEBYSCORE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min(range)).add(max(range));

        if (limit.isLimited()) {
            args.add(LIMIT).add(limit.getOffset()).add(limit.getCount());
        }
        return createCommand(ZRANGEBYSCORE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max) {
        return zrangebyscore(channel, key, string(min), string(max));
    }

    Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);
        LettuceAssert.notNull(channel, "ScoredValueStreamingChannel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(min).add(max);
        return createCommand(ZRANGEBYSCORE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset,
            long count) {
        return zrangebyscore(channel, key, string(min), string(max), offset, count);
    }

    Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset,
            long count) {
        notNullKey(key);
        notNullMinMax(min, max);
        LettuceAssert.notNull(channel, "ScoredValueStreamingChannel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min).add(max), Limit.create(offset, count));
        return createCommand(ZRANGEBYSCORE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);
        LettuceAssert.notNull(channel, "ScoredValueStreamingChannel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min(range)).add(max(range)), limit);
        return createCommand(ZRANGEBYSCORE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max) {
        return zrangebyscoreWithScores(key, string(min), string(max));
    }

    Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES);
        return createCommand(ZRANGEBYSCORE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count) {
        return zrangebyscoreWithScores(key, string(min), string(max), offset, count);
    }

    Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min).add(max).add(WITHSCORES), Limit.create(offset, count));
        return createCommand(ZRANGEBYSCORE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min(range)).add(max(range)).add(WITHSCORES), limit);
        return createCommand(ZRANGEBYSCORE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max) {
        return zrangebyscoreWithScores(channel, key, string(min), string(max));
    }

    Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min).add(max).add(WITHSCORES);
        return createCommand(ZRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count) {
        return zrangebyscoreWithScores(channel, key, string(min), string(max), offset, count);
    }

    Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min).add(max).add(WITHSCORES), Limit.create(offset, count));
        return createCommand(ZRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min(range)).add(max(range)).add(WITHSCORES), limit);
        return createCommand(ZRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrank(K key, V member) {
        notNullKey(key);

        return createCommand(ZRANK, new IntegerOutput<>(codec), key, member);
    }

    Command<K, V, Long> zrem(K key, V... members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);

        return createCommand(ZREM, new IntegerOutput<>(codec), key, members);
    }

    RedisCommand<K, V, Long> zremrangebylex(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(min).add(max);
        return createCommand(ZREMRANGEBYLEX, new IntegerOutput<>(codec), args);
    }

    RedisCommand<K, V, Long> zremrangebylex(K key, Range<? extends V> range) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(minValue(range)).add(maxValue(range));
        return createCommand(ZREMRANGEBYLEX, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zremrangebyrank(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZREMRANGEBYRANK, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zremrangebyscore(K key, double min, double max) {
        return zremrangebyscore(key, string(min), string(max));
    }

    Command<K, V, Long> zremrangebyscore(K key, String min, String max) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(min).add(max);
        return createCommand(ZREMRANGEBYSCORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zremrangebyscore(K key, Range<? extends Number> range) {
        notNullKey(key);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(min(range)).add(max(range));
        return createCommand(ZREMRANGEBYSCORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrevrange(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZREVRANGE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(start).add(stop);
        return createCommand(ZREVRANGE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZREVRANGE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop) {
        notNullKey(key);
        LettuceAssert.notNull(channel, "ValueStreamingChannel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(start).add(stop).add(WITHSCORES);
        return createCommand(ZREVRANGE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, List<V>> zrevrangebylex(K key, Range<? extends V> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(maxValue(range)).add(minValue(range)), limit);
        return createCommand(ZREVRANGEBYLEX, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrevrangebyscore(K key, double max, double min) {
        return zrevrangebyscore(key, string(max), string(min));
    }

    Command<K, V, List<V>> zrevrangebyscore(K key, String max, String min) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(max).add(min);
        return createCommand(ZREVRANGEBYSCORE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count) {
        return zrevrangebyscore(key, string(max), string(min), offset, count);
    }

    Command<K, V, List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max).add(min), Limit.create(offset, count));
        return createCommand(ZREVRANGEBYSCORE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrevrangebyscore(K key, Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max(range)).add(min(range)), limit);
        return createCommand(ZREVRANGEBYSCORE, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min) {
        return zrevrangebyscore(channel, key, string(max), string(min));
    }

    Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(max).add(min);
        return createCommand(ZREVRANGEBYSCORE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count) {
        return zrevrangebyscore(channel, key, string(max), string(min), offset, count);
    }

    Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max).add(min), Limit.create(offset, count));
        return createCommand(ZREVRANGEBYSCORE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max(range)).add(min(range)), limit);
        return createCommand(ZREVRANGEBYSCORE, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min) {
        return zrevrangebyscoreWithScores(key, string(max), string(min));
    }

    Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count) {
        return zrevrangebyscoreWithScores(key, string(max), string(min), offset, count);
    }

    Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max).add(min).add(WITHSCORES), Limit.create(offset, count));
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max(range)).add(min(range)).add(WITHSCORES), limit);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min) {
        return zrevrangebyscoreWithScores(channel, key, string(max), string(min));
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key).add(max).add(min).add(WITHSCORES);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max,
            double min, long offset, long count) {
        notNullKey(key);
        LettuceAssert.notNull(min, "Min " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(max, "Max " + MUST_NOT_BE_NULL);
        notNull(channel);
        return zrevrangebyscoreWithScores(channel, key, string(max), string(min), offset, count);
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max,
            String min, long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max).add(min).add(WITHSCORES), Limit.create(offset, count));
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key,
            Range<? extends Number> range, Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max(range)).add(min(range)).add(WITHSCORES), limit);
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrevrank(K key, V member) {
        notNullKey(key);

        return createCommand(ZREVRANK, new IntegerOutput<>(codec), key, member);
    }

    Command<K, V, ScoredValueScanCursor<V>> zscan(K key) {
        notNullKey(key);

        return zscan(key, ScanCursor.INITIAL, null);
    }

    Command<K, V, ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor) {
        notNullKey(key);

        return zscan(key, scanCursor, null);
    }

    Command<K, V, ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs) {
        notNullKey(key);

        return zscan(key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        ScoredValueScanOutput<K, V> output = new ScoredValueScanOutput<>(codec);
        return createCommand(ZSCAN, output, args);
    }

    Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return zscanStreaming(channel, key, ScanCursor.INITIAL, null);
    }

    Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor) {
        notNullKey(key);
        notNull(channel);

        return zscanStreaming(channel, key, scanCursor, null);
    }

    Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        return zscanStreaming(channel, key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, StreamScanCursor> zscanStreaming(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        ScoredValueScanStreamingOutput<K, V> output = new ScoredValueScanStreamingOutput<>(codec, channel);
        return createCommand(ZSCAN, output, args);
    }

    Command<K, V, Double> zscore(K key, V member) {
        notNullKey(key);

        return createCommand(ZSCORE, new DoubleOutput<>(codec), key, member);
    }

    Command<K, V, Long> zunionstore(K destination, K... keys) {
        notEmpty(keys);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        return zunionstore(destination, new ZStoreArgs(), keys);
    }

    Command<K, V, Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(destination).add(keys.length).addKeys(keys);
        storeArgs.build(args);
        return createCommand(ZUNIONSTORE, new IntegerOutput<>(codec), args);
    }

    private boolean allElementsInstanceOf(Object[] objects, Class<?> expectedAssignableType) {

        for (Object object : objects) {
            if (!expectedAssignableType.isAssignableFrom(object.getClass())) {
                return false;
            }
        }

        return true;
    }

    private byte[] maxValue(Range<? extends V> range) {

        Range.Boundary<? extends V> upper = range.getUpper();

        if (upper.getValue() == null) {
            return PLUS_BYTES;
        }

        ByteBuffer encoded = codec.encodeValue(upper.getValue());
        ByteBuffer allocated = ByteBuffer.allocate(encoded.remaining() + 1);
        allocated.put(upper.isIncluding() ? (byte) '[' : (byte) '(').put(encoded);

        return allocated.array();
    }

    private byte[] minValue(Range<? extends V> range) {

        Range.Boundary<? extends V> lower = range.getLower();

        if (lower.getValue() == null) {
            return MINUS_BYTES;
        }

        ByteBuffer encoded = codec.encodeValue(lower.getValue());
        ByteBuffer allocated = ByteBuffer.allocate(encoded.remaining() + 1);
        allocated.put(lower.isIncluding() ? (byte) '[' : (byte) '(').put(encoded);

        return allocated.array();
    }

    static void notNull(ScoredValueStreamingChannel<?> channel) {
        LettuceAssert.notNull(channel, "ScoredValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    static void notNull(KeyStreamingChannel<?> channel) {
        LettuceAssert.notNull(channel, "KeyValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    static void notNull(ValueStreamingChannel<?> channel) {
        LettuceAssert.notNull(channel, "ValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    static void notNull(KeyValueStreamingChannel<?, ?> channel) {
        LettuceAssert.notNull(channel, "KeyValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    static void notNullMinMax(String min, String max) {
        LettuceAssert.notNull(min, "Min " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(max, "Max " + MUST_NOT_BE_NULL);
    }

    private static void addLimit(CommandArgs<?, ?> args, Limit limit) {

        if (limit.isLimited()) {
            args.add(LIMIT).add(limit.getOffset()).add(limit.getCount());
        }
    }

    private static void assertNodeId(String nodeId) {
        LettuceAssert.notNull(nodeId, "NodeId " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(nodeId, "NodeId " + MUST_NOT_BE_EMPTY);
    }

    private static String max(Range<? extends Number> range) {

        Range.Boundary<? extends Number> upper = range.getUpper();

        if (upper.getValue() == null || upper.getValue() instanceof Double
                && upper.getValue().doubleValue() == Double.POSITIVE_INFINITY) {
            return "+inf";
        }

        if (!upper.isIncluding()) {
            return "(" + upper.getValue();
        }

        return upper.getValue().toString();
    }

    private static String min(Range<? extends Number> range) {

        Range.Boundary<? extends Number> lower = range.getLower();

        if (lower.getValue() == null || lower.getValue() instanceof Double
                && lower.getValue().doubleValue() == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }

        if (!lower.isIncluding()) {
            return "(" + lower.getValue();
        }

        return lower.getValue().toString();
    }

    private static void notEmpty(Object[] keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(keys, "Keys " + MUST_NOT_BE_EMPTY);
    }

    private static void notEmptySlots(int[] slots) {
        LettuceAssert.notNull(slots, "Slots " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(slots, "Slots " + MUST_NOT_BE_EMPTY);
    }

    private static void notEmptyValues(Object[] values) {
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(values, "Values " + MUST_NOT_BE_EMPTY);
    }

    private static void notNullKey(Object key) {
        LettuceAssert.notNull(key, "Key " + MUST_NOT_BE_NULL);
    }

    private static void notNullLimit(Limit limit) {
        LettuceAssert.notNull(limit, "Limit " + MUST_NOT_BE_NULL);
    }

    private static void notNullRange(Range<?> range) {
        LettuceAssert.notNull(range, "Range " + MUST_NOT_BE_NULL);
    }
}
