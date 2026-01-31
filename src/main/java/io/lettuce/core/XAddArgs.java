/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
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

import java.util.Arrays;
import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/xadd">XADD</a> command. Static import the methods from
 * {@link Builder} and call the methods: {@code maxlen(…)} .
 * <p>
 * {@link XAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @author dengliming
 * @since 5.1
 */
public class XAddArgs implements CompositeArgument {

    private String id;

    private Long maxlen;

    private boolean approximateTrimming;

    private boolean exactTrimming;

    private boolean nomkstream;

    private String minid;

    private Long limit;

    private StreamDeletionPolicy trimmingMode;

    private byte[] producerId;

    private byte[] idempotentId;

    private boolean autoIdempotent;

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
         * Creates new {@link XAddArgs} and setting {@literal MAXLEN}.
         *
         * @return new {@link XAddArgs} with {@literal MAXLEN} set.
         * @see XAddArgs#maxlen(long)
         */
        public static XAddArgs maxlen(long count) {
            return new XAddArgs().maxlen(count);
        }

        /**
         * Creates new {@link XAddArgs} and setting {@literal NOMKSTREAM}.
         *
         * @return new {@link XAddArgs} with {@literal NOMKSTREAM} set.
         * @see XAddArgs#nomkstream()
         * @since 6.1
         */
        public static XAddArgs nomkstream() {
            return new XAddArgs().nomkstream();
        }

        /**
         * Creates new {@link XAddArgs} and setting {@literal MINID}.
         *
         * @param minid the oldest ID in the stream will be exactly the minimum between its original oldest ID and the specified
         *        threshold.
         * @return new {@link XAddArgs} with {@literal MINID} set.
         * @see XAddArgs#minId(String)
         * @since 6.1
         */
        public static XAddArgs minId(String minid) {
            return new XAddArgs().minId(minid);
        }

        /**
         * Creates new {@link XAddArgs} with {@literal IDMP} (idempotent producer with explicit ID).
         *
         * @param producerId the producer ID (must be unique per producer).
         * @param idempotentId the idempotent ID (must be unique per message).
         * @return new {@link XAddArgs} with {@literal IDMP} set.
         * @see XAddArgs#idmp(byte[], byte[])
         */
        public static XAddArgs idmp(byte[] producerId, byte[] idempotentId) {
            return new XAddArgs().idmp(producerId, idempotentId);
        }

        /**
         * Creates new {@link XAddArgs} with {@literal IDMP} (idempotent producer with explicit ID).
         *
         * @param producerId the producer ID (must be unique per producer).
         * @param idempotentId the idempotent ID (must be unique per message).
         * @return new {@link XAddArgs} with {@literal IDMP} set.
         * @see XAddArgs#idmp(String, String)
         */
        public static XAddArgs idmp(String producerId, String idempotentId) {
            return new XAddArgs().idmp(producerId, idempotentId);
        }

        /**
         * Creates new {@link XAddArgs} with {@literal IDMPAUTO} (idempotent producer with auto-generated ID).
         *
         * @param producerId the producer ID (must be unique per producer).
         * @return new {@link XAddArgs} with {@literal IDMPAUTO} set.
         * @see XAddArgs#idmpAuto(byte[])
         */
        public static XAddArgs idmpAuto(byte[] producerId) {
            return new XAddArgs().idmpAuto(producerId);
        }

        /**
         * Creates new {@link XAddArgs} with {@literal IDMPAUTO} (idempotent producer with auto-generated ID).
         *
         * @param producerId the producer ID (must be unique per producer).
         * @return new {@link XAddArgs} with {@literal IDMPAUTO} set.
         * @see XAddArgs#idmpAuto(String)
         */
        public static XAddArgs idmpAuto(String producerId) {
            return new XAddArgs().idmpAuto(producerId);
        }

    }

    /**
     * Specify the message {@code id}.
     *
     * @param id must not be {@code null}.
     * @return {@code this}
     */
    public XAddArgs id(String id) {

        LettuceAssert.notNull(id, "Id must not be null");

        this.id = id;
        return this;
    }

    /**
     * Limit stream to {@code maxlen} entries.
     *
     * @param maxlen number greater 0.
     * @return {@code this}
     */
    public XAddArgs maxlen(long maxlen) {

        LettuceAssert.isTrue(maxlen > 0, "Maxlen must be greater 0");

        this.maxlen = maxlen;
        return this;
    }

    /**
     * Limit stream entries by message Id.
     *
     * @param minid the oldest ID in the stream will be exactly the minimum between its original oldest ID and the specified
     *        threshold.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs minId(String minid) {

        LettuceAssert.notNull(minid, "minId must not be null");

        this.minid = minid;
        return this;
    }

    /**
     * The maximum number of entries to trim.
     *
     * @param limit has meaning only if {@link #approximateTrimming `~`} was set.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs limit(long limit) {

        LettuceAssert.isTrue(limit > 0, "Limit must be greater 0");

        this.limit = limit;
        return this;
    }

    /**
     * When trimming, defines desired behaviour for handling consumer group references. See {@link StreamDeletionPolicy} for
     * details.
     *
     * @param trimmingMode the deletion policy to apply during trimming.
     * @return {@code this}
     */
    public XAddArgs trimmingMode(StreamDeletionPolicy trimmingMode) {
        this.trimmingMode = trimmingMode;
        return this;
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @return {@code this}
     */
    public XAddArgs approximateTrimming() {
        return approximateTrimming(true);
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @param approximateTrimming {@code true} to apply efficient radix node trimming.
     * @return {@code this}
     */
    public XAddArgs approximateTrimming(boolean approximateTrimming) {

        this.approximateTrimming = approximateTrimming;
        return this;
    }

    /**
     * Apply exact trimming for capped streams using the {@code =} flag.
     *
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs exactTrimming() {
        return exactTrimming(true);
    }

    /**
     * Apply exact trimming for capped streams using the {@code =} flag.
     *
     * @param exactTrimming {@code true} to apply exact radix node trimming.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs exactTrimming(boolean exactTrimming) {

        this.exactTrimming = exactTrimming;
        return this;
    }

    /**
     * Do add the message if the stream does not already exist.
     *
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs nomkstream() {
        return nomkstream(true);
    }

    /**
     * Do add the message if the stream does not already exist.
     *
     * @param nomkstream {@code true} to not create a stream if it does not already exist.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs nomkstream(boolean nomkstream) {

        this.nomkstream = nomkstream;
        return this;
    }

    /**
     * Enable idempotent producer mode with explicit idempotent ID.
     *
     * @param producerId the producer ID (must be unique per producer and consistent across restarts).
     * @param idempotentId the idempotent ID (must be unique per message and consistent across resends).
     * @return {@code this}
     */
    public XAddArgs idmp(byte[] producerId, byte[] idempotentId) {

        LettuceAssert.notNull(producerId, "Producer ID must not be null");
        LettuceAssert.notNull(idempotentId, "Idempotent ID must not be null");
        LettuceAssert.isTrue(!autoIdempotent, "Cannot use both IDMP and IDMPAUTO");

        this.producerId = producerId;
        this.idempotentId = idempotentId;
        return this;
    }

    /**
     * Enable idempotent producer mode with explicit idempotent ID.
     *
     * @param producerId the producer ID (must be unique per producer and consistent across restarts).
     * @param idempotentId the idempotent ID (must be unique per message and consistent across resends).
     * @return {@code this}
     */
    public XAddArgs idmp(String producerId, String idempotentId) {

        LettuceAssert.notNull(producerId, "Producer ID must not be null");
        LettuceAssert.notNull(idempotentId, "Idempotent ID must not be null");

        return idmp(producerId.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                idempotentId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Enable idempotent producer mode with auto-generated idempotent ID (content-based).
     *
     * @param producerId the producer ID (must be unique per producer and consistent across restarts).
     * @return {@code this}
     */
    public XAddArgs idmpAuto(byte[] producerId) {

        LettuceAssert.notNull(producerId, "Producer ID must not be null");
        LettuceAssert.isTrue(idempotentId == null, "Cannot use both IDMP and IDMPAUTO");

        this.producerId = producerId;
        this.autoIdempotent = true;
        return this;
    }

    /**
     * Enable idempotent producer mode with auto-generated idempotent ID (content-based).
     *
     * @param producerId the producer ID (must be unique per producer and consistent across restarts).
     * @return {@code this}
     */
    public XAddArgs idmpAuto(String producerId) {

        LettuceAssert.notNull(producerId, "Producer ID must not be null");

        return idmpAuto(producerId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        // Order: NOMKSTREAM → trimmingMode → IDMP/IDMPAUTO → MAXLEN/MINID → LIMIT → id

        if (nomkstream) {
            args.add(CommandKeyword.NOMKSTREAM);
        }

        if (trimmingMode != null) {
            args.add(trimmingMode);
        }

        if (autoIdempotent) {
            args.add(CommandKeyword.IDMPAUTO);
            args.add(producerId);
        } else if (producerId != null && idempotentId != null) {
            args.add(CommandKeyword.IDMP);
            args.add(producerId);
            args.add(idempotentId);
        }

        if (maxlen != null) {
            args.add(CommandKeyword.MAXLEN);

            if (approximateTrimming) {
                args.add("~");
            } else if (exactTrimming) {
                args.add("=");
            }

            args.add(maxlen);
        }

        if (minid != null) {

            args.add(CommandKeyword.MINID);

            if (approximateTrimming) {
                args.add("~");
            } else if (exactTrimming) {
                args.add("=");
            }

            args.add(minid);
        }

        if (limit != null && approximateTrimming) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }

        if (id != null) {
            args.add(id);
        } else {
            args.add("*");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        XAddArgs xAddArgs = (XAddArgs) o;
        return approximateTrimming == xAddArgs.approximateTrimming && exactTrimming == xAddArgs.exactTrimming
                && nomkstream == xAddArgs.nomkstream && autoIdempotent == xAddArgs.autoIdempotent
                && Objects.equals(id, xAddArgs.id) && Objects.equals(maxlen, xAddArgs.maxlen)
                && Objects.equals(minid, xAddArgs.minid) && Objects.equals(limit, xAddArgs.limit)
                && trimmingMode == xAddArgs.trimmingMode && Arrays.equals(producerId, xAddArgs.producerId)
                && Arrays.equals(idempotentId, xAddArgs.idempotentId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, maxlen, approximateTrimming, exactTrimming, nomkstream, minid, limit, trimmingMode,
                autoIdempotent);
        result = 31 * result + Arrays.hashCode(producerId);
        result = 31 * result + Arrays.hashCode(idempotentId);
        return result;
    }

}
