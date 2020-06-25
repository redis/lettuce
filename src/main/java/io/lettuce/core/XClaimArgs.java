/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/xclaim">XCLAIM</a> command. Static import the methods
 * from {@link XClaimArgs.Builder} and call the methods: {@code minIdleTime(…)} .
 * <p>
 * {@link XClaimArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class XClaimArgs {

    long minIdleTime;

    private Long idle;

    private Long time;

    private Long retrycount;

    private boolean force;

    private boolean justid;

    /**
     * Builder entry points for {@link XAddArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XClaimArgs} and set the {@code JUSTID} flag to return just the message id and do not increment the
         * retry counter. The message body is not returned when calling {@code XCLAIM}.
         *
         * @return new {@link XClaimArgs} with min idle time set.
         * @see XClaimArgs#justid()
         * @since 5.3
         */
        public static XClaimArgs justid() {
            return new XClaimArgs().justid();
        }

        public static XClaimArgs minIdleTime(long milliseconds) {
            return new XClaimArgs().minIdleTime(milliseconds);
        }

        /**
         * Creates new {@link XClaimArgs} and set the minimum idle time.
         *
         * @return new {@link XClaimArgs} with min idle time set.
         * @see XClaimArgs#minIdleTime(long)
         */
        public static XClaimArgs minIdleTime(Duration minIdleTime) {

            LettuceAssert.notNull(minIdleTime, "Min idle time must not be null");

            return minIdleTime(minIdleTime.toMillis());
        }

    }

    /**
     * Set the {@code JUSTID} flag to return just the message id and do not increment the retry counter. The message body is not
     * returned when calling {@code XCLAIM}.
     *
     * @return {@code this}.
     * @since 5.3
     */
    public XClaimArgs justid() {

        this.justid = true;
        return this;
    }

    /**
     * Return only messages that are idle for at least {@code milliseconds}.
     *
     * @param milliseconds min idle time.
     * @return {@code this}.
     */
    public XClaimArgs minIdleTime(long milliseconds) {

        this.minIdleTime = milliseconds;
        return this;
    }

    /**
     * Return only messages that are idle for at least {@code minIdleTime}.
     *
     * @param minIdleTime min idle time.
     * @return {@code this}.
     */
    public XClaimArgs minIdleTime(Duration minIdleTime) {

        LettuceAssert.notNull(minIdleTime, "Min idle time must not be null");

        return minIdleTime(minIdleTime.toMillis());
    }

    /**
     * Set the idle time (last time it was delivered) of the message. If IDLE is not specified, an IDLE of 0 is assumed, that
     * is, the time count is reset because the message has now a new owner trying to process it
     *
     * @param milliseconds idle time.
     * @return {@code this}.
     */
    public XClaimArgs idle(long milliseconds) {

        this.idle = milliseconds;
        return this;
    }

    /**
     * Set the idle time (last time it was delivered) of the message. If IDLE is not specified, an IDLE of 0 is assumed, that
     * is, the time count is reset because the message has now a new owner trying to process it
     *
     * @param idleTime idle time.
     * @return {@code this}.
     */
    public XClaimArgs idle(Duration idleTime) {

        LettuceAssert.notNull(idleTime, "Idle time must not be null");

        return idle(idleTime.toMillis());
    }

    /**
     * This is the same as IDLE but instead of a relative amount of milliseconds, it sets the idle time to a specific unix time
     * (in milliseconds). This is useful in order to rewrite the AOF file generating XCLAIM commands.
     *
     * @param millisecondsUnixTime idle time.
     * @return {@code this}.
     */
    public XClaimArgs time(long millisecondsUnixTime) {

        this.time = millisecondsUnixTime;
        return this;
    }

    /**
     * This is the same as IDLE but instead of a relative amount of milliseconds, it sets the idle time to a specific unix time
     * (in milliseconds). This is useful in order to rewrite the AOF file generating XCLAIM commands.
     *
     * @param timestamp idle time.
     * @return {@code this}.
     */
    public XClaimArgs time(TemporalAccessor timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return time(Instant.from(timestamp).toEpochMilli());
    }

    /**
     * Set the retry counter to the specified value. This counter is incremented every time a message is delivered again.
     * Normally {@code XCLAIM} does not alter this counter, which is just served to clients when the XPENDING command is called:
     * this way clients can detect anomalies, like messages that are never processed for some reason after a big number of
     * delivery attempts.
     *
     * @param retrycount number of retries.
     * @return {@code this}.
     */
    public XClaimArgs retryCount(long retrycount) {

        this.retrycount = retrycount;
        return this;
    }

    /**
     * Creates the pending message entry in the PEL even if certain specified IDs are not already in the PEL assigned to a
     * different client. However the message must be exist in the stream, otherwise the IDs of non existing messages are
     * ignored.
     *
     * @return {@code this}.
     */
    public XClaimArgs force() {
        return force(true);
    }

    /**
     * Creates the pending message entry in the PEL even if certain specified IDs are not already in the PEL assigned to a
     * different client. However the message must be exist in the stream, otherwise the IDs of non existing messages are
     * ignored.
     *
     * @param force {@code true} to enforce PEL creation.
     * @return {@code this}.
     */
    public XClaimArgs force(boolean force) {

        this.force = force;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (idle != null) {
            args.add(CommandKeyword.IDLE).add(idle);
        }

        if (time != null) {
            args.add(CommandType.TIME).add(time);
        }

        if (retrycount != null) {
            args.add(CommandKeyword.RETRYCOUNT).add(retrycount);
        }

        if (force) {
            args.add(CommandKeyword.FORCE);
        }

        if (justid) {
            args.add(CommandKeyword.JUSTID);
        }
    }

}
