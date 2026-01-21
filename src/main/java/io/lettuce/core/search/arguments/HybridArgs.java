/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Argument list builder for the Redis {@code FT.HYBRID} command. Combines text search and vector similarity search with
 * configurable combination strategies and post-processing operations.
 *
 * <h3>Basic Usage:</h3>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     HybridArgs<String, String> args = HybridArgs.<String, String> builder()
 *             .search(HybridSearchArgs.<String, String> builder().query("comfortable shoes").build())
 *             .vectorSearch(HybridVectorArgs.<String, String> builder().field("@embedding").vector(vectorBlob)
 *                     .method(HybridVectorArgs.Knn.of(10)).build())
 *             .combine(CombineArgs.of(new CombineArgs.RRF<>())).build();
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see <a href="https://redis.io/docs/latest/commands/ft.hybrid/">FT.HYBRID</a>
 * @see HybridSearchArgs
 * @see HybridVectorArgs
 * @see CombineArgs
 * @see PostProcessingArgs
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class HybridArgs<K, V> {

    private final List<HybridSearchArgs<K, V>> searchArgs = new ArrayList<>();

    private final List<HybridVectorArgs<K, V>> vectorArgs = new ArrayList<>();

    private Optional<CombineArgs<K>> combineArgs = Optional.empty();

    private Optional<PostProcessingArgs<K, V>> postProcessingArgs = Optional.empty();

    private final Map<K, V> params = new HashMap<>();

    private Optional<Duration> timeout = Optional.empty();

    /**
     * @return a new {@link Builder} for {@link HybridArgs}.
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link HybridArgs}.
     */
    public static class Builder<K, V> {

        private final HybridArgs<K, V> instance = new HybridArgs<>();

        /**
         * Build the {@link HybridArgs} instance.
         *
         * @return the configured arguments
         */
        public HybridArgs<K, V> build() {
            return instance;
        }

        /**
         * Configure the SEARCH clause using {@link HybridSearchArgs}.
         *
         * @param searchArgs the search arguments
         * @return this builder
         */
        public Builder<K, V> search(HybridSearchArgs<K, V> searchArgs) {
            LettuceAssert.notNull(searchArgs, "Search args must not be null");
            instance.searchArgs.add(searchArgs);
            return this;
        }

        /**
         * Configure the VSIM clause using {@link HybridVectorArgs}.
         *
         * @param vectorArgs the vector search arguments
         * @return this builder
         */
        public Builder<K, V> vectorSearch(HybridVectorArgs<K, V> vectorArgs) {
            LettuceAssert.notNull(vectorArgs, "Vector args must not be null");
            instance.vectorArgs.add(vectorArgs);
            return this;
        }

        /**
         * Configure the COMBINE clause using {@link CombineArgs}.
         *
         * @param combineArgs the combine arguments
         * @return this builder
         */
        public Builder<K, V> combine(CombineArgs<K> combineArgs) {
            LettuceAssert.notNull(combineArgs, "Combine args must not be null");
            instance.combineArgs = Optional.of(combineArgs);
            return this;
        }

        /**
         * Set the post-processing arguments.
         *
         * @param postProcessingArgs the post-processing configuration
         * @return this builder
         */
        public Builder<K, V> postProcessing(PostProcessingArgs<K, V> postProcessingArgs) {
            LettuceAssert.notNull(postProcessingArgs, "PostProcessingArgs must not be null");
            instance.postProcessingArgs = Optional.of(postProcessingArgs);
            return this;
        }

        /**
         * Add a parameter for parameterized queries.
         * <p>
         * Parameters can be referenced in queries using {@code $name} syntax.
         * </p>
         *
         * @param name the parameter name
         * @param value the parameter value
         * @return this builder
         */
        public Builder<K, V> param(K name, V value) {
            LettuceAssert.notNull(name, "Parameter name must not be null");
            LettuceAssert.notNull(value, "Parameter value must not be null");
            instance.params.put(name, value);
            return this;
        }

        /**
         * Add a parameter for parameterized queries with byte array value.
         * <p>
         * This overload allows passing binary parameter data (e.g., vector embeddings) when using String codec. Parameters can
         * be referenced in queries using {@code $name} syntax.
         * </p>
         *
         * @param name the parameter name
         * @param value the parameter value as byte array
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public Builder<K, V> param(K name, byte[] value) {
            LettuceAssert.notNull(name, "Parameter name must not be null");
            LettuceAssert.notNull(value, "Parameter value must not be null");
            instance.params.put(name, (V) value);
            return this;
        }

        /**
         * Set the maximum time to wait for the query to complete.
         *
         * @param timeout the timeout duration (with millisecond resolution)
         * @return this builder
         */
        public Builder<K, V> timeout(Duration timeout) {
            LettuceAssert.notNull(timeout, "Timeout must not be null");
            instance.timeout = Optional.of(timeout);
            return this;
        }

    }

    /**
     * Build the command arguments for the configured {@link HybridArgs}.
     * <p>
     * Command structure: SEARCH [SCORER] [YIELD_SCORE_AS] VSIM [KNN/RANGE] [FILTER]* [YIELD_DISTANCE_AS] [COMBINE]
     * [YIELD_SCORE_AS] [SORTBY] [FILTER]* [LIMIT] [RETURN] [LOAD] [PARAMS]
     * </p>
     *
     * @param args the {@link CommandArgs} to append to
     */
    public void build(CommandArgs<K, V> args) {
        // Both SEARCH and VSIM must be configured (per PRD)
        LettuceAssert.notNull(searchArgs, "SEARCH clause is required - use search() or search(HybridSearchArgs)");
        LettuceAssert.notNull(vectorArgs, "VSIM clause is required - use vectorSearch() or vectorSearch(HybridVectorArgs)");

        // SEARCH clause
        searchArgs.forEach(searchArg -> searchArg.build(args));

        // VSIM clause
        vectorArgs.forEach(vectorArg -> vectorArg.build(args));

        // COMBINE clause
        if (combineArgs.isPresent()) {
            args.add(CommandKeyword.COMBINE);
            combineArgs.get().build(args);
        }

        // Post-processing operations (LOAD, GROUPBY, APPLY, SORTBY, FILTER, LIMIT)
        postProcessingArgs.ifPresent(postProcessing -> postProcessing.build(args));

        // PARAMS clause
        if (!params.isEmpty()) {
            args.add(CommandKeyword.PARAMS);
            args.add(params.size() * 2L);
            params.forEach((name, value) -> {
                args.addKey(name);
                // Handle byte[] values specially (they're always binary data)
                if (value instanceof byte[]) {
                    args.add((byte[]) value);
                } else {
                    args.addValue(value);
                }
            });
        }

        // TIMEOUT clause
        timeout.ifPresent(t -> {
            args.add(CommandKeyword.TIMEOUT);
            args.add(t.toMillis());
        });

    }

}
