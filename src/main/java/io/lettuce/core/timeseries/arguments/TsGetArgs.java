/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.get/">TS.GET</a> command.
 * <p>
 * {@link TsGetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsGetArgs implements CompositeArgument {

    private boolean latest;

    /**
     * Builder entry points for {@link TsGetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TsGetArgs} and requests the compacted value of the latest, possibly partial, bucket.
         *
         * @return a new {@link TsGetArgs} with {@code LATEST} configured.
         */
        public static TsGetArgs latest() {
            return new TsGetArgs().latest();
        }

    }

    /**
     * Request the compacted value of the latest, possibly partial, bucket. Only meaningful when the key is the destination of a
     * compaction rule; ignored otherwise.
     *
     * @return {@code this} {@link TsGetArgs}.
     */
    public TsGetArgs latest() {
        this.latest = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (latest) {
            args.add(CommandKeyword.LATEST);
        }
    }

}
