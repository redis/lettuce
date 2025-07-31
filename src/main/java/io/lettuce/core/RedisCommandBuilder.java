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
package io.lettuce.core;

import io.lettuce.core.Range.Boundary;
import io.lettuce.core.XReadArgs.StreamOffset;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.stream.ClaimedMessages;
import io.lettuce.core.models.stream.PendingMessage;
import io.lettuce.core.models.stream.PendingMessages;
import io.lettuce.core.models.stream.StreamEntryDeletionResult;
import io.lettuce.core.output.*;
import io.lettuce.core.output.StreamEntryDeletionResultListOutput;
import io.lettuce.core.protocol.BaseRedisCommandBuilder;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.lettuce.core.internal.LettuceStrings.string;
import static io.lettuce.core.protocol.CommandKeyword.*;
import static io.lettuce.core.protocol.CommandType.*;
import static io.lettuce.core.protocol.CommandType.COPY;
import static io.lettuce.core.protocol.CommandType.SAVE;

/**
 * @param <K>
 * @param <V>
 * @author Mark Paluch
 * @author Zhang Jessey
 * @author Tugdual Grall
 * @author dengliming
 * @author Mikhael Sokolov
 * @author Tihomir Mateev
 * @author Ali Takavci
 * @author Seonghwan Lee
 */
@SuppressWarnings({ "unchecked", "varargs" })
class RedisCommandBuilder<K, V> extends BaseRedisCommandBuilder<K, V> {

    RedisCommandBuilder(RedisCodec<K, V> codec) {
        super(codec);
    }

