/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.List;

/**
 * Argument builder for the Redis {@code ARGREP} command.
 * <p>
 * Bundles the range (start/end), one or more predicates, optional AND/OR combiner, LIMIT, and NOCASE flags.
 * <p>
 * A {@code null} lower bound maps to the Redis {@code -} token and a {@code null} upper bound maps to {@code +}.
 * <p>
 * Example usage:
 *
 * <pre>
 * ArGrepArgs.unbounded().exact("hello");
 * ArGrepArgs.range(0, 100).match("world").nocase();
 * ArGrepArgs.from(5).glob("prefix*");
 * ArGrepArgs.to(100).re("^foo.*");
 * ArGrepArgs.unbounded().re("^foo.*").glob("bar*").and().limit(10);
 * ArGrepArgs.unbounded().reversed().exact("hello"); // all elements, descending
 * ArGrepArgs.range(3, 7).reversed().exact("foo"); // elements in [3,7], descending
 * </pre>
 *
 * {@link ArGrepArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/argrep/">Redis Documentation: ARGREP</a>
 */
@Experimental
public class ArGrepArgs implements CompositeArgument {

    private final Long rangeStart;

    private final Long rangeEnd;

    private final List<ArGrepPredicate> predicates = new ArrayList<>();

    private boolean useCombinerAnd = false;

    private Long limit;

    private boolean noCase = false;

    private boolean reversed = false;

    private ArGrepArgs(Long rangeStart, Long rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    /**
     * Creates a new {@link ArGrepArgs} with an unbounded range ({@code - +} in Redis).
     *
     * @return a new {@link ArGrepArgs} instance.
     */
    public static ArGrepArgs unbounded() {
        return new ArGrepArgs(null, null);
    }

    /**
     * Creates a new {@link ArGrepArgs} with a bounded range.
     *
     * @param start the start index (inclusive).
     * @param end the end index (inclusive).
     * @return a new {@link ArGrepArgs} instance.
     */
    public static ArGrepArgs range(long start, long end) {
        return new ArGrepArgs(start, end);
    }

    /**
     * Creates a new {@link ArGrepArgs} with a lower bound only ({@code start +} in Redis).
     *
     * @param start the start index (inclusive); upper bound is unbounded.
     * @return a new {@link ArGrepArgs} instance.
     */
    public static ArGrepArgs from(long start) {
        return new ArGrepArgs(start, null);
    }

    /**
     * Creates a new {@link ArGrepArgs} with an upper bound only ({@code - end} in Redis).
     *
     * @param end the end index (inclusive); lower bound is unbounded.
     * @return a new {@link ArGrepArgs} instance.
     */
    public static ArGrepArgs to(long end) {
        return new ArGrepArgs(null, end);
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

    /**
     * Reverses the iteration order. The same elements are matched, but results are returned in descending index order instead
     * of ascending.
     * <p>
     * On the wire, this swaps the two range arguments (e.g. {@code 3 7} becomes {@code 7 3}).
     *
     * @return {@code this}.
     */
    public ArGrepArgs reversed() {
        this.reversed = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        // Range: two tokens emitted in order. When reversed, the tokens are swapped.
        // Forward: (start or '-') (end or '+')
        // Reversed: (end or '+') (start or '-')
        String startToken = rangeStart != null ? String.valueOf(rangeStart) : "-";
        String endToken = rangeEnd != null ? String.valueOf(rangeEnd) : "+";

        if (reversed) {
            args.add(endToken);
            args.add(startToken);
        } else {
            args.add(startToken);
            args.add(endToken);
        }

        // Predicates
        for (ArGrepPredicate predicate : predicates) {
            args.add(predicate.getType().getKeyword());
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
