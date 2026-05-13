/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.Range;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument builder for the Redis {@code ARSCAN} command.
 * <p>
 * Bundles the range (start/end) and optional LIMIT for ARSCAN.
 * <p>
 * Example usage:
 *
 * <pre>
 * ArScanArgs.range(0, 100);
 * ArScanArgs.range(0, 100).limit(50);
 * </pre>
 *
 * {@link ArScanArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/arscan/">Redis Documentation: ARSCAN</a>
 */
public class ArScanArgs implements CompositeArgument {

    private final Range<Long> range;

    private Long limit;

    private ArScanArgs(Range<Long> range) {
        LettuceAssert.notNull(range, "Range must not be null");
        this.range = range;
    }

    /**
     * Creates a new {@link ArScanArgs} with a bounded range.
     *
     * @param start the start index (inclusive).
     * @param end the end index (inclusive).
     * @return a new {@link ArScanArgs} instance.
     */
    public static ArScanArgs range(long start, long end) {
        return new ArScanArgs(Range.create(start, end));
    }

    /**
     * Sets the maximum number of entries to return.
     *
     * @param limit the limit.
     * @return {@code this}.
     */
    public ArScanArgs limit(long limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        args.add(range.getLower().getValue());
        args.add(range.getUpper().getValue());

        if (limit != null) {
            args.add(CommandKeyword.LIMIT);
            args.add(limit);
        }
    }

}
