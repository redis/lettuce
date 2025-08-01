/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Argument list builder for {@code FT.SEARCH}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 6.8
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/commands/ft.search/">FT.SEARCH</a>
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SearchArgs<K, V> {

    private Optional<Boolean> noContent = Optional.empty();

    private Optional<Boolean> verbatim = Optional.empty();

    private Optional<Boolean> noStopWords = Optional.empty();

    private Optional<Boolean> withScores = Optional.empty();

    private Optional<Boolean> withPayloads = Optional.empty();

    private Optional<Boolean> withSortKeys = Optional.empty();

    // FIXME verify if we need to support this, deprecated since 2.10
    // private List<NumericFilter<K, V>> filters = new ArrayList<>();

    // FIXME verify if we need to support this, deprecated since 2.6
    // private Optional<GeoFilter<K, V>> geoFilter = Optional.empty();

    private final List<K> inKeys = new ArrayList<>();

    private final List<K> inFields = new ArrayList<>();

    private final Map<K, Optional<K>> returnFields = new HashMap<>();

    private Optional<SummarizeArgs<K, V>> summarize = Optional.empty();

    private Optional<HighlightArgs<K, V>> highlight = Optional.empty();

    private OptionalLong slop = OptionalLong.empty();

    private Optional<Boolean> inOrder = Optional.empty();

    private Optional<DocumentLanguage> language = Optional.empty();

    private Optional<V> expander = Optional.empty();

    private Optional<ScoringFunction> scorer = Optional.empty();

    // FIXME verify if we want to support this
    // private Optional<Boolean> explainScore = Optional.empty();

    private Optional<V> payload = Optional.empty();

    private Optional<SortByArgs<K>> sortBy = Optional.empty();

    private Optional<Limit> limit = Optional.empty();

    private Optional<Duration> timeout = Optional.empty();

    private final Map<K, V> params = new HashMap<>();

    private QueryDialects dialect = QueryDialects.DIALECT2;

    /**
     * Used to build a new instance of the {@link SearchArgs}.
     *
     * @return a {@link SearchArgs.Builder} that provides the option to build up a new instance of the {@link SearchArgs}
     * @param <K> the key type
     * @param <V> the value type
     */
    public static <K, V> SearchArgs.Builder<K, V> builder() {
        return new SearchArgs.Builder<>();
    }

    /**
     * Builder for {@link SearchArgs}.
     * <p>
     * As a final step the {@link SearchArgs.Builder#build()} method needs to be executed to create the final {@link SearchArgs}
     * instance.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder<K, V> {

        private final SearchArgs<K, V> instance = new SearchArgs<>();

        /**
         * Build a new instance of the {@link SearchArgs}.
         *
         * @return a new instance of the {@link SearchArgs}
         */
        public SearchArgs<K, V> build() {
            return instance;
        }

        /**
         * Returns the document ids and not the content. This is useful if RediSearch is only an index on an external document
         * collection. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> noContent() {
            instance.noContent = Optional.of(true);
            return this;
        }

        /**
         * Do not try to use stemming for query expansion but searches the query terms verbatim. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> verbatim() {
            instance.verbatim = Optional.of(true);
            return this;
        }

        /**
         * Ignore any defined stop words in full text searches. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> noStopWords() {
            instance.noStopWords = Optional.of(true);
            return this;
        }

        /**
         * Return the relative internal score of each document. This can be used to merge results from multiple instances.
         * Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> withScores() {
            instance.withScores = Optional.of(true);
            return this;
        }

        /**
         * Retrieve optional document payloads. The payloads follow the document id and, if
         * {@link SearchArgs.Builder#withScores} is set, the scores. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
         */
        public SearchArgs.Builder<K, V> withPayloads() {
            instance.withPayloads = Optional.of(true);
            return this;
        }

        /**
         * Return the value of the sorting key, right after the id and score and/or payload, if requested. This is usually not
         * needed, and exists for distributed search coordination purposes. This option is relevant only if used in conjunction
         * with {@link SearchArgs.Builder#sortBy(SortByArgs)}. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> withSortKeys() {
            instance.withSortKeys = Optional.of(true);
            return this;
        }

        /**
         * Limit the result to a given set of keys specified in the list. Non-existent keys are ignored, unless all the keys are
         * non-existent.
         *
         * @param key the key to search in
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> inKey(K key) {
            instance.inKeys.add(key);
            return this;
        }

        /**
         * Filter the result to those appearing only in specific attributes of the document.
         *
         * @param field the field to search in
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> inField(K field) {
            instance.inFields.add(field);
            return this;
        }

        /**
         * Limit the attributes returned from the document. The field is either an attribute name (for hashes and JSON) or a
         * JSON Path expression (for JSON). <code>as</code> is the name of the field used in the result as an alias.
         *
         * @param field the field to return
         * @param as the alias to use for this field in the result
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> returnField(K field, K as) {
            instance.returnFields.put(field, Optional.ofNullable(as));
            return this;
        }

        /**
         * Limit the attributes returned from the document. The field is either an attribute name (for hashes and JSON) or a
         * JSON Path expression (for JSON).
         *
         * @param field the field to return
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> returnField(K field) {
            instance.returnFields.put(field, Optional.empty());
            return this;
        }

        /**
         * Return only the sections of the attribute that contain the matched text.
         *
         * @param summarizeFilter the summarization filter
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/highlight/">Highlighting</a>
         */
        public SearchArgs.Builder<K, V> summarize(SummarizeArgs<K, V> summarizeFilter) {
            instance.summarize = Optional.ofNullable(summarizeFilter);
            return this;
        }

        /**
         * Format occurrences of matched text.
         *
         * @param highlightFilter the highlighting filter
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/highlight/">Highlighting</a>
         */
        public SearchArgs.Builder<K, V> highlight(HighlightArgs<K, V> highlightFilter) {
            instance.highlight = Optional.ofNullable(highlightFilter);
            return this;
        }

        /**
         * Allow for a number of intermediate terms allowed to appear between the terms of the query. Suppose you're searching
         * for a phrase <code>hello world</code>, if some other terms appear in-between <code>hello</code> and
         * <code>world</code>, a SLOP greater than 0 allows for these text attributes to match. By default, there is no SLOP
         * constraint.
         *
         * @param slop the slop value how many intermediate terms are allowed
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> slop(long slop) {
            instance.slop = OptionalLong.of(slop);
            return this;
        }

        /**
         * Require the terms in the document to have the same order as the terms in the query, regardless of the offsets between
         * them. Typically used in conjunction with {@link SearchArgs.Builder#slop(long)}. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> inOrder() {
            instance.inOrder = Optional.of(true);
            return this;
        }

        /**
         * Specify the language of the query. This is used to stem the query terms. The default is
         * {@link DocumentLanguage#ENGLISH}.
         * <p/>
         * If this setting was specified as part of index creation, it doesn't need to be specified here.
         *
         * @param language the language of the query
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> language(DocumentLanguage language) {
            instance.language = Optional.ofNullable(language);
            return this;
        }

        /**
         * Use a custom query expander instead of the stemmer
         *
         * @param expander the query expander to use
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/administration/extensions/">Extensions</a>
         */
        public SearchArgs.Builder<K, V> expander(V expander) {
            instance.expander = Optional.ofNullable(expander);
            return this;
        }

        /**
         * Use a built-in or a user-provided scoring function
         *
         * @param scorer the {@link ScoringFunction} to use
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/administration/extensions/">Extensions</a>
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/scoring/">Scoring</a>
         */
        public SearchArgs.Builder<K, V> scorer(ScoringFunction scorer) {
            instance.scorer = Optional.ofNullable(scorer);
            return this;
        }

        // /**
        // * Return a textual description of how the scores were calculated. Using this option requires
        // * {@link Builder#withScores()}.
        // *
        // * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
        // */
        // public SearchArgs.Builder<K, V> explainScore() {
        // instance.explainScore = Optional.of(true);
        // return this;
        // }

        /**
         * Add an arbitrary, binary safe payload exposed to custom scoring functions.
         *
         * @param payload the payload to return
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/administration/extensions/">Extensions</a>
         */
        public SearchArgs.Builder<K, V> payload(V payload) {
            instance.payload = Optional.ofNullable(payload);
            return this;
        }

        /**
         * Order the results by the value of this attribute. This applies to both text and numeric attributes. Attributes needed
         * for SORTBY should be declared as SORTABLE in the index, to be available with very low latency.
         * <p/>
         * Note that this adds memory overhead.
         *
         * @param sortBy the {@link SortByArgs} to use
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> sortBy(SortByArgs<K> sortBy) {
            instance.sortBy = Optional.ofNullable(sortBy);
            return this;
        }

        /**
         * Limit the results to the offset and number of results given. Note that the offset is zero-indexed. The default is 0
         * 10, which returns 10 items starting from the first result. You can use LIMIT 0 0 to count the number of documents in
         * the result set without actually returning them.
         * <p/>
         * LIMIT behavior: If you use the LIMIT option without sorting, the results returned are non-deterministic, which means
         * that subsequent queries may return duplicated or missing values. Add SORTBY with a unique field, or use FT.AGGREGATE
         * with the WITHCURSOR option to ensure deterministic result set paging.
         *
         * @param offset the offset to use
         * @param number the limit to use
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> limit(long offset, long number) {
            instance.limit = Optional.of(new Limit(offset, number));
            return this;
        }

        /**
         * Override the maximum time to wait for the query to complete.
         *
         * @param timeout the timeout to use (with millisecond resolution)
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> timeout(Duration timeout) {
            instance.timeout = Optional.ofNullable(timeout);
            return this;
        }

        /**
         * Add one or more value parameters. Each parameter has a name and a value.
         * <p/>
         * Requires {@link QueryDialects#DIALECT2} or higher.
         *
         * @param name the name of the parameter
         * @param value the value of the parameter
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K, V> param(K name, V value) {
            instance.params.put(name, value);
            return this;
        }

        /**
         * Set the query dialect. The default is {@link QueryDialects#DIALECT2}.
         *
         * @param dialect the dialect to use
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see QueryDialects
         */
        public SearchArgs.Builder<K, V> dialect(QueryDialects dialect) {
            instance.dialect = dialect;
            return this;
        }

    }

    /**
     * Gets whether the NOCONTENT option is enabled.
     *
     * @return true if NOCONTENT is enabled, false otherwise
     */
    public boolean isNoContent() {
        return noContent.orElse(false);
    }

    /**
     * Gets whether the WITHSCORES option is enabled.
     *
     * @return true if WITHSCORES is enabled, false otherwise
     */
    public boolean isWithScores() {
        return withScores.orElse(false);
    }

    /**
     * Gets whether the WITHPAYLOADS option is enabled.
     *
     * @return true if WITHPAYLOADS is enabled, false otherwise
     */
    public boolean isWithPayloads() {
        return withPayloads.orElse(false);
    }

    /**
     * Gets whether the WITHSORTKEYS option is enabled.
     *
     * @return true if WITHSORTKEYS is enabled, false otherwise
     */
    public boolean isWithSortKeys() {
        return withSortKeys.orElse(false);
    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<K, V> args) {

        noContent.ifPresent(v -> args.add(CommandKeyword.NOCONTENT));
        verbatim.ifPresent(v -> args.add(CommandKeyword.VERBATIM));
        noStopWords.ifPresent(v -> args.add(CommandKeyword.NOSTOPWORDS));
        withScores.ifPresent(v -> args.add(CommandKeyword.WITHSCORES));
        withPayloads.ifPresent(v -> args.add(CommandKeyword.WITHPAYLOADS));
        withSortKeys.ifPresent(v -> args.add(CommandKeyword.WITHSORTKEYS));

        if (!inKeys.isEmpty()) {
            args.add(CommandKeyword.INKEYS);
            args.add(inKeys.size());
            args.addKeys(inKeys);
        }

        if (!inFields.isEmpty()) {
            args.add(CommandKeyword.INFIELDS);
            args.add(inFields.size());
            args.addKeys(inFields);
        }

        if (!returnFields.isEmpty()) {
            args.add(CommandKeyword.RETURN);
            args.add(returnFields.size());
            returnFields.forEach((field, as) -> {
                args.addKey(field);
                as.ifPresent(args::addKey);
            });
        }

        summarize.ifPresent(summarizeArgs -> {
            summarizeArgs.build(args);
        });

        highlight.ifPresent(highlightArgs -> {
            highlightArgs.build(args);
        });

        slop.ifPresent(v -> {
            args.add(CommandKeyword.SLOP);
            args.add(v);
        });

        timeout.ifPresent(timeoutDuration -> {
            args.add(CommandKeyword.TIMEOUT);
            args.add(timeoutDuration.toMillis());
        });

        inOrder.ifPresent(v -> args.add(CommandKeyword.INORDER));

        language.ifPresent(documentLanguage -> {
            args.add(CommandKeyword.LANGUAGE);
            args.add(documentLanguage.toString());
        });

        expander.ifPresent(v -> {
            args.add(CommandKeyword.EXPANDER);
            args.addValue(v);
        });

        scorer.ifPresent(scoringFunction -> {
            args.add(CommandKeyword.SCORER);
            args.add(scoringFunction.toString());
        });

        // explainScore.ifPresent(v -> args.add(CommandKeyword.EXPLAINSCORE));

        payload.ifPresent(v -> {
            args.add(CommandKeyword.PAYLOAD);
            args.addValue(v);
        });

        sortBy.ifPresent(sortByArgs -> {
            sortByArgs.build(args);
        });

        limit.ifPresent(limitArgs -> {
            args.add(CommandKeyword.LIMIT);
            args.add(limitArgs.offset);
            args.add(limitArgs.num);
        });

        if (!params.isEmpty()) {
            args.add(CommandKeyword.PARAMS);
            args.add(params.size() * 2L);
            params.forEach((name, value) -> {
                args.addKey(name);
                args.addValue(value);
            });
        }

        args.add(CommandKeyword.DIALECT);
        args.add(dialect.toString());
    }

    static class Limit {

        private final long offset;

        private final long num;

        Limit(long offset, long num) {
            this.offset = offset;
            this.num = num;
        }

    }

}
