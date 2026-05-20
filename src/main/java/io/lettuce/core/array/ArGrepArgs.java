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

import java.util.ArrayList;
import java.util.List;

/**
 * Argument builder for the Redis {@code ARGREP} command.
 * <p>
 * Bundles the range (start/end), one or more predicates, optional AND/OR combiner, LIMIT, NOCASE, and WITHVALUES flags.
 * <p>
 * The range is specified using {@link Range}{@code <Long>}. Use {@link Range.Boundary#unbounded()} to map to the Redis
 * {@code -} and {@code +} tokens.
 * <p>
 * Example usage:
 *
 * <pre>
 * ArGrepArgs.unbounded().exact("hello");
 * ArGrepArgs.range(0, 100).match("world").nocase();
 * ArGrepArgs.unbounded().re("^foo.*").glob("bar*").and().limit(10);
 * </pre>
 *
 * {@link ArGrepArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/argrep/">Redis Documentation: ARGREP</a>
 */
public class ArGrepArgs implements CompositeArgument {

    private Range<Long> range;

    private final List<ArGrepPredicate> predicates = new ArrayList<>();

    private boolean useCombinerAnd = false;

    private Long limit;

    private boolean noCase = false;

    private ArGrepArgs(Range<Long> range) {
        LettuceAssert.notNull(range, "Range must not be null");
        this.range = range;
    }

    /**
     * Creates a new {@link ArGrepArgs} with an unbounded range ({@code - +} in Redis).
     *
     * @return a new {@link ArGrepArgs} instance.
     */
    public static ArGrepArgs unbounded() {
        return new ArGrepArgs(Range.unbounded());
    }

    /**
     * Creates a new {@link ArGrepArgs} with a bounded range.
     *
     * @param start the start index (inclusive).
     * @param end the end index (inclusive).
     * @return a new {@link ArGrepArgs} instance.
     */
    public static ArGrepArgs range(long start, long end) {
        return new ArGrepArgs(Range.create(start, end));
    }

    /**
     * Adds an EXACT predicate.
     *
     * @param value the exact value to match.
     * @return {@code this}.
     */
    public ArGrepArgs exact(String value) {
        predicates.add(new ArGrepPredicate(ArGrepPredicate.Type.EXACT, value));
        return this;
    }

    /**
     * Adds a MATCH (substring) predicate.
     *
     * @param value the substring to match.
     * @return {@code this}.
     */
    public ArGrepArgs match(String value) {
        predicates.add(new ArGrepPredicate(ArGrepPredicate.Type.MATCH, value));
        return this;
    }

    /**
     * Adds a GLOB predicate.
     *
     * @param pattern the glob pattern.
     * @return {@code this}.
     */
    public ArGrepArgs glob(String pattern) {
        predicates.add(new ArGrepPredicate(ArGrepPredicate.Type.GLOB, pattern));
        return this;
    }

    /**
     * Adds a RE (regex) predicate.
     *
     * @param pattern the regex pattern.
     * @return {@code this}.
     */
    public ArGrepArgs re(String pattern) {
        predicates.add(new ArGrepPredicate(ArGrepPredicate.Type.RE, pattern));
        return this;
    }

    /**
     * Sets the predicate combiner to AND (default is OR).
     *
     * @return {@code this}.
     */
    public ArGrepArgs and() {
        this.useCombinerAnd = true;
        return this;
    }

    /**
     * Sets the maximum number of results to return.
     *
     * @param limit the limit.
     * @return {@code this}.
     */
    public ArGrepArgs limit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Enables case-insensitive matching.
     *
     * @return {@code this}.
     */
    public ArGrepArgs nocase() {
        this.noCase = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        // Range: start end (unbounded maps to - / +)
        if (range.getLower().isUnbounded()) {
            args.add("-");
        } else {
            args.add(range.getLower().getValue());
        }

        if (range.getUpper().isUnbounded()) {
            args.add("+");
        } else {
            args.add(range.getUpper().getValue());
        }

        // Predicates
        for (ArGrepPredicate predicate : predicates) {
            switch (predicate.getType()) {
                case EXACT:
                    args.add(CommandKeyword.EXACT);
                    break;
                case MATCH:
                    args.add(CommandKeyword.MATCH);
                    break;
                case GLOB:
                    args.add(CommandKeyword.GLOB);
                    break;
                case RE:
                    args.add(CommandKeyword.RE);
                    break;
            }
            args.add(predicate.getPattern());
        }

        // Combiner (AND keyword; OR is default so not emitted)
        if (useCombinerAnd) {
            args.add(CommandKeyword.AND);
        }

        // LIMIT
        if (limit != null) {
            args.add(CommandKeyword.LIMIT);
            args.add(limit);
        }

        // NOCASE
        if (noCase) {
            args.add(CommandKeyword.NOCASE);
        }
    }

}
