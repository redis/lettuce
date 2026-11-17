/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.COUNT;
import static io.lettuce.core.protocol.CommandKeyword.MAX;
import static io.lettuce.core.protocol.CommandKeyword.MIN;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the blocking {@code BZMOVE} command. Selects the end of the sorted set to take members from
 * ({@code MIN} or {@code MAX}) together with the blocking timeout in seconds, and an optional {@code COUNT}. Static import the
 * methods from {@link Builder} and chain the method calls: {@code min(0.5).count(10)}.
 * <p>
 * The timeout is required; {@code 0} blocks indefinitely. When the connection's command timeout ends the call first, the
 * command may still be waiting on the server and may still move members. {@link BZMoveArgs} is a mutable object and instances
 * should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class BZMoveArgs implements CompositeArgument {

    private final ProtocolKeyword selection;

    private final Long longTimeout;

    private final Double doubleTimeout;

    private Long count;

    private BZMoveArgs(ProtocolKeyword selection, Long longTimeout, Double doubleTimeout) {
        this.selection = selection;
        this.longTimeout = longTimeout;
        this.doubleTimeout = doubleTimeout;
    }

    /**
     * Builder entry points for {@link BZMoveArgs}.
     *
     * @since 8.0
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link BZMoveArgs} taking the members with the lowest scores ({@code MIN}), blocking up to
         * {@code timeout} seconds. {@code 0} blocks indefinitely. The value is sent as given.
         *
         * @param timeout the timeout in seconds.
         * @return new {@link BZMoveArgs} with {@code MIN} and the timeout set.
         * @since 8.0
         */
        public static BZMoveArgs min(long timeout) {
            return new BZMoveArgs(MIN, timeout, null);
        }

        /**
         * Creates new {@link BZMoveArgs} taking the members with the lowest scores ({@code MIN}), blocking up to
         * {@code timeout} seconds, allowing for sub-second resolution. {@code 0} blocks indefinitely. The value is sent as
         * given.
         *
         * @param timeout the timeout in seconds.
         * @return new {@link BZMoveArgs} with {@code MIN} and the timeout set.
         * @since 8.0
         */
        public static BZMoveArgs min(double timeout) {
            return new BZMoveArgs(MIN, null, timeout);
        }

        /**
         * Creates new {@link BZMoveArgs} taking the members with the highest scores ({@code MAX}), blocking up to
         * {@code timeout} seconds. {@code 0} blocks indefinitely. The value is sent as given.
         *
         * @param timeout the timeout in seconds.
         * @return new {@link BZMoveArgs} with {@code MAX} and the timeout set.
         * @since 8.0
         */
        public static BZMoveArgs max(long timeout) {
            return new BZMoveArgs(MAX, timeout, null);
        }

        /**
         * Creates new {@link BZMoveArgs} taking the members with the highest scores ({@code MAX}), blocking up to
         * {@code timeout} seconds, allowing for sub-second resolution. {@code 0} blocks indefinitely. The value is sent as
         * given.
         *
         * @param timeout the timeout in seconds.
         * @return new {@link BZMoveArgs} with {@code MAX} and the timeout set.
         * @since 8.0
         */
        public static BZMoveArgs max(double timeout) {
            return new BZMoveArgs(MAX, null, timeout);
        }

    }

    /**
     * Move up to {@code count} members. The value is sent as given. Without a count, one member is moved.
     *
     * @param count the maximum number of members to move.
     * @return {@code this} {@link BZMoveArgs}.
     * @since 8.0
     */
    public BZMoveArgs count(long count) {
        this.count = count;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        args.add(selection);

        if (doubleTimeout != null) {
            args.add(doubleTimeout);
        } else {
            args.add(longTimeout);
        }

        if (count != null) {
            args.add(COUNT).add(count);
        }
    }

}
