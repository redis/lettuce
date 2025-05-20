/*
 * Copyright 2018-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.Optional;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/vsim/">VSIM</a> command. Static import the
 * methods from {@link Builder} and call the methods: {@code count(…)} .
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *    VSimArgs args = VSimArgs
 *                        .count(10)
 *                        .explorationFactor(200)
 *                        .filter("price < 100")
 *                        .filterEfficiency(10);
 * }
 * </pre>
 * <p>
 * {@link VSimArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class VSimArgs implements CompositeArgument {

    private Optional<Long> count = Optional.empty();

    private Optional<Long> explorationFactor = Optional.empty();

    private Optional<String> filter = Optional.empty();

    private Optional<Long> filterEfficiency = Optional.empty();

    private Optional<Boolean> truth = Optional.empty();

    private Optional<Boolean> noThread = Optional.empty();

    /**
     * Builder entry points for {@link VSimArgs}.
     * <p>
     * These static methods provide a convenient way to create new instances of {@link VSimArgs} with specific options set. Each
     * method creates a new instance and sets the corresponding option.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal COUNT}.
         * <p>
         * The COUNT option limits the number of returned results to the specified value.
         *
         * @param count the number of results to return.
         * @return new {@link VSimArgs} with {@literal COUNT} set.
         * @see VSimArgs#count(Long)
         */
        public static VSimArgs count(Long count) {
            return new VSimArgs().count(count);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal EF}.
         * <p>
         * The EF (exploration factor) option controls the search effort. Higher values explore more nodes, improving recall at
         * the cost of speed. Typical values range from 50 to 1000.
         *
         * @param explorationFactor the exploration factor for the vector search.
         * @return new {@link VSimArgs} with {@literal EF} set.
         * @see VSimArgs#explorationFactor(Long)
         */
        public static VSimArgs explorationFactor(Long explorationFactor) {
            return new VSimArgs().explorationFactor(explorationFactor);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal FILTER}.
         * <p>
         * The FILTER option applies a filter expression to restrict matching elements. Filter expressions can be used to narrow
         * down search results based on attributes.
         *
         * @param filter the filter expression to apply.
         * @return new {@link VSimArgs} with {@literal FILTER} set.
         * @see VSimArgs#filter(String)
         * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/filtered-search/">Filter expressions</a>
         */
        public static VSimArgs filter(String filter) {
            return new VSimArgs().filter(filter);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal FILTER-EF}.
         * <p>
         * The FILTER-EF option limits the number of filtering attempts for the FILTER expression. This controls the maximum
         * effort spent on filtering during the search process.
         *
         * @param filterEfficiency the maximum filtering effort to use.
         * @return new {@link VSimArgs} with {@literal FILTER-EF} set.
         * @see VSimArgs#filterEfficiency(Long)
         * @see <a href="https://redis.io/docs/latest/develop/data-types/vector-sets/filtered-search/">Filter expressions</a>
         */
        public static VSimArgs filterEfficiency(Long filterEfficiency) {
            return new VSimArgs().filterEfficiency(filterEfficiency);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal TRUTH}.
         * <p>
         * The TRUTH option forces an exact linear scan of all elements, bypassing the HNSW graph. This is useful for
         * benchmarking or to calculate recall. Note that this is significantly slower with a time complexity of O(N) instead of
         * O(log(N)).
         *
         * @return new {@link VSimArgs} with {@literal TRUTH} set.
         * @see VSimArgs#truth()
         */
        public static VSimArgs truth() {
            return new VSimArgs().truth();
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal NOTHREAD}.
         * <p>
         * The NOTHREAD option executes the search in the main thread instead of a background thread. This is useful for small
         * vector sets or benchmarks, but may block the server during execution and increase server latency.
         *
         * @return new {@link VSimArgs} with {@literal NOTHREAD} set.
         * @see VSimArgs#noThread()
         */
        public static VSimArgs noThread() {
            return new VSimArgs().noThread();
        }

    }

    /**
     * Set the number of results to return.
     * <p>
     * The COUNT option limits the number of returned results to the specified value. This is useful when you only need a
     * specific number of the most similar elements.
     * <p>
     * Time complexity: O(log(N)) where N is the number of elements in the vector set.
     *
     * @param count the number of results to return.
     * @return {@code this}
     */
    public VSimArgs count(Long count) {
        LettuceAssert.isTrue(count > 0, "Count must be greater than 0");
        this.count = Optional.of(count);
        return this;
    }

    /**
     * Set the exploration factor for the vector search.
     * <p>
     * The EF (exploration factor) option controls the search effort. Higher values explore more nodes, improving recall at the
     * cost of speed. Typical values range from 50 to 1000.
     * <p>
     * Increasing this value will generally improve search quality but reduce performance.
     *
     * @param explorationFactor the exploration factor for the vector search.
     * @return {@code this}
     */
    public VSimArgs explorationFactor(Long explorationFactor) {
        LettuceAssert.isTrue(explorationFactor > 0, "Exploration factor must be greater than 0");
        this.explorationFactor = Optional.of(explorationFactor);
        return this;
    }

    /**
     * Set the filter expression to apply.
     * <p>
     * The FILTER option applies a filter expression to restrict matching elements. Filter expressions can be used to narrow
     * down search results based on attributes associated with the vector elements.
     * <p>
     * For example, a filter like "price < 100" would only return elements with a price attribute less than 100.
     *
     * @param filter the filter expression to apply.
     * @return {@code this}
     */
    public VSimArgs filter(String filter) {
        LettuceAssert.notNull(filter, "Filter must not be null");
        LettuceAssert.notEmpty(filter, "Filter must not be empty");
        this.filter = Optional.of(filter);
        return this;
    }

    /**
     * Set the filter efficiency to use.
     * <p>
     * The FILTER-EF option limits the number of filtering attempts for the FILTER expression. This controls the maximum effort
     * spent on filtering during the search process.
     * <p>
     * Higher values will spend more effort trying to find elements that match the filter, potentially improving recall for
     * filtered searches at the cost of performance.
     *
     * @param filterEfficiency the maximum filtering effort to use.
     * @return {@code this}
     */
    public VSimArgs filterEfficiency(Long filterEfficiency) {
        LettuceAssert.isTrue(filterEfficiency >= 0, "Filter efficiency must be greater than or equal to 0");
        this.filterEfficiency = Optional.of(filterEfficiency);
        return this;
    }

    /**
     * Enable truth mode for the vector search.
     * <p>
     * The TRUTH option forces an exact linear scan of all elements, bypassing the HNSW graph. This is useful for benchmarking
     * or to calculate recall. Note that this is significantly slower with a time complexity of O(N) instead of O(log(N)).
     * <p>
     * This option is primarily intended for testing and evaluation purposes.
     *
     * @return {@code this}
     */
    public VSimArgs truth() {
        return truth(true);
    }

    /**
     * Enable or disable truth mode for the vector search.
     * <p>
     * The TRUTH option forces an exact linear scan of all elements, bypassing the HNSW graph. This is useful for benchmarking
     * or to calculate recall. Note that this is significantly slower with a time complexity of O(N) instead of O(log(N)).
     * <p>
     * This option is primarily intended for testing and evaluation purposes.
     *
     * @param truth whether to enable truth mode.
     * @return {@code this}
     */
    public VSimArgs truth(boolean truth) {
        this.truth = Optional.of(truth);
        return this;
    }

    /**
     * Disable threading for the vector search.
     * <p>
     * The NOTHREAD option executes the search in the main thread instead of a background thread. This is useful for small
     * vector sets or benchmarks, but may block the server during execution and increase server latency.
     * <p>
     * Use this option with caution in production environments.
     *
     * @return {@code this}
     */
    public VSimArgs noThread() {
        return noThread(true);
    }

    /**
     * Enable or disable threading for the vector search.
     * <p>
     * The NOTHREAD option executes the search in the main thread instead of a background thread. This is useful for small
     * vector sets or benchmarks, but may block the server during execution and increase server latency.
     * <p>
     * Use this option with caution in production environments.
     *
     * @param noThread whether to disable threading.
     * @return {@code this}
     */
    public VSimArgs noThread(boolean noThread) {
        this.noThread = Optional.of(noThread);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        count.ifPresent(Long -> args.add(CommandKeyword.COUNT).add(Long));

        explorationFactor.ifPresent(Long -> args.add(CommandKeyword.EF).add(Long));

        filter.ifPresent(s -> args.add(CommandKeyword.FILTER).add(s));

        filterEfficiency.ifPresent(Long -> args.add(CommandKeyword.FILTER_EF).add(Long));

        if (truth.isPresent() && truth.get()) {
            args.add(CommandKeyword.TRUTH);
        }

        if (noThread.isPresent() && noThread.get()) {
            args.add(CommandKeyword.NOTHREAD);
        }
    }

}
