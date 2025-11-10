/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis MSETEX command (Redis 8.4+).
 *
 * <p>
 * Usage examples (type-safe step builder):
 * <ul>
 * <li>{@code MSetExArgs.builder().nx().ex(10).build()}</li>
 * <li>{@code MSetExArgs.builder().noCondition().exAt(Instant.now().plusSeconds(30)).build()}</li>
 * <li>{@code MSetExArgs.builder().noCondition().pxAt(Instant.now().plusSeconds(1_500)).build()}</li>
 * <li>{@code MSetExArgs.builder().noCondition().keepttl().build()}</li>
 * </ul>
 *
 * <p>
 * Token emission order follows the MSETEX PRD: [NX|XX] first, then exactly one of [EX seconds | PX milliseconds | EXAT
 * unix-time-seconds | PXAT unix-time-milliseconds | KEEPTTL].
 *
 * <p>
 * Construct via {@link #builder()} to ensure type-safe sequencing (condition → expiration → build). NX/XX are mutually
 * exclusive, and at most one expiration can be specified. Instances are intended for single use within a command invocation.
 *
 * @author Aleksandar Todorov
 */
public class MSetExArgs implements CompositeArgument {

    private Long ex; // seconds

    private Long exAt; // epoch seconds

    private Long px; // millis

    private Long pxAt; // epoch millis

    private boolean nx = false;

    private boolean xx = false;

    private boolean keepttl = false;

    /**
     * Type-safe builder implementation for {@link MSetExArgs}. Obtain an instance via {@link #builder()} and then follow the
     * step sequence.
     */
    public static final class Builder implements Step.ConditionStep, Step.ExpirationStep, Step.BuildStep {

        private final MSetExArgs args = new MSetExArgs();

        private Builder() {
        }

        // Condition
        @Override
        public Step.ExpirationStep nx() {
            args.nx();
            return this;
        }

        @Override
        public Step.ExpirationStep xx() {
            args.xx();
            return this;
        }

        @Override
        public Step.ExpirationStep noCondition() {
            return this;
        }

        // Expiration (all overloads)
        @Override
        public Step.BuildStep ex(long seconds) {
            args.ex(seconds);
            return this;
        }

        @Override
        public Step.BuildStep ex(Duration timeout) {
            args.ex(timeout);
            return this;
        }

        @Override
        public Step.BuildStep exAt(long epochSeconds) {
            args.exAt(epochSeconds);
            return this;
        }

        @Override
        public Step.BuildStep exAt(Date timestamp) {
            args.exAt(timestamp);
            return this;
        }

        @Override
        public Step.BuildStep exAt(Instant timestamp) {
            args.exAt(timestamp);
            return this;
        }

        @Override
        public Step.BuildStep px(long millis) {
            args.px(millis);
            return this;
        }

        @Override
        public Step.BuildStep px(Duration timeout) {
            args.px(timeout);
            return this;
        }

        @Override
        public Step.BuildStep pxAt(long epochMillis) {
            args.pxAt(epochMillis);
            return this;
        }

        @Override
        public Step.BuildStep pxAt(Date timestamp) {
            args.pxAt(timestamp);
            return this;
        }

        @Override
        public Step.BuildStep pxAt(Instant timestamp) {
            args.pxAt(timestamp);
            return this;
        }

        @Override
        public Step.BuildStep keepttl() {
            args.keepttl();
            return this;
        }

        @Override
        public Step.BuildStep noExpiration() {
            return this;
        }

        @Override
        public MSetExArgs build() {
            return args;
        }

    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        // Emit NX/XX first
        if (nx) {
            args.add(NX);
        }
        if (xx) {
            args.add(XX);
        }

        // Then the single expiration spec, if any
        if (ex != null) {
            args.add(EX).add(ex);
        }
        if (exAt != null) {
            args.add(EXAT).add(exAt);
        }
        if (px != null) {
            args.add(PX).add(px);
        }
        if (pxAt != null) {
            args.add(PXAT).add(pxAt);
        }
        if (keepttl) {
            args.add(KEEPTTL);
        }
    }

    /**
     * Start a new type-safe builder for {@link MSetExArgs}.
     * <p>
     * Guides callers through valid choices in this order: Condition (NX/XX/none) -> Expiration (EX/PX/EXAT/PXAT/KEEPTTL/none)
     * -> build().
     * <p>
     * Example: {@code MSetExArgs.builder().nx().ex(10).build()}.
     */
    public static Step.ConditionStep builder() {
        return new Builder();
    }

    /** Type-safe step-builder for {@link MSetExArgs}. */
    public static final class Step {

        /** First step: choose condition (or none). */
        public interface ConditionStep {

            /** Only set keys if none exist (NX). */
            ExpirationStep nx();

            /** Only set keys if all exist (XX). */
            ExpirationStep xx();

            /** Do not apply an existence condition (neither NX nor XX). */
            ExpirationStep noCondition();

        }

        /** Second step: choose at most one expiration (or none). */
        public interface ExpirationStep {

            /** Set expire time in seconds (EX). */
            BuildStep ex(long seconds);

            /** Set expire time as a {@link Duration} (EX). */
            BuildStep ex(Duration timeout);

            /** Set absolute expire time in epoch seconds (EXAT). */
            BuildStep exAt(long epochSeconds);

            /** Set absolute expire time (EXAT) from {@link Date}. */
            BuildStep exAt(Date timestamp);

            /** Set absolute expire time (EXAT) from {@link Instant}. */
            BuildStep exAt(Instant timestamp);

            /** Set expire time in milliseconds (PX). */
            BuildStep px(long millis);

            /** Set expire time as a {@link Duration} (PX). */
            BuildStep px(Duration timeout);

            /** Set absolute expire time in epoch milliseconds (PXAT). */
            BuildStep pxAt(long epochMillis);

            /** Set absolute expire time (PXAT) from {@link Date}. */
            BuildStep pxAt(Date timestamp);

            /** Set absolute expire time (PXAT) from {@link Instant}. */
            BuildStep pxAt(Instant timestamp);

            /** Retain existing TTL of keys (KEEPTTL). */
            BuildStep keepttl();

            /** Do not specify an expiration. */
            BuildStep noExpiration();

        }

        /** Final step: build the {@link MSetExArgs} instance. */
        public interface BuildStep {

            MSetExArgs build();

        }

    }

    private void ex(long seconds) {
        LettuceAssert.isTrue(seconds >= 0, "Timeout must be greater or equal to 0");
        this.ex = seconds;
    }

    public void ex(Duration timeout) {
        LettuceAssert.notNull(timeout, "Timeout must not be null");
        this.ex = timeout.getSeconds();
    }

    private void exAt(long epochSeconds) {
        this.exAt = epochSeconds;
    }

    private void exAt(Date timestamp) {
        LettuceAssert.notNull(timestamp, "Timestamp must not be null");
        this.exAt = timestamp.toInstant().getEpochSecond();
    }

    private void exAt(Instant timestamp) {
        LettuceAssert.notNull(timestamp, "Timestamp must not be null");
        this.exAt = timestamp.getEpochSecond();
    }

    private void px(long millis) {
        LettuceAssert.isTrue(millis >= 0, "Timeout must be greater or equal to 0");
        this.px = millis;
    }

    private void px(Duration timeout) {
        LettuceAssert.notNull(timeout, "Timeout must not be null");
        this.px = timeout.toMillis();
    }

    private void pxAt(long epochMillis) {
        this.pxAt = epochMillis;
    }

    private void pxAt(Date timestamp) {
        LettuceAssert.notNull(timestamp, "Timestamp must not be null");
        this.pxAt = timestamp.toInstant().toEpochMilli();
    }

    private void pxAt(Instant timestamp) {
        LettuceAssert.notNull(timestamp, "Timestamp must not be null");
        this.pxAt = timestamp.toEpochMilli();
    }

    private void nx() {
        this.nx = true;
    }

    private void xx() {
        this.xx = true;
    }

    private void keepttl() {
        this.keepttl = true;
    }

}