    Command<K, V, Set<AclCategory>> aclCat() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(CAT);
        return createCommand(ACL, new EnumSetOutput<>(codec, AclCategory.class, String::toUpperCase, it -> null), args);
    }

    Command<K, V, Set<CommandType>> aclCat(AclCategory category) {
        LettuceAssert.notNull(category, "Category " + MUST_NOT_BE_NULL);
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(CAT).add(category.name().toLowerCase());
        return createCommand(ACL, new EnumSetOutput<>(codec, CommandType.class, String::toUpperCase, it -> null), args);
    }

    Command<K, V, Long> aclDeluser(String... usernames) {
        notEmpty(usernames);
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(DELUSER);
        for (String username : usernames) {
            args.add(username);
        }
        return createCommand(ACL, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> aclDryRun(String username, String command, String... commandArgs) {
        LettuceAssert.notNull(username, "username " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(command, "command " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(DRYRUN).add(username).add(command);

        for (String commandArg : commandArgs) {
            args.add(commandArg);
        }
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> aclDryRun(String username, RedisCommand<K, V, ?> command) {
        LettuceAssert.notNull(username, "username " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(command, "command " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(DRYRUN).add(username).add(command.getType()).addAll(command.getArgs());

        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> aclGenpass() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(GENPASS);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> aclGenpass(int bits) {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(GENPASS).add(bits);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Object>> aclGetuser(String username) {
        LettuceAssert.notNull(username, "Username " + MUST_NOT_BE_NULL);
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(GETUSER).add(username);
        return createCommand(ACL, new NestedMultiOutput<>(codec), args);
    }

    Command<K, V, List<String>> aclList() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(LIST);
        return createCommand(ACL, new StringListOutput<>(codec), args);
    }

    Command<K, V, String> aclLoad() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(LOAD);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Map<String, Object>>> aclLog() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(LOG);
        return new Command(ACL, new ListOfGenericMapsOutput<>(StringCodec.ASCII), args);
    }

    Command<K, V, List<Map<String, Object>>> aclLog(int count) {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(LOG).add(count);
        return new Command(ACL, new ListOfGenericMapsOutput<>(StringCodec.ASCII), args);
    }

    Command<K, V, String> aclLogReset() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(LOG).add(RESET);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> aclSave() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(CommandKeyword.SAVE);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> aclSetuser(String username, AclSetuserArgs setuserArgs) {
        notNullKey(username);
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(SETUSER).add(username);
        setuserArgs.build(args);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<String>> aclUsers() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(USERS);
        return createCommand(ACL, new StringListOutput<>(codec), args);
    }

    Command<K, V, String> aclWhoami() {
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(WHOAMI);
        return createCommand(ACL, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> append(K key, V value) {
        notNullKey(key);

        return createCommand(APPEND, new IntegerOutput<>(codec), key, value);
    }

    Command<K, V, String> asking() {

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        return createCommand(ASKING, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> auth(CharSequence password) {
        LettuceAssert.notNull(password, "Password " + MUST_NOT_BE_NULL);

        char[] chars = new char[password.length()];
        for (int i = 0; i < password.length(); i++) {
            chars[i] = password.charAt(i);
        }
        return auth(chars);
    }

    Command<K, V, String> auth(char[] password) {
        LettuceAssert.notNull(password, "Password " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(password);
        return createCommand(AUTH, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> auth(String username, CharSequence password) {
        LettuceAssert.notNull(username, "Username " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!username.isEmpty(), "Username " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(password, "Password " + MUST_NOT_BE_NULL);

        char[] chars = new char[password.length()];
        for (int i = 0; i < password.length(); i++) {
            chars[i] = password.charAt(i);
        }
        return auth(username, chars);
    }

    Command<K, V, String> auth(String username, char[] password) {
        LettuceAssert.notNull(username, "Username " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!username.isEmpty(), "Username " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(password, "Password " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(username).add(password);
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

    Command<K, V, Long> bitopDiff(K destination, K sourceKey, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(sourceKey, "Source key " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(DIFF).addKey(destination).addKey(sourceKey).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitopDiff1(K destination, K sourceKey, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(sourceKey, "Source key " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(DIFF1).addKey(destination).addKey(sourceKey).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitopAndor(K destination, K sourceKey, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(sourceKey, "Source key " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(ANDOR).addKey(destination).addKey(sourceKey).addKeys(keys);
        return createCommand(BITOP, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> bitopOne(K destination, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(ONE).addKey(destination).addKeys(keys);
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

    Command<K, V, V> blmove(K source, K destination, LMoveArgs lMoveArgs, long timeout) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(lMoveArgs, "LMoveArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(source).addKey(destination);
        lMoveArgs.build(args);
        args.add(timeout);
        return createCommand(BLMOVE, new ValueOutput<>(codec), args);
    }

    Command<K, V, V> blmove(K source, K destination, LMoveArgs lMoveArgs, double timeout) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(lMoveArgs, "LMoveArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(source).addKey(destination);
        lMoveArgs.build(args);
        args.add(timeout);
        return createCommand(BLMOVE, new ValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, List<V>>> blmpop(long timeout, LMPopArgs lmPopArgs, K... keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(lmPopArgs, "LMPopArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(timeout).add(keys.length).addKeys(keys);
        lmPopArgs.build(args);

        return createCommand(BLMPOP, new KeyValueValueListOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, List<V>>> blmpop(double timeout, LMPopArgs lmPopArgs, K... keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(lmPopArgs, "LMPopArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(timeout).add(keys.length).addKeys(keys);
        lmPopArgs.build(args);

        return createCommand(BLMPOP, new KeyValueValueListOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> blpop(long timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);
        return createCommand(BLPOP, new KeyValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> blpop(double timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);
        return createCommand(BLPOP, new KeyValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> brpop(long timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);
        return createCommand(BRPOP, new KeyValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> brpop(double timeout, K... keys) {
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

    Command<K, V, V> brpoplpush(double timeout, K source, K destination) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(source).addKey(destination).add(timeout);
        return createCommand(BRPOPLPUSH, new ValueOutput<>(codec), args);
    }

    Command<K, V, String> clientCaching(boolean enabled) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CACHING).add(enabled ? YES : NO);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, K> clientGetname() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GETNAME);
        return createCommand(CLIENT, new KeyOutput<>(codec), args);
    }

    Command<K, V, Long> clientGetredir() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GETREDIR);
        return createCommand(CLIENT, new IntegerOutput<>(codec), args);
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

    Command<K, V, String> clientList(ClientListArgs clientListArgs) {
        LettuceAssert.notNull(clientListArgs, "ClientListArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LIST);
        clientListArgs.build(args);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clientInfo() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandKeyword.INFO);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clientNoEvict(boolean on) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add("NO-EVICT").add(on ? ON : OFF);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, Long> clientId() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(ID);
        return createCommand(CLIENT, new IntegerOutput<>(codec), args);
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

    Command<K, V, String> clientSetinfo(String key, String value) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SETINFO).add(key).add(value);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clientTracking(TrackingArgs trackingArgs) {
        LettuceAssert.notNull(trackingArgs, "TrackingArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(TRACKING);
        trackingArgs.build(args);
        return createCommand(CLIENT, new StatusOutput<>(codec), args);
    }

    Command<K, V, TrackingInfo> clientTrackinginfo() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(TRACKINGINFO);

        return new Command<>(CLIENT, new ComplexOutput<>(codec, TrackingInfoParser.INSTANCE), args);
    }

    Command<K, V, Long> clientUnblock(long id, UnblockType type) {
        LettuceAssert.notNull(type, "UnblockType " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(UNBLOCK).add(id).add(type);
        return createCommand(CLIENT, new IntegerOutput<>(codec), args);
    }

    Command<K, V, String> clusterAddslots(int[] slots) {
        notEmptySlots(slots);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(ADDSLOTS);

        for (int slot : slots) {
            args.add(slot);
        }
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterAddSlotsRange(Range<Integer>... ranges) {
        notEmptyRanges(ranges);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(ADDSLOTSRANGE);

        for (Range<Integer> range : ranges) {
            args.add(range.getLower().getValue());
            args.add(range.getUpper().getValue());
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

    Command<K, V, String> clusterDelSlotsRange(Range<Integer>... ranges) {
        notEmptyRanges(ranges);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(DELSLOTSRANGE);

        for (Range<Integer> range : ranges) {
            args.add(range.getLower().getValue());
            args.add(range.getUpper().getValue());
        }
        return createCommand(CLUSTER, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> clusterFailover(boolean force) {
        return clusterFailover(force, false);
    }

    Command<K, V, String> clusterFailover(boolean force, boolean takeOver) {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FAILOVER);
        if (force) {
            args.add(FORCE);
        } else if (takeOver) {
            args.add(TAKEOVER);
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
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CommandType.INFO);

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

    Command<K, V, String> clusterMyShardId() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(MYSHARDID);

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

    Command<K, V, List<String>> clusterReplicas(String nodeId) {
        assertNodeId(nodeId);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(REPLICAS).add(nodeId);
        return createCommand(CLUSTER, new StringListOutput<>(codec), args);
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

    Command<K, V, List<Object>> clusterShards() {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SHARDS);
        return createCommand(CLUSTER, new ArrayOutput<>(codec), args);
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
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        return Command.class.cast(new Command(COMMAND, new ArrayOutput<>(StringCodec.UTF8), args));
    }

    Command<K, V, Long> commandCount() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(COUNT);
        return createCommand(COMMAND, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Object>> commandInfo(String... commands) {
        LettuceAssert.notNull(commands, "Commands " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(commands, "Commands " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(commands, "Commands " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        args.add(CommandKeyword.INFO);

        for (String command : commands) {
            args.add(command);
        }

        return Command.class.cast(new Command<>(COMMAND, new ArrayOutput<>(StringCodec.UTF8), args));
    }

    Command<K, V, Map<String, String>> configGet(String parameter) {
        LettuceAssert.notNull(parameter, "Parameter " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(parameter, "Parameter " + MUST_NOT_BE_EMPTY);

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(GET).add(parameter);
        return Command.class.cast(new Command<>(CONFIG, new MapOutput<>(StringCodec.UTF8), args));
    }

    Command<K, V, Map<String, String>> configGet(String... parameters) {
        LettuceAssert.notNull(parameters, "Parameters " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(parameters, "Parameters " + MUST_NOT_BE_EMPTY);

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(GET);
        for (String parameter : parameters) {
            args.add(parameter);
        }
        return Command.class.cast(new Command<>(CONFIG, new MapOutput<>(StringCodec.UTF8), args));
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

    Command<K, V, String> configSet(Map<String, String> configValues) {
        LettuceAssert.notNull(configValues, "ConfigValues " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!configValues.isEmpty(), "ConfigValues " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SET);
        configValues.forEach((parameter, value) -> {
            args.add(parameter);
            args.add(value);
        });
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

    <T> Command<K, V, T> eval(byte[] script, ScriptOutputType type, K[] keys, V... values) {
        return eval(script, type, false, keys, values);
    }

    <T> Command<K, V, T> eval(byte[] script, ScriptOutputType type, boolean readonly, K[] keys, V... values) {
        LettuceAssert.notNull(script, "Script " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(script).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);

        return createCommand(readonly ? EVAL_RO : EVAL, output, args);
    }

    <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values) {
        return evalsha(digest, type, false, keys, values);
    }

    <T> Command<K, V, T> evalsha(String digest, ScriptOutputType type, boolean readonly, K[] keys, V... values) {
        LettuceAssert.notNull(digest, "Digest " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(digest, "Digest " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(digest).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);

        return createCommand(readonly ? EVALSHA_RO : EVALSHA, output, args);
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

    Command<K, V, Boolean> expire(K key, long seconds, ExpireArgs expireArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(seconds);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        return createCommand(EXPIRE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hexpire(K key, long seconds, ExpireArgs expireArgs, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(seconds);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HEXPIRE, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hexpireat(K key, long seconds, ExpireArgs expireArgs, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(seconds);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HEXPIREAT, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> httl(K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HTTL, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hpexpire(K key, long milliseconds, ExpireArgs expireArgs, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(milliseconds);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HPEXPIRE, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hpexpireat(K key, long timestamp, ExpireArgs expireArgs, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HPEXPIREAT, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hpexpiretime(K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HPEXPIRETIME, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hpttl(K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HPTTL, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, Boolean> expireat(K key, long timestamp, ExpireArgs expireArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        return createCommand(EXPIREAT, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Long> expiretime(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(EXPIRETIME, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Long>> hexpiretime(K key, K... fields) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HEXPIRETIME, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, String> flushall() {
        return createCommand(FLUSHALL, new StatusOutput<>(codec));
    }

    Command<K, V, String> flushall(FlushMode flushMode) {
        LettuceAssert.notNull(flushMode, "FlushMode " + MUST_NOT_BE_NULL);

        return createCommand(FLUSHALL, new StatusOutput<>(codec), new CommandArgs<>(codec).add(flushMode));
    }

    Command<K, V, String> flushdb() {
        return createCommand(FLUSHDB, new StatusOutput<>(codec));
    }

    Command<K, V, String> flushdb(FlushMode flushMode) {
        LettuceAssert.notNull(flushMode, "FlushMode " + MUST_NOT_BE_NULL);

        return createCommand(FLUSHDB, new StatusOutput<>(codec), new CommandArgs<>(codec).add(flushMode));
    }

    <T> Command<K, V, T> fcall(String function, ScriptOutputType type, boolean readonly, K[] keys, V... values) {
        LettuceAssert.notEmpty(function, "Function " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(type, "ScriptOutputType " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(function).add(keys.length).addKeys(keys).addValues(values);
        CommandOutput<K, V, T> output = newScriptOutput(codec, type);

        return createCommand(readonly ? FCALL_RO : FCALL, output, args);
    }

    Command<K, V, String> functionLoad(byte[] functionCode, boolean replace) {
        LettuceAssert.notNull(functionCode, "Function code " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LOAD);
        if (replace) {
            args.add(REPLACE);
        }
        args.add(functionCode);

        return createCommand(FUNCTION, new StatusOutput<>(codec), args);
    }

    Command<K, V, byte[]> functionDump() {
        return createCommand(FUNCTION, new ByteArrayOutput<>(codec), new CommandArgs<>(codec).add(DUMP));
    }

    Command<K, V, String> functionRestore(byte dump[], FunctionRestoreMode mode) {

        LettuceAssert.notNull(dump, "Function dump " + MUST_NOT_BE_NULL);
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(RESTORE).add(dump);

        if (mode != null) {
            args.add(mode);
        }

        return createCommand(FUNCTION, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> functionFlush(FlushMode mode) {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FLUSH);

        if (mode != null) {
            args.add(mode);
        }

        return createCommand(FUNCTION, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> functionKill() {
        return createCommand(FUNCTION, new StatusOutput<>(codec), new CommandArgs<>(codec).add(KILL));
    }

    Command<K, V, List<Map<String, Object>>> functionList(String pattern) {

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LIST);

        if (pattern != null) {
            args.add("LIBRARYNAME").add(pattern);
        }

        return createCommand(FUNCTION, (CommandOutput) new ObjectOutput<>(StringCodec.UTF8), args);
    }

    Command<K, V, Long> geoadd(K key, double longitude, double latitude, V member, GeoAddArgs geoArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (geoArgs != null) {
            geoArgs.build(args);
        }

        args.add(longitude).add(latitude).addValue(member);
        return createCommand(GEOADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> geoadd(K key, Object[] lngLatMember, GeoAddArgs geoArgs) {

        notNullKey(key);
        LettuceAssert.notNull(lngLatMember, "LngLatMember " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(lngLatMember, "LngLatMember " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(lngLatMember, "LngLatMember " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
        LettuceAssert.isTrue(lngLatMember.length % 3 == 0, "LngLatMember.length must be a multiple of 3 and contain a "
                + "sequence of longitude1, latitude1, member1, longitude2, latitude2, member2, ... longitudeN, latitudeN, memberN");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (geoArgs != null) {
            geoArgs.build(args);
        }

        for (int i = 0; i < lngLatMember.length; i += 3) {
            args.add((Double) lngLatMember[i]);
            args.add((Double) lngLatMember[i + 1]);
            args.addValue((V) lngLatMember[i + 2]);
        }

        return createCommand(GEOADD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> geoadd(K key, GeoValue<V>[] values, GeoAddArgs geoArgs) {

        notNullKey(key);
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(values, "Values " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(values, "Values " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (geoArgs != null) {
            geoArgs.build(args);
        }

        for (GeoValue<V> value : values) {
            args.add(value.getCoordinates().getX().doubleValue());
            args.add(value.getCoordinates().getY().doubleValue());
            args.addValue(value.getValue());
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

        return createCommand(commandType,
                new GeoWithinListOutput<>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(), geoArgs.isWithCoordinates()),
                args);
    }

    Command<K, V, Long> georadius(K key, double longitude, double latitude, double distance, String unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {

        notNullKey(key);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(geoRadiusStoreArgs, "GeoRadiusStoreArgs " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(geoRadiusStoreArgs.getStoreKey() != null || geoRadiusStoreArgs.getStoreDistKey() != null,
                "At least STORE key or STOREDIST key is required");

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

        return createCommand(commandType,
                new GeoWithinListOutput<>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(), geoArgs.isWithCoordinates()),
                args);
    }

    Command<K, V, Long> georadiusbymember(K key, V member, double distance, String unit,
            GeoRadiusStoreArgs<K> geoRadiusStoreArgs) {

        notNullKey(key);
        LettuceAssert.notNull(geoRadiusStoreArgs, "GeoRadiusStoreArgs " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(unit, "Unit " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(unit, "Unit " + MUST_NOT_BE_EMPTY);
        LettuceAssert.isTrue(geoRadiusStoreArgs.getStoreKey() != null || geoRadiusStoreArgs.getStoreDistKey() != null,
                "At least STORE key or STOREDIST key is required");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(member).add(distance).add(unit);
        geoRadiusStoreArgs.build(args);

        return createCommand(GEORADIUSBYMEMBER, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Set<V>> geosearch(K key, GeoSearch.GeoRef<K> reference, GeoSearch.GeoPredicate predicate) {
        notNullKey(key);
        LettuceAssert.notNull(reference, "GeoRef " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(predicate, "GeoPredicate " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        reference.build(args);
        predicate.build(args);

        return createCommand(GEOSEARCH, new ValueSetOutput<>(codec), args);
    }

    Command<K, V, List<GeoWithin<V>>> geosearch(K key, GeoSearch.GeoRef<K> reference, GeoSearch.GeoPredicate predicate,
            GeoArgs geoArgs) {
        notNullKey(key);
        LettuceAssert.notNull(reference, "GeoRef " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(predicate, "GeoPredicate " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        reference.build(args);
        predicate.build(args);
        geoArgs.build(args);

        return createCommand(GEOSEARCH,
                new GeoWithinListOutput<>(codec, geoArgs.isWithDistance(), geoArgs.isWithHash(), geoArgs.isWithCoordinates()),
                args);
    }

    Command<K, V, Long> geosearchstore(K destination, K key, GeoSearch.GeoRef<K> reference, GeoSearch.GeoPredicate predicate,
            GeoArgs geoArgs, boolean storeDist) {
        notNullKey(key);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(key, "Key " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(reference, "GeoRef " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(predicate, "GeoPredicate " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).addKey(key);

        reference.build(args);
        predicate.build(args);
        geoArgs.build(args);

        if (storeDist) {
            args.add("STOREDIST");
        }

        return createCommand(GEOSEARCHSTORE, new IntegerOutput<>(codec), args);
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

    Command<K, V, V> getdel(K key) {
        notNullKey(key);

        return createCommand(GETDEL, new ValueOutput<>(codec), key);
    }

    Command<K, V, V> getex(K key, GetExArgs getExArgs) {
        notNullKey(key);
        LettuceAssert.notNull(getExArgs, "GetExArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        getExArgs.build(args);

        return createCommand(GETEX, new ValueOutput<>(codec), args);
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
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HDEL, new IntegerOutput<>(codec), args);
    }

    Command<String, String, Map<String, Object>> hello(int protocolVersion, String user, char[] password, String name) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.ASCII).add(protocolVersion);

        if (user != null && password != null) {
            args.add(AUTH).add(user).add(password);
        }

        if (name != null) {
            args.add(SETNAME).add(name);
        }

        return new Command<>(HELLO, new GenericMapOutput<>(StringCodec.ASCII), args);
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

    Command<K, V, List<KeyValue<K, V>>> hgetallKeyValue(K key) {
        notNullKey(key);

        return createCommand(HGETALL, new KeyValueListOutput<>(codec), key);
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
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new ValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> hmget(KeyValueStreamingChannel<K, V> channel, K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKeys(fields);
        return createCommand(HMGET, new KeyValueStreamingOutput<>(codec, channel, Arrays.asList(fields)), args);
    }

    Command<K, V, List<KeyValue<K, V>>> hmgetKeyValue(K key, K... fields) {
        keyAndFieldsProvided(key, fields);

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

    Command<K, V, K> hrandfield(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(HRANDFIELD, new KeyOutput<>(codec), args);
    }

    Command<K, V, List<K>> hrandfield(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count);
        return createCommand(HRANDFIELD, new KeyListOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, V>> hrandfieldWithvalues(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(1).add(WITHVALUES);
        return createCommand(HRANDFIELD, new KeyValueOutput<>(codec), args);
    }

    Command<K, V, List<KeyValue<K, V>>> hrandfieldWithvalues(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(count).add(WITHVALUES);
        return createCommand(HRANDFIELD, new KeyValueListOutput<>(codec), args);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key) {
        notNullKey(key);

        return hscan(key, ScanCursor.INITIAL, null);
    }

    Command<K, V, KeyScanCursor<K>> hscanNovalues(K key) {
        notNullKey(key);

        return hscanNovalues(key, ScanCursor.INITIAL, null);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor) {
        notNullKey(key);

        return hscan(key, scanCursor, null);
    }

    Command<K, V, KeyScanCursor<K>> hscanNovalues(K key, ScanCursor scanCursor) {
        notNullKey(key);

        return hscanNovalues(key, scanCursor, null);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs) {
        notNullKey(key);

        return hscan(key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, KeyScanCursor<K>> hscanNovalues(K key, ScanArgs scanArgs) {
        notNullKey(key);

        return hscanNovalues(key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        MapScanOutput<K, V> output = new MapScanOutput<>(codec);
        return createCommand(HSCAN, output, args);
    }

    Command<K, V, KeyScanCursor<K>> hscanNovalues(K key, ScanCursor scanCursor, ScanArgs scanArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(key);

        scanArgs(scanCursor, scanArgs, args);

        args.add(NOVALUES);

        KeyScanOutput<K, V> output = new KeyScanOutput<>(codec);
        return createCommand(HSCAN, output, args);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return hscanStreaming(channel, key, ScanCursor.INITIAL, null);
    }

    Command<K, V, StreamScanCursor> hscanNoValuesStreaming(KeyStreamingChannel<K> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return hscanNoValuesStreaming(channel, key, ScanCursor.INITIAL, null);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor) {
        notNullKey(key);
        notNull(channel);

        return hscanStreaming(channel, key, scanCursor, null);
    }

    Command<K, V, StreamScanCursor> hscanNoValuesStreaming(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor) {
        notNullKey(key);
        notNull(channel);

        return hscanNoValuesStreaming(channel, key, scanCursor, null);
    }

    Command<K, V, StreamScanCursor> hscanStreaming(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        return hscanStreaming(channel, key, ScanCursor.INITIAL, scanArgs);
    }

    Command<K, V, StreamScanCursor> hscanNoValuesStreaming(KeyStreamingChannel<K> channel, K key, ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        return hscanNoValuesStreaming(channel, key, ScanCursor.INITIAL, scanArgs);
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

    Command<K, V, StreamScanCursor> hscanNoValuesStreaming(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor,
            ScanArgs scanArgs) {
        notNullKey(key);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        args.addKey(key);
        scanArgs(scanCursor, scanArgs, args);

        args.add(NOVALUES);

        KeyScanStreamingOutput<K, V> output = new KeyScanStreamingOutput<>(codec, channel);
        return createCommand(HSCAN, output, args);
    }

    Command<K, V, Boolean> hset(K key, K field, V value) {
        notNullKey(key);
        LettuceAssert.notNull(field, "Field " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(field).addValue(value);
        return createCommand(HSET, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Long> hset(K key, Map<K, V> map) {
        notNullKey(key);
        LettuceAssert.notNull(map, "Map " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!map.isEmpty(), "Map " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(map);
        return createCommand(HSET, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> hsetex(K key, Map<K, V> map) {
        notNullKey(key);
        LettuceAssert.notNull(map, "Map " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!map.isEmpty(), "Map " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(map.size()).add(map);

        return createCommand(HSETEX, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> hsetex(K key, HSetExArgs hSetExArgs, Map<K, V> map) {
        notNullKey(key);
        LettuceAssert.notNull(hSetExArgs, "HSetExArgs " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(map, "Map " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(!map.isEmpty(), "Map " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        hSetExArgs.build(args);
        args.add(FIELDS).add(map.size()).add(map);

        return createCommand(HSETEX, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<KeyValue<K, V>>> hgetex(K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HGETEX, new KeyValueListOutput<>(codec, Arrays.asList(fields)), args);
    }

    Command<K, V, List<KeyValue<K, V>>> hgetex(K key, HGetExArgs hGetExArgs, K... fields) {
        keyAndFieldsProvided(key, fields);
        LettuceAssert.notNull(hGetExArgs, "HGetExArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        hGetExArgs.build(args);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HGETEX, new KeyValueListOutput<>(codec, Arrays.asList(fields)), args);
    }

    Command<K, V, Long> hgetex(KeyValueStreamingChannel<K, V> channel, K key, HGetExArgs hGetExArgs, K... fields) {
        keyAndFieldsProvided(key, fields);
        LettuceAssert.notNull(hGetExArgs, "HGetExArgs " + MUST_NOT_BE_NULL);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        hGetExArgs.build(args);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HGETEX, new KeyValueStreamingOutput<>(codec, channel, Arrays.asList(fields)), args);
    }

    Command<K, V, List<KeyValue<K, V>>> hgetdel(K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HGETDEL, new KeyValueListOutput<>(codec, Arrays.asList(fields)), args);
    }

    Command<K, V, Long> hgetdel(KeyValueStreamingChannel<K, V> channel, K key, K... fields) {
        keyAndFieldsProvided(key, fields);

        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(FIELDS).add(fields.length).addKeys(fields);
        return createCommand(HGETDEL, new KeyValueStreamingOutput<>(codec, channel, Arrays.asList(fields)), args);
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
        return createCommand(CommandType.INFO, new StatusOutput<>(codec));
    }

    Command<K, V, String> info(String section) {
        LettuceAssert.notNull(section, "Section " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(section);
        return createCommand(CommandType.INFO, new StatusOutput<>(codec), args);
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

    Command<K, V, V> lmove(K source, K destination, LMoveArgs lMoveArgs) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(lMoveArgs, "LMoveArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(source).addKey(destination);
        lMoveArgs.build(args);
        return createCommand(LMOVE, new ValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, List<V>>> lmpop(LMPopArgs lmPopArgs, K... keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(lmPopArgs, "LMPopArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys);
        lmPopArgs.build(args);

        return createCommand(LMPOP, new KeyValueValueListOutput<>(codec), args);
    }

    Command<K, V, V> lpop(K key) {
        notNullKey(key);

        return createCommand(LPOP, new ValueOutput<>(codec), key);
    }

    Command<K, V, List<V>> lpop(K key, long count) {
        notNullKey(key);

        return createCommand(LPOP, new ValueListOutput<>(codec), new CommandArgs<>(codec).addKey(key).add(count));
    }

    Command<K, V, Long> lpos(K key, V value, LPosArgs lposArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);

        if (lposArgs != null) {
            lposArgs.build(args);
        }

        return createCommand(LPOS, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Long>> lpos(K key, V value, long count, LPosArgs lposArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value).add(COUNT).add(count);

        if (lposArgs != null) {
            lposArgs.build(args);
        }

        return createCommand(LPOS, new IntegerListOutput<>(codec), args);
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

    Command<K, V, Long> memoryUsage(K key) {
        return createCommand(MEMORY, new IntegerOutput<>(codec), new CommandArgs<>(codec).add(USAGE).addKey(key));
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

    Command<K, V, Long> objectFreq(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FREQ).addKey(key);
        return createCommand(OBJECT, new IntegerOutput<>(codec), args);
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

    Command<K, V, List<Long>> hpersist(K key, K... fields) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        args.add(FIELDS).add(fields.length).addKeys(fields);

        return createCommand(HPERSIST, new IntegerListOutput<>(codec), args);
    }

    Command<K, V, Boolean> pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(milliseconds);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        return createCommand(PEXPIRE, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Boolean> pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(timestamp);

        if (expireArgs != null) {
            expireArgs.build(args);
        }

        return createCommand(PEXPIREAT, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Long> pexpiretime(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(PEXPIRETIME, new IntegerOutput<>(codec), args);
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
    Command<K, V, Map<K, Long>> pubsubNumsub(K... channels) {
        LettuceAssert.notNull(channels, "Channels " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(channels, "Channels " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(NUMSUB).addKeys(channels);
        return createCommand(PUBSUB, (MapOutput) new MapOutput<K, Long>((RedisCodec) codec), args);
    }

    Command<K, V, List<K>> pubsubShardChannels() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SHARDCHANNELS);
        return createCommand(PUBSUB, new KeyListOutput<>(codec), args);
    }

    Command<K, V, List<K>> pubsubShardChannels(K pattern) {
        LettuceAssert.notNull(pattern, "Pattern " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SHARDCHANNELS).addKey(pattern);
        return createCommand(PUBSUB, new KeyListOutput<>(codec), args);
    }

    Command<K, V, Map<K, Long>> pubsubShardNumsub(K... shardChannels) {
        LettuceAssert.notNull(shardChannels, "ShardChannels " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(shardChannels, "ShardChannels " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(SHARDNUMSUB).addKeys(shardChannels);
        return createCommand(PUBSUB, (MapOutput) new MapOutput<K, Long>((RedisCodec) codec), args);
    }

    Command<K, V, String> quit() {
        return createCommand(QUIT, new StatusOutput<>(codec));
    }

    Command<K, V, K> randomkey() {
        return createCommand(RANDOMKEY, new KeyOutput<>(codec));
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

    Command<K, V, String> replicaof(String host, int port) {
        LettuceAssert.notNull(host, "Host " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(host, "Host " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(host).add(port);
        return createCommand(REPLICAOF, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> replicaofNoOne() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(NO).add(ONE);
        return createCommand(REPLICAOF, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> restore(K key, byte[] value, RestoreArgs restoreArgs) {
        notNullKey(key);
        LettuceAssert.notNull(value, "Value " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(restoreArgs, "RestoreArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(restoreArgs.ttl).add(value);

        restoreArgs.build(args);

        return createCommand(RESTORE, new StatusOutput<>(codec), args);
    }

    Command<K, V, List<Object>> role() {
        return createCommand(ROLE, new ArrayOutput<>(codec));
    }

    Command<K, V, V> rpop(K key) {
        notNullKey(key);

        return createCommand(RPOP, new ValueOutput<>(codec), key);
    }

    Command<K, V, List<V>> rpop(K key, long count) {
        notNullKey(key);

        return createCommand(RPOP, new ValueListOutput<>(codec), new CommandArgs<>(codec).addKey(key).add(count));
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

    Command<K, V, String> scriptFlush(FlushMode flushMode) {
        LettuceAssert.notNull(flushMode, "FlushMode " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(FLUSH).add(flushMode.name());
        return createCommand(SCRIPT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> scriptKill() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(KILL);
        return createCommand(SCRIPT, new StatusOutput<>(codec), args);
    }

    Command<K, V, String> scriptLoad(byte[] script) {
        LettuceAssert.notNull(script, "Script " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LOAD).add(script);
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

    Command<K, V, V> setGet(K key, V value) {
        return setGet(key, value, new SetArgs());
    }

    Command<K, V, V> setGet(K key, V value, SetArgs setArgs) {
        notNullKey(key);
        LettuceAssert.notNull(setArgs, "SetArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);
        setArgs.build(args);
        args.add(GET);

        return createCommand(SET, new ValueOutput<>(codec), args);
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

    Command<K, V, String> shutdown(ShutdownArgs shutdownArgs) {
        LettuceAssert.notNull(shutdownArgs, "shutdownArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        shutdownArgs.build(args);
        return createCommand(SHUTDOWN, new StatusOutput<>(codec), args);
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

    Command<K, V, Long> sintercard(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys);
        return createCommand(SINTERCARD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> sintercard(long limit, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys).add(LIMIT).add(limit);
        return createCommand(SINTERCARD, new IntegerOutput<>(codec), args);
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

    Command<K, V, List<Boolean>> smismember(K key, V... members) {
        notNullKey(key);
        LettuceAssert.notNull(members, "Members " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(members, "Members " + MUST_NOT_BE_EMPTY);

        return createCommand(SMISMEMBER, new BooleanListOutput<>(codec), key, members);
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

    Command<K, V, List<V>> sortReadOnly(K key) {
        notNullKey(key);

        return createCommand(SORT_RO, new ValueListOutput<>(codec), key);
    }

    Command<K, V, List<V>> sortReadOnly(K key, SortArgs sortArgs) {
        notNullKey(key);
        LettuceAssert.notNull(sortArgs, "SortArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        sortArgs.build(args, null);
        return createCommand(SORT_RO, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> sortReadOnly(ValueStreamingChannel<V> channel, K key) {
        notNullKey(key);
        notNull(channel);

        return createCommand(SORT_RO, new ValueStreamingOutput<>(codec, channel), key);
    }

    Command<K, V, Long> sortReadOnly(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs) {
        notNullKey(key);
        notNull(channel);
        LettuceAssert.notNull(sortArgs, "SortArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        sortArgs.build(args, null);
        return createCommand(SORT_RO, new ValueStreamingOutput<>(codec, channel), args);
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

    Command<K, V, Long> spublish(K shardChannel, V message) {
        LettuceAssert.notNull(shardChannel, "ShardChannel " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(shardChannel).addValue(message);
        return createCommand(SPUBLISH, new IntegerOutput<>(codec), args);
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

    Command<K, V, StringMatchResult> stralgoLcs(StrAlgoArgs strAlgoArgs) {
        LettuceAssert.notNull(strAlgoArgs, "StrAlgoArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        strAlgoArgs.build(args);
        return createCommand(STRALGO, new StringMatchResultOutput<>(codec), args);
    }

    Command<K, V, StringMatchResult> lcs(LcsArgs lcsArgs) {
        LettuceAssert.notNull(lcsArgs, "lcsArgs" + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        lcsArgs.build(args);
        return createCommand(LCS, new StringMatchResultOutput<>(codec), args);
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

        return createCommand(CommandType.TYPE, new StatusOutput<>(codec), key);
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

    Command<K, V, Boolean> copy(K source, K destination) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(source).addKey(destination);
        return createCommand(COPY, new BooleanOutput<>(codec), args);
    }

    Command<K, V, Boolean> copy(K source, K destination, CopyArgs copyArgs) {
        LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(source).addKey(destination);
        copyArgs.build(args);
        return createCommand(COPY, new BooleanOutput<>(codec), args);
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

    public Command<K, V, Long> xack(K key, K group, String[] messageIds) {
        notNullKey(key);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(messageIds, "MessageIds " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(messageIds, "MessageIds " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(group);

        for (String messageId : messageIds) {
            args.add(messageId);
        }

        return createCommand(XACK, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, List<StreamEntryDeletionResult>> xackdel(K key, K group, String[] messageIds) {
        return xackdel(key, group, null, messageIds);
    }

    public Command<K, V, List<StreamEntryDeletionResult>> xackdel(K key, K group, StreamDeletionPolicy policy,
            String[] messageIds) {
        notNullKey(key);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(messageIds, "MessageIds " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(messageIds, "MessageIds " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(group);

        if (policy != null) {
            args.add(policy);
        }

        args.add(CommandKeyword.IDS).add(messageIds.length);

        for (String messageId : messageIds) {
            args.add(messageId);
        }

        return createCommand(XACKDEL, new StreamEntryDeletionResultListOutput<>(codec), args);
    }

    public Command<K, V, ClaimedMessages<K, V>> xautoclaim(K key, XAutoClaimArgs<K> xAutoClaimArgs) {
        notNullKey(key);
        LettuceAssert.notNull(xAutoClaimArgs, "XAutoClaimArgs " + MUST_NOT_BE_NULL);
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        xAutoClaimArgs.build(args);
        return createCommand(XAUTOCLAIM, new ClaimedMessagesOutput<>(codec, key, xAutoClaimArgs.isJustid()), args);
    }

    public Command<K, V, List<StreamMessage<K, V>>> xclaim(K key, Consumer<K> consumer, XClaimArgs xClaimArgs,
            String[] messageIds) {

        notNullKey(key);
        LettuceAssert.notNull(consumer, "Consumer " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(messageIds, "MessageIds " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(messageIds, "MessageIds " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
        LettuceAssert.notNull(xClaimArgs, "XClaimArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(consumer.group).addKey(consumer.name)
                .add(xClaimArgs.minIdleTime);

        for (String messageId : messageIds) {
            args.add(messageId);
        }

        xClaimArgs.build(args);

        return createCommand(XCLAIM, new StreamMessageListOutput<>(codec, key), args);
    }

    public Command<K, V, String> xadd(K key, XAddArgs xAddArgs, Map<K, V> map) {
        notNullKey(key);
        LettuceAssert.notNull(map, "Message body " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (xAddArgs != null) {
            xAddArgs.build(args);
        } else {
            args.add("*");
        }

        args.add(map);

        return createCommand(XADD, new StatusOutput<>(codec), args);
    }

    public Command<K, V, String> xadd(K key, XAddArgs xAddArgs, Object[] body) {
        notNullKey(key);
        LettuceAssert.notNull(body, "Message body " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(body, "Message body " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(body, "Message body " + MUST_NOT_CONTAIN_NULL_ELEMENTS);
        LettuceAssert.isTrue(body.length % 2 == 0, "Message body.length must be a multiple of 2 and contain a "
                + "sequence of field1, value1, field2, value2, fieldN, valueN");

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (xAddArgs != null) {
            xAddArgs.build(args);
        } else {
            args.add("*");
        }

        for (int i = 0; i < body.length; i += 2) {
            args.addKey((K) body[i]);
            args.addValue((V) body[i + 1]);
        }

        return createCommand(XADD, new StatusOutput<>(codec), args);
    }

    public Command<K, V, Long> xdel(K key, String[] messageIds) {
        notNullKey(key);
        LettuceAssert.notEmpty(messageIds, "MessageIds " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(messageIds, "MessageIds " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        for (String messageId : messageIds) {
            args.add(messageId);
        }

        return createCommand(XDEL, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, List<StreamEntryDeletionResult>> xdelex(K key, String[] messageIds) {
        return xdelex(key, null, messageIds);
    }

    public Command<K, V, List<StreamEntryDeletionResult>> xdelex(K key, StreamDeletionPolicy policy, String[] messageIds) {
        notNullKey(key);
        LettuceAssert.notEmpty(messageIds, "MessageIds " + MUST_NOT_BE_EMPTY);
        LettuceAssert.noNullElements(messageIds, "MessageIds " + MUST_NOT_CONTAIN_NULL_ELEMENTS);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        if (policy != null) {
            args.add(policy);
        }

        args.add(CommandKeyword.IDS).add(messageIds.length);

        for (String messageId : messageIds) {
            args.add(messageId);
        }

        return createCommand(XDELEX, new StreamEntryDeletionResultListOutput<>(codec), args);
    }

    public Command<K, V, String> xgroupCreate(StreamOffset<K> offset, K group, XGroupCreateArgs commandArgs) {
        LettuceAssert.notNull(offset, "StreamOffset " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CREATE).addKey(offset.getName()).addKey(group)
                .add(offset.getOffset());

        if (commandArgs != null) {
            commandArgs.build(args);
        }

        return createCommand(XGROUP, new StatusOutput<>(codec), args);
    }

    public Command<K, V, Boolean> xgroupCreateconsumer(K key, Consumer<K> consumer) {
        notNullKey(key);
        LettuceAssert.notNull(consumer, "Consumer " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add("CREATECONSUMER").addKey(key).addKey(consumer.getGroup())
                .addKey(consumer.getName());

        return createCommand(XGROUP, new BooleanOutput<>(codec), args);
    }

    public Command<K, V, Long> xgroupDelconsumer(K key, Consumer<K> consumer) {
        notNullKey(key);
        LettuceAssert.notNull(consumer, "Consumer " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add("DELCONSUMER").addKey(key).addKey(consumer.getGroup())
                .addKey(consumer.getName());

        return createCommand(XGROUP, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Boolean> xgroupDestroy(K key, K group) {
        notNullKey(key);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add("DESTROY").addKey(key).addKey(group);

        return createCommand(XGROUP, new BooleanOutput<>(codec), args);
    }

    public Command<K, V, String> xgroupSetid(StreamOffset<K> offset, K group) {
        LettuceAssert.notNull(offset, "StreamOffset " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add("SETID").addKey(offset.getName()).addKey(group)
                .add(offset.getOffset());

        return createCommand(XGROUP, new StatusOutput<>(codec), args);
    }

    public Command<K, V, List<Object>> xinfoStream(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(STREAM).addKey(key);

        return createCommand(XINFO, new ArrayOutput<>(codec), args);
    }

    public Command<K, V, List<Object>> xinfoGroups(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(GROUPS).addKey(key);

        return createCommand(XINFO, new ArrayOutput<>(codec), args);
    }

    public Command<K, V, List<Object>> xinfoConsumers(K key, K group) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(CONSUMERS).addKey(key).addKey(group);

        return createCommand(XINFO, new ArrayOutput<>(codec), args);
    }

    public Command<K, V, Long> xlen(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        return createCommand(XLEN, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, PendingMessages> xpending(K key, K group) {
        notNullKey(key);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(group);

        return createCommand(XPENDING, new PendingMessagesOutput<>(codec), args);
    }

    public Command<K, V, List<PendingMessage>> xpending(K key, K group, Range<String> range, Limit limit) {
        notNullKey(key);
        LettuceAssert.notNull(group, "Group " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(range, "Range " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(limit, "Limit " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(group);

        args.add(getLowerValue(range)).add(getUpperValue(range));
        args.add(limit.isLimited() ? limit.getCount() : Long.MAX_VALUE);

        return createCommand(XPENDING, new PendingMessageListOutput<>(codec), args);
    }

    public Command<K, V, List<PendingMessage>> xpending(K key, Consumer<K> consumer, Range<String> range, Limit limit) {
        notNullKey(key);
        LettuceAssert.notNull(consumer, "Consumer " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(range, "Range " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(limit, "Limit " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addKey(consumer.group);

        args.add(getLowerValue(range)).add(getUpperValue(range));

        args.add(limit.isLimited() ? limit.getCount() : Long.MAX_VALUE);
        args.addKey(consumer.name);

        return createCommand(XPENDING, new PendingMessageListOutput<>(codec), args);
    }

    public Command<K, V, List<PendingMessage>> xpending(K key, XPendingArgs<K> commandArgs) {
        notNullKey(key);
        LettuceAssert.notNull(commandArgs, "XPendingArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        commandArgs.build(args);
        return createCommand(XPENDING, new PendingMessageListOutput<>(codec), args);
    }

    public Command<K, V, List<StreamMessage<K, V>>> xrange(K key, Range<String> range, Limit limit) {
        notNullKey(key);
        LettuceAssert.notNull(range, "Range " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(limit, "Limit " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(getLowerValue(range)).add(getUpperValue(range));

        if (limit.isLimited()) {
            args.add(COUNT).add(limit.getCount());
        }

        return createCommand(XRANGE, new StreamMessageListOutput<>(codec, key), args);
    }

    public Command<K, V, List<StreamMessage<K, V>>> xrevrange(K key, Range<String> range, Limit limit) {
        notNullKey(key);
        LettuceAssert.notNull(range, "Range " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(limit, "Limit " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);

        args.add(getUpperValue(range)).add(getLowerValue(range));

        if (limit.isLimited()) {
            args.add(COUNT).add(limit.getCount());
        }

        return createCommand(XREVRANGE, new StreamMessageListOutput<>(codec, key), args);
    }

    public Command<K, V, Long> xtrim(K key, boolean approximateTrimming, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(MAXLEN);

        if (approximateTrimming) {
            args.add("~");
        }

        args.add(count);

        return createCommand(XTRIM, new IntegerOutput<>(codec), args);
    }

    public Command<K, V, Long> xtrim(K key, XTrimArgs xTrimArgs) {
        notNullKey(key);
        LettuceAssert.notNull(xTrimArgs, "XTrimArgs " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        xTrimArgs.build(args);
        return createCommand(XTRIM, new IntegerOutput<>(codec), args);
    }

    private static String getLowerValue(Range<String> range) {

        Boundary<String> boundary = range.getLower();

        return boundary.equals(Boundary.unbounded()) ? "-" : getRange(boundary);
    }

    private static String getUpperValue(Range<String> range) {

        Boundary<String> boundary = range.getUpper();

        return boundary.equals(Boundary.unbounded()) ? "+" : getRange(boundary);
    }

    private static String getRange(Boundary<String> boundary) {
        return !boundary.isIncluding() ? "(" + boundary.getValue() : boundary.getValue();
    }

    public Command<K, V, List<StreamMessage<K, V>>> xread(XReadArgs xReadArgs, StreamOffset<K>[] streams) {
        LettuceAssert.notNull(streams, "Streams " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(streams.length > 0, "Streams " + MUST_NOT_BE_EMPTY);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        if (xReadArgs != null) {
            xReadArgs.build(args);
        }

        args.add("STREAMS");

        for (StreamOffset<K> stream : streams) {
            args.addKey(stream.name);
        }

        for (StreamOffset<K> stream : streams) {
            args.add(stream.offset);
        }

        return createCommand(XREAD, new StreamReadOutput<>(codec), args);
    }

    public Command<K, V, List<StreamMessage<K, V>>> xreadgroup(Consumer<K> consumer, XReadArgs xReadArgs,
            StreamOffset<K>... streams) {
        LettuceAssert.notNull(streams, "Streams " + MUST_NOT_BE_NULL);
        LettuceAssert.isTrue(streams.length > 0, "Streams " + MUST_NOT_BE_EMPTY);
        LettuceAssert.notNull(consumer, "Consumer " + MUST_NOT_BE_NULL);

        CommandArgs<K, V> args = new CommandArgs<>(codec);

        args.add("GROUP").add(encode(consumer.group)).add(encode(consumer.name));

        if (xReadArgs != null) {
            xReadArgs.build(args);
        }

        args.add("STREAMS");

        for (StreamOffset<K> stream : streams) {
            args.addKey(stream.name);
        }

        for (XReadArgs.StreamOffset<K> stream : streams) {
            args.add(stream.offset);
        }

        return createCommand(XREADGROUP, new StreamReadOutput<>(codec), args);
    }

    private byte[] encode(K k) {

        ByteBuffer byteBuffer = codec.encodeKey(k);

        byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);

        return result;
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> bzmpop(long timeout, ZPopArgs popArgs, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(timeout).add(keys.length).addKeys(keys);

        popArgs.build(args);

        return createCommand(BZMPOP, new KeyValueOfScoredValueOutput<>(codec, keys[0]), args);
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> bzmpop(double timeout, ZPopArgs popArgs, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(timeout).add(keys.length).addKeys(keys);

        popArgs.build(args);

        return createCommand(BZMPOP, new KeyValueOfScoredValueOutput<>(codec, keys[0]), args);
    }

    Command<K, V, KeyValue<K, List<ScoredValue<V>>>> bzmpop(long timeout, long count, ZPopArgs popArgs, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(timeout).add(keys.length).addKeys(keys);

        popArgs.build(args);
        args.add(COUNT).add(count);

        return createCommand(BZMPOP, new KeyValueListScoredValueOutput<>(codec, keys[0]), args);
    }

    Command<K, V, KeyValue<K, List<ScoredValue<V>>>> bzmpop(double timeout, long count, ZPopArgs popArgs, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(timeout).add(keys.length).addKeys(keys);

        popArgs.build(args);
        args.add(COUNT).add(count);

        return createCommand(BZMPOP, new KeyValueListScoredValueOutput<>(codec, keys[0]), args);
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> bzpopmin(long timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);

        return createCommand(BZPOPMIN, new KeyValueScoredValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> bzpopmin(double timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);

        return createCommand(BZPOPMIN, new KeyValueScoredValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> bzpopmax(long timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);

        return createCommand(BZPOPMAX, new KeyValueScoredValueOutput<>(codec), args);
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> bzpopmax(double timeout, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(keys).add(timeout);

        return createCommand(BZPOPMAX, new KeyValueScoredValueOutput<>(codec), args);
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
                            + "sequence of score1, value1, score2, value2, scoreN, valueN");

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

    Command<K, V, List<V>> zdiff(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(keys.length).addKeys(keys);
        return createCommand(ZDIFF, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zdiffstore(K destKey, K... srcKeys) {
        notNullKey(destKey);
        notEmpty(srcKeys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(destKey).add(srcKeys.length).addKeys(srcKeys);
        return createCommand(ZDIFFSTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zdiffWithScores(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(keys.length).addKeys(keys).add(WITHSCORES);
        return createCommand(ZDIFF, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Double> zincrby(K key, double amount, V member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).add(amount).addValue(member);
        return createCommand(ZINCRBY, new DoubleOutput<>(codec), args);
    }

    Command<K, V, List<V>> zinter(K... keys) {
        notEmpty(keys);

        return zinter(new ZAggregateArgs(), keys);
    }

    Command<K, V, List<V>> zinter(ZAggregateArgs aggregateArgs, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(keys.length).addKeys(keys);
        aggregateArgs.build(args);
        return createCommand(ZINTER, new ValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zintercard(K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys);
        return createCommand(ZINTERCARD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zintercard(long limit, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys).add(LIMIT).add(limit);
        return createCommand(ZINTERCARD, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zinterWithScores(K... keys) {
        notEmpty(keys);

        return zinterWithScores(new ZAggregateArgs(), keys);
    }

    Command<K, V, List<ScoredValue<V>>> zinterWithScores(ZAggregateArgs aggregateArgs, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(keys.length).addKeys(keys).add(WITHSCORES);
        aggregateArgs.build(args);
        return createCommand(ZINTER, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zinterstore(K destination, K... keys) {
        notEmpty(keys);

        return zinterstore(destination, new ZAggregateArgs(), keys);
    }

    Command<K, V, Long> zinterstore(K destination, ZAggregateArgs aggregateArgs, K... keys) {
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(aggregateArgs, "ZStoreArgs " + MUST_NOT_BE_NULL);
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(destination).add(keys.length).addKeys(keys);
        aggregateArgs.build(args);
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

    Command<K, V, List<Double>> zmscore(K key, V... members) {
        notNullKey(key);
        notEmpty(members);

        return createCommand(ZMSCORE, new DoubleListOutput<>(codec), key, members);
    }

    Command<K, V, KeyValue<K, ScoredValue<V>>> zmpop(ZPopArgs popArgs, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys);

        popArgs.build(args);

        return createCommand(ZMPOP, new KeyValueOfScoredValueOutput<>(codec, keys[0]), args);
    }

    Command<K, V, KeyValue<K, List<ScoredValue<V>>>> zmpop(long count, ZPopArgs popArgs, K[] keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec).add(keys.length).addKeys(keys);

        popArgs.build(args);
        args.add(COUNT).add(count);

        return createCommand(ZMPOP, new KeyValueListScoredValueOutput<>(codec, keys[0]), args);
    }

    Command<K, V, ScoredValue<V>> zpopmin(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key);

        return createCommand(ZPOPMIN, new ScoredValueOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zpopmin(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key).add(count);

        return createCommand(ZPOPMIN, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, ScoredValue<V>> zpopmax(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key);

        return createCommand(ZPOPMAX, new ScoredValueOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zpopmax(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key).add(count);

        return createCommand(ZPOPMAX, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, V> zrandmember(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key);

        return createCommand(ZRANDMEMBER, new ValueOutput<>(codec), args);
    }

    Command<K, V, List<V>> zrandmember(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key).add(count);

        return createCommand(ZRANDMEMBER, new ValueListOutput<>(codec), args);
    }

    Command<K, V, ScoredValue<V>> zrandmemberWithScores(K key) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key).add(1).add(WITHSCORES);

        return createCommand(ZRANDMEMBER, new ScoredValueOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zrandmemberWithScores(K key, long count) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKeys(key).add(count).add(WITHSCORES);

        return createCommand(ZRANDMEMBER, new ScoredValueListOutput<>(codec), args);
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

    Command<K, V, Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
        notNullKey(key);
        notNullRange(range);
        notNullLimit(limit);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(min(range)).add(max(range)).add(WITHSCORES), limit);
        return createCommand(ZRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrangestore(K dstKey, K srcKey, Range<Long> range, boolean rev) {
        notNullKey(srcKey);
        notNullKey(dstKey);
        notNullRange(range);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKeys(dstKey, srcKey);

        args.add(min(range)).add(max(range));

        if (rev) {
            args.add(REV);
        }
        return createCommand(ZRANGESTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zrangestorebylex(K dstKey, K srcKey, Range<? extends V> range, Limit limit, boolean rev) {
        notNullKey(srcKey);
        notNullKey(dstKey);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKeys(dstKey, srcKey);

        if (rev) {
            args.add(maxValue(range)).add(minValue(range));
        } else {
            args.add(minValue(range)).add(maxValue(range));
        }

        args.add(BYLEX);

        if (rev) {
            args.add(REV);
        }
        addLimit(args, limit);
        return createCommand(ZRANGESTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zrangestorebyscore(K dstKey, K srcKey, Range<? extends Number> range, Limit limit, boolean rev) {
        notNullKey(srcKey);
        notNullKey(dstKey);
        notNullRange(range);
        notNullLimit(limit);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKeys(dstKey, srcKey);

        if (rev) {
            args.add(max(range)).add(min(range));
        } else {
            args.add(min(range)).add(max(range));
        }

        args.add(BYSCORE);
        if (rev) {
            args.add(REV);
        }
        addLimit(args, limit);
        return createCommand(ZRANGESTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, Long> zrank(K key, V member) {
        notNullKey(key);

        return createCommand(ZRANK, new IntegerOutput<>(codec), key, member);
    }

    Command<K, V, ScoredValue<Long>> zrankWithScore(K key, V member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(member).add(WITHSCORE);
        return createCommand(ZRANK, (ScoredValueOutput) new ScoredValueOutput<>(LongCodec.INSTANCE), args);
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

    Command<K, V, Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, Range<? extends Number> range, Limit limit) {
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

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count) {
        notNullKey(key);
        notNull(channel);
        return zrevrangebyscoreWithScores(channel, key, string(max), string(min), offset, count);
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count) {
        notNullKey(key);
        notNullMinMax(min, max);
        notNull(channel);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        addLimit(args.addKey(key).add(max).add(min).add(WITHSCORES), Limit.create(offset, count));
        return createCommand(ZREVRANGEBYSCORE, new ScoredValueStreamingOutput<>(codec, channel), args);
    }

    Command<K, V, Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, Range<? extends Number> range,
            Limit limit) {
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

    Command<K, V, ScoredValue<Long>> zrevrankWithScore(K key, V member) {
        notNullKey(key);

        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(member).add(WITHSCORE);
        return createCommand(ZREVRANK, (ScoredValueOutput) new ScoredValueOutput<>(LongCodec.INSTANCE), args);
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

    Command<K, V, List<V>> zunion(K... keys) {
        notEmpty(keys);

        return zunion(new ZAggregateArgs(), keys);
    }

    Command<K, V, List<V>> zunion(ZAggregateArgs aggregateArgs, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(keys.length).addKeys(keys);
        aggregateArgs.build(args);
        return createCommand(ZUNION, new ValueListOutput<>(codec), args);
    }

    Command<K, V, List<ScoredValue<V>>> zunionWithScores(K... keys) {
        notEmpty(keys);

        return zunionWithScores(new ZAggregateArgs(), keys);
    }

    Command<K, V, List<ScoredValue<V>>> zunionWithScores(ZAggregateArgs aggregateArgs, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.add(keys.length).addKeys(keys).add(WITHSCORES);
        aggregateArgs.build(args);
        return createCommand(ZUNION, new ScoredValueListOutput<>(codec), args);
    }

    Command<K, V, Long> zunionstore(K destination, K... keys) {
        notEmpty(keys);
        LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);

        return zunionstore(destination, new ZAggregateArgs(), keys);
    }

    Command<K, V, Long> zunionstore(K destination, ZAggregateArgs aggregateArgs, K... keys) {
        notEmpty(keys);

        CommandArgs<K, V> args = new CommandArgs<>(codec);
        args.addKey(destination).add(keys.length).addKeys(keys);
        aggregateArgs.build(args);
        return createCommand(ZUNIONSTORE, new IntegerOutput<>(codec), args);
    }

    Command<K, V, List<Map<String, Object>>> clusterLinks() {
        CommandArgs<K, V> args = new CommandArgs<>(codec).add(LINKS);
        return createCommand(CLUSTER, (CommandOutput) new ObjectOutput<>(StringCodec.UTF8), args);
    }

    enum LongCodec implements RedisCodec<Long, Long> {

        INSTANCE;

        @Override
        public Long decodeKey(ByteBuffer bytes) {

            String s = StringCodec.ASCII.decodeKey(bytes);

            return s == null ? null : Long.valueOf(s);
        }

        @Override
        public Long decodeValue(ByteBuffer bytes) {
            return decodeKey(bytes);
        }

        @Override
        public ByteBuffer encodeKey(Long key) {
            return StringCodec.ASCII.encodeKey(key == null ? null : key.toString());
        }

        @Override
        public ByteBuffer encodeValue(Long value) {
            return encodeKey(value);
        }

    }

}
