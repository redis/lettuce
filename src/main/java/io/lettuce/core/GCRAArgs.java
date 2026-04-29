/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/gcra">GCRA</a> command. Static import the methods from
 * {@link GCRAArgs.Builder} and call the methods: {@code maxBurst(…)} .
 * <p>
 * {@link GCRAArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 * <p>
 * Command syntax: {@code GCRA key max_burst tokens_per_period period [TOKENS count]}
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/commands/gcra">GCRA command reference</a>
 */
public class GCRAArgs implements CompositeArgument {

    private long maxBurst;

    private long tokensPerPeriod;

    private double period;

    private Long tokens;

    /**
     * Builder entry points for {@link GCRAArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link GCRAArgs} with the required parameters.
         *
         * @param maxBurst maximum number of tokens allowed as a burst. Min: 0.
         * @param tokensPerPeriod number of tokens replenished per period. Min: 1.
         * @param period period in seconds. Min: 1.0, Max: 1e12.
         * @return new {@link GCRAArgs} with required parameters set.
         */
        public static GCRAArgs rate(long maxBurst, long tokensPerPeriod, double period) {
            return new GCRAArgs().maxBurst(maxBurst).tokensPerPeriod(tokensPerPeriod).period(period);
        }

    }

    /**
     * Set the maximum number of tokens allowed as a burst.
     *
     * @param maxBurst maximum burst size. Min: 0.
     * @return {@code this} {@link GCRAArgs}.
     */
    public GCRAArgs maxBurst(long maxBurst) {
        LettuceAssert.isTrue(maxBurst >= 0, "maxBurst must be >= 0");
        this.maxBurst = maxBurst;
        return this;
    }

    /**
     * Set the number of tokens replenished per period.
     *
     * @param tokensPerPeriod tokens per period. Min: 1.
     * @return {@code this} {@link GCRAArgs}.
     */
    public GCRAArgs tokensPerPeriod(long tokensPerPeriod) {
        LettuceAssert.isTrue(tokensPerPeriod >= 1, "tokensPerPeriod must be >= 1");
        this.tokensPerPeriod = tokensPerPeriod;
        return this;
    }

    /**
     * Set the period in seconds.
     *
     * @param period period in seconds. Min: 1.0, Max: 1e12.
     * @return {@code this} {@link GCRAArgs}.
     */
    public GCRAArgs period(double period) {
        LettuceAssert.isTrue(period >= 1.0, "period must be >= 1.0");
        LettuceAssert.isTrue(period <= 1e12, "period must be <= 1e12");
        this.period = period;
        return this;
    }

    /**
     * Set the number of tokens to consume. Defaults to 1 if not specified.
     *
     * @param tokens number of tokens to consume. Min: 1.
     * @return {@code this} {@link GCRAArgs}.
     */
    public GCRAArgs tokens(long tokens) {
        LettuceAssert.isTrue(tokens >= 1, "tokens must be >= 1");
        this.tokens = tokens;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        args.add(maxBurst);
        args.add(tokensPerPeriod);
        args.add(period);

        if (tokens != null) {
            args.add(CommandKeyword.TOKENS);
            args.add(tokens);
        }
    }

}
