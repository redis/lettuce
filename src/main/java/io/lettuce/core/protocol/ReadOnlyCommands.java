/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.protocol;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Contains all command names that are read-only commands.
 *
 * @author Mark Paluch
 * @author Mingi Lee
 * @since 6.2.5
 */
public class ReadOnlyCommands {

    private static final Set<CommandType> READ_ONLY_COMMANDS = EnumSet.noneOf(CommandType.class);

    private static final ReadOnlyPredicate PREDICATE = command -> isReadOnlyCommand(command.getType());

    static {
        for (CommandName commandNames : CommandName.values()) {
            READ_ONLY_COMMANDS.add(CommandType.valueOf(commandNames.name()));
        }
    }

    /**
     * @param protocolKeyword must not be {@code null}.
     * @return {@code true} if {@link ProtocolKeyword} is a read-only command.
     */
    public static boolean isReadOnlyCommand(ProtocolKeyword protocolKeyword) {
        return READ_ONLY_COMMANDS.contains(protocolKeyword);
    }

    /**
     * @return an unmodifiable {@link Set} of {@link CommandType read-only} commands.
     */
    public static Set<CommandType> getReadOnlyCommands() {
        return Collections.unmodifiableSet(READ_ONLY_COMMANDS);
    }

    /**
     * Return a {@link ReadOnlyPredicate} to test against the underlying {@link #isReadOnlyCommand(ProtocolKeyword) known
     * commands}.
     *
     * @return a {@link ReadOnlyPredicate} to test against the underlying {@link #isReadOnlyCommand(ProtocolKeyword) known
     *         commands}.
     */
    public static ReadOnlyPredicate asPredicate() {
        return PREDICATE;
    }

    enum CommandName {
        ASKING, BITCOUNT, BITPOS, CLIENT, COMMAND, DUMP, ECHO, EVAL_RO, EVALSHA_RO, EXISTS, FCALL_RO, //
        GEODIST, GEOPOS, GEORADIUS_RO, GEORADIUSBYMEMBER_RO, GEOSEARCH, GEOHASH, GET, DIGEST, GETBIT, //
        GETRANGE, HEXISTS, HGET, HGETALL, HKEYS, HLEN, HMGET, HRANDFIELD, HSCAN, HSTRLEN, //
        HVALS, INFO, KEYS, LINDEX, LLEN, LPOS, LRANGE, SORT_RO, MGET, PFCOUNT, PTTL, //
        RANDOMKEY, READWRITE, SCAN, SCARD, SCRIPT, //
        SDIFF, SINTER, SISMEMBER, SMISMEMBER, SMEMBERS, SRANDMEMBER, SSCAN, STRLEN, //
        SUNION, TIME, TTL, TYPE, //
        XINFO, XLEN, XPENDING, XRANGE, XREVRANGE, XREAD, //
        JSON_ARRINDEX, JSON_ARRLEN, JSON_GET, JSON_MGET, JSON_OBJKEYS, JSON_OBJLEN, JSON_STRLEN, JSON_TYPE, //
        ZCARD, ZCOUNT, ZLEXCOUNT, ZRANGE, //
        ZRANDMEMBER, ZRANGEBYLEX, ZRANGEBYSCORE, ZRANK, ZREVRANGE, ZREVRANGEBYLEX, ZREVRANGEBYSCORE, ZREVRANK, ZSCAN, ZSCORE,
        // RediSearch (keyless read-only)
        FT_AGGREGATE, FT_SEARCH, FT_EXPLAIN, FT_SPELLCHECK, FT_TAGVALS, FT_SYNDUMP, FT_LIST, FT_DICTDUMP,
    }

    /**
     * A predicate to determine whether a command qualifies as Read-Only command.
     *
     * @since 6.2.4
     */
    @FunctionalInterface
    public interface ReadOnlyPredicate {

        /**
         * Evaluates this predicate on the given {@link RedisCommand}.
         *
         * @param command the input command.
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         */
        boolean isReadOnly(RedisCommand<?, ?, ?> command);

        /**
         * Returns a composed predicate that represents a short-circuiting logical AND of this predicate and another. When
         * evaluating the composed predicate, if this predicate is {@code false}, then the {@code other} predicate is not
         * evaluated.
         *
         * <p>
         * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if evaluation of this
         * predicate throws an exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ANDed with this predicate.
         * @return a composed predicate that represents the short-circuiting logical AND of this predicate and the {@code other}
         *         predicate.
         * @throws IllegalArgumentException if other is {@code null}.
         */
        default ReadOnlyPredicate and(ReadOnlyPredicate other) {

            LettuceAssert.notNull(other, "Other ReadOnlyPredicate must not be null");

            return (t) -> isReadOnly(t) && other.isReadOnly(t);
        }

        /**
         * Returns a predicate that represents the logical negation of this predicate.
         *
         * @return a predicate that represents the logical negation of this predicate.
         */
        default ReadOnlyPredicate negate() {
            return (t) -> !isReadOnly(t);
        }

        /**
         * Returns a composed predicate that represents a short-circuiting logical OR of this predicate and another. When
         * evaluating the composed predicate, if this predicate is {@code true}, then the {@code other} predicate is not
         * evaluated.
         *
         * <p>
         * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if evaluation of this
         * predicate throws an exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ORed with this predicate.
         * @return a composed predicate that represents the short-circuiting logical OR of this predicate and the {@code other}
         *         predicate.
         * @throws IllegalArgumentException if other is {@code null}.
         */
        default ReadOnlyPredicate or(ReadOnlyPredicate other) {

            LettuceAssert.notNull(other, "Other ReadOnlyPredicate must not be null");

            return (t) -> isReadOnly(t) || other.isReadOnly(t);
        }

    }

}
