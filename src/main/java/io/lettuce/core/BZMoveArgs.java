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
 * ({@code MIN} or {@code MAX}), the blocking {@code timeout} in seconds and an optional {@code COUNT}. Static import the
 * methods from {@link Builder} and chain the method calls: {@code min().timeout(0.5).count(10)}.
 * <p>
 * The {@code timeout} defaults to {@code 0} (block indefinitely) when not set explicitly. When the connection's command timeout
 * ends the call first, the command may still be waiting on the server and may still move members. {@link BZMoveArgs} is a
 * mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class BZMoveArgs implements CompositeArgument {

    private final ProtocolKeyword selection;

    private Long longTimeout;

    private Double doubleTimeout;

    private Long count;

    private BZMoveArgs(ProtocolKeyword selection) {
        this.selection = selection;
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
         * Creates new {@link BZMoveArgs} taking the members with the lowest scores ({@code MIN}).
         *
         * @return new {@link BZMoveArgs} with {@code MIN} set.
         * @since 8.0
         */
        public static BZMoveArgs min() {
            return new BZMoveArgs(MIN);
        }

        /**
         * Creates new {@link BZMoveArgs} taking the members with the highest scores ({@code MAX}).
         *
         * @return new {@link BZMoveArgs} with {@code MAX} set.
         * @since 8.0
         */
        public static BZMoveArgs max() {
            return new BZMoveArgs(MAX);
        }

    }

    /**
     * Set the blocking {@code timeout} in seconds. {@code 0} blocks indefinitely. The value is sent as given. Replaces a
     * timeout set before.
     *
     * @param timeout the timeout in seconds.
     * @return {@code this} {@link BZMoveArgs}.
     * @since 8.0
     */
    public BZMoveArgs timeout(long timeout) {
        this.longTimeout = timeout;
        this.doubleTimeout = null;
        return this;
    }

    /**
     * Set the blocking {@code timeout} in seconds, allowing for sub-second resolution. {@code 0} blocks indefinitely. The value
     * is sent as given. Replaces a timeout set before.
     *
     * @param timeout the timeout in seconds.
     * @return {@code this} {@link BZMoveArgs}.
     * @since 8.0
     */
    public BZMoveArgs timeout(double timeout) {
        this.doubleTimeout = timeout;
        this.longTimeout = null;
        return this;
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
            args.add(longTimeout != null ? longTimeout : 0L);
        }

        if (count != null) {
            args.add(COUNT).add(count);
        }
    }

}
