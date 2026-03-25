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
 * Command syntax: {@code GCRA key max_burst requests_per_period period [num_requests]}
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/commands/gcra">GCRA command reference</a>
 */
public class GCRAArgs implements CompositeArgument {

    private long maxBurst;

    private long requestsPerPeriod;

    private double period;

    private Long numRequests;

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
         * @param requestsPerPeriod number of requests allowed per period. Min: 1.
         * @param period period in seconds. Min: 1.0, Max: 1e12.
         * @return new {@link GCRAArgs} with required parameters set.
         */
        public static GCRAArgs rate(long maxBurst, long requestsPerPeriod, double period) {
            return new GCRAArgs().maxBurst(maxBurst).requestsPerPeriod(requestsPerPeriod).period(period);
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
     * Set the number of requests allowed per period.
     *
     * @param requestsPerPeriod requests per period. Min: 1.
     * @return {@code this} {@link GCRAArgs}.
     */
    public GCRAArgs requestsPerPeriod(long requestsPerPeriod) {
        LettuceAssert.isTrue(requestsPerPeriod >= 1, "requestsPerPeriod must be >= 1");
        this.requestsPerPeriod = requestsPerPeriod;
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
     * Set the cost/weight of this request. Defaults to 1 if not specified.
     *
     * @param numRequests cost of the request. Min: 1.
     * @return {@code this} {@link GCRAArgs}.
     */
    public GCRAArgs numRequests(long numRequests) {
        LettuceAssert.isTrue(numRequests >= 1, "numRequests must be >= 1");
        this.numRequests = numRequests;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        args.add(maxBurst);
        args.add(requestsPerPeriod);
        args.add(period);

        if (numRequests != null) {
            args.add(CommandKeyword.NUM_REQUESTS);
            args.add(numRequests);
        }
    }

}
