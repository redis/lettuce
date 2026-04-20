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
 * Argument list builder for {@code FT.SEARCH}.
 *
 * @param <K> Key type.
 * @since 6.8
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/commands/ft.search/">FT.SEARCH</a>
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SearchArgs<K> {

    private boolean noContent = false;

    private boolean verbatim = false;

    private boolean withScores = false;

    private boolean withSortKeys = false;

    private final List<K> inKeys = new ArrayList<>();

    private final List<K> inFields = new ArrayList<>();

    private final Map<K, Optional<K>> returnFields = new HashMap<>();

    private Optional<SummarizeArgs<K>> summarize = Optional.empty();

    private Optional<HighlightArgs<K>> highlight = Optional.empty();

    private Long slop;

    private boolean inOrder = false;

    private Optional<DocumentLanguage> language = Optional.empty();

    private Optional<String> expander = Optional.empty();

    private Optional<ScoringFunction> scorer = Optional.empty();

    private Optional<SortByArgs> sortBy = Optional.empty();

    private Optional<Limit> limit = Optional.empty();

    private Optional<Duration> timeout = Optional.empty();

    private final Map<String, Object> params = new HashMap<>();

    private QueryDialects dialect = QueryDialects.DIALECT2;

    /**
     * Used to build a new instance of the {@link SearchArgs}.
     *
     * @return a {@link SearchArgs.Builder} that provides the option to build up a new instance of the {@link SearchArgs}
     * @param <K> the key type
     */
    public static <K> SearchArgs.Builder<K> builder() {
        return new SearchArgs.Builder<>();
    }

    /**
     * Builder for {@link SearchArgs}.
     * <p>
     * As a final step the {@link SearchArgs.Builder#build()} method needs to be executed to create the final {@link SearchArgs}
     * instance.
     *
     * @param <K> the key type
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder<K> {

        private final SearchArgs<K> instance = new SearchArgs<>();

        private SummarizeArgs.Builder<K> summarizeArgs;

        private HighlightArgs.Builder<K> highlightArgs;

        /**
         * Build a new instance of the {@link SearchArgs}.
         *
         * @return a new instance of the {@link SearchArgs}
         */
        public SearchArgs<K> build() {
            if (!instance.summarize.isPresent() && summarizeArgs != null) {
                instance.summarize = Optional.of(summarizeArgs.build());
            }

            if (!instance.highlight.isPresent() && highlightArgs != null) {
                instance.highlight = Optional.of(highlightArgs.build());
            }

            return instance;
        }

        /**
         * Returns the document ids and not the content. This is useful if RediSearch is only an index on an external document
         * collection. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> noContent() {
            instance.noContent = true;
            return this;
        }

        /**
         * Do not try to use stemming for query expansion but searches the query terms verbatim. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> verbatim() {
            instance.verbatim = true;
            return this;
        }

        /**
         * Return the relative internal score of each document. This can be used to merge results from multiple instances.
         * Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> withScores() {
            instance.withScores = true;
            return this;
        }

        /**
         * Return the value of the sorting key, right after the id and score and/or payload, if requested. This is usually not
         * needed, and exists for distributed search coordination purposes. This option is relevant only if used in conjunction
         * with {@link SearchArgs.Builder#sortBy(SortByArgs)}. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> withSortKeys() {
            instance.withSortKeys = true;
            return this;
        }

        /**
         * Limit the result to a given set of keys specified in the list. Non-existent keys are ignored, unless all the keys are
         * non-existent.
         *
         * @param key the key to search in
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> inKey(K key) {
            instance.inKeys.add(key);
            return this;
        }

        /**
         * Filter the result to those appearing only in specific attributes of the document.
         *
         * @param field the field to search in
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> inField(K field) {
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
        public SearchArgs.Builder<K> returnField(K field, K as) {
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
        public SearchArgs.Builder<K> returnField(K field) {
            instance.returnFields.put(field, Optional.empty());
            return this;
        }

        /**
         * Return only the sections of the attribute that contain the matched text.
         *
         * @param summarizeFilter the summarization filter
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#summarization">Summarization</a>
         */
        public SearchArgs.Builder<K> summarizeArgs(SummarizeArgs<K> summarizeFilter) {
            instance.summarize = Optional.ofNullable(summarizeFilter);
            return this;
        }

        /**
         * Convenience method to build {@link SummarizeArgs}
         * <p>
         * Add a field to summarize. Each field is summarized. If no FIELDS directive is passed, then all returned fields are
         * summarized.
         *
         * @param field the field to add
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#summarization">Summarization</a>
         */
        public SearchArgs.Builder<K> summarizeField(K field) {
            if (summarizeArgs == null) {
                summarizeArgs = new SummarizeArgs.Builder<>();
            }

            summarizeArgs.field(field);

            return this;
        }

        /**
         * Convenience method to build {@link SummarizeArgs}
         * <p>
         * Set the number of context words each fragment should contain. Context words surround the found term. A higher value
         * will return a larger block of text. If not specified, the default value is 20.
         *
         * @param len the field to add
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#summarization">Summarization</a>
         */
        public SearchArgs.Builder<K> summarizeLen(long len) {
            if (summarizeArgs == null) {
                summarizeArgs = new SummarizeArgs.Builder<>();
            }

            summarizeArgs.len(len);

            return this;
        }

        /**
         * Convenience method to build {@link SummarizeArgs}
         * <p>
         * The string used to divide individual summary snippets. The default is <code>...</code> which is common among search
         * engines, but you may override this with any other string if you desire to programmatically divide the snippets later
         * on. You may also use a newline sequence, as newlines are stripped from the result body during processing.
         *
         * @param separator the separator between fragments
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#summarization">Summarization</a>
         */
        public SearchArgs.Builder<K> summarizeSeparator(String separator) {
            if (summarizeArgs == null) {
                summarizeArgs = new SummarizeArgs.Builder<>();
            }

            summarizeArgs.separator(separator);

            return this;
        }

        /**
         * Convenience method to build {@link SummarizeArgs}
         * <p>
         * Set the number of fragments to be returned. If not specified, the default is 3.
         *
         * @param fragments the number of fragments to return
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#summarization">Summarization</a>
         */
        public SearchArgs.Builder<K> summarizeFragments(long fragments) {
            if (summarizeArgs == null) {
                summarizeArgs = new SummarizeArgs.Builder<>();
            }

            summarizeArgs.fragments(fragments);

            return this;
        }

        /**
         * Format occurrences of matched text.
         *
         * @param highlightFilter the highlighting filter
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#highlighting">Highlighting</a>
         */
        public SearchArgs.Builder<K> highlightArgs(HighlightArgs<K> highlightFilter) {
            instance.highlight = Optional.ofNullable(highlightFilter);
            return this;
        }

        /**
         * Convenience method to build {@link HighlightArgs}
         * <p>
         * Add a field to highlight. If no FIELDS directive is passed, then all returned fields are highlighted.
         *
         * @param field the field to summarize
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#highlighting">Highlighting</a>
         */
        public SearchArgs.Builder<K> highlightField(K field) {
            if (highlightArgs == null) {
                highlightArgs = new HighlightArgs.Builder<>();
            }

            highlightArgs.field(field);

            return this;
        }

        /**
         * Convenience method to build {@link HighlightArgs}
         * <p>
         * Tags to surround the matched terms with. If no TAGS are specified, a built-in tag pair is prepended and appended to
         * each matched term.
         *
         * @param startTag the string is prepended to each matched term
         * @param endTag the string is appended to each matched term
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/highlight/#highlighting">Highlighting</a>
         */
        public SearchArgs.Builder<K> highlightTags(String startTag, String endTag) {
            if (highlightArgs == null) {
                highlightArgs = new HighlightArgs.Builder<>();
            }

            highlightArgs.tags(startTag, endTag);

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
        public SearchArgs.Builder<K> slop(long slop) {
            instance.slop = slop;
            return this;
        }

        /**
         * Require the terms in the document to have the same order as the terms in the query, regardless of the offsets between
         * them. Typically used in conjunction with {@link SearchArgs.Builder#slop(long)}. Disabled by default.
         *
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> inOrder() {
            instance.inOrder = true;
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
        public SearchArgs.Builder<K> language(DocumentLanguage language) {
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
        public SearchArgs.Builder<K> expander(String expander) {
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
        public SearchArgs.Builder<K> scorer(ScoringFunction scorer) {
            instance.scorer = Optional.ofNullable(scorer);
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
        public SearchArgs.Builder<K> sortBy(SortByArgs sortBy) {
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
        public SearchArgs.Builder<K> limit(long offset, long number) {
            instance.limit = Optional.of(new Limit(offset, number));
            return this;
        }

        /**
         * Override the maximum time to wait for the query to complete.
         *
         * @param timeout the timeout to use (with millisecond resolution)
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         */
        public SearchArgs.Builder<K> timeout(Duration timeout) {
            instance.timeout = Optional.ofNullable(timeout);
            return this;
        }

        /**
         * Add a value parameter for parameterized queries.
         * <p>
         * Defines a parameter that can be referenced in the query using {@code $name}. Requires {@link QueryDialects#DIALECT2}
         * or higher.
         * </p>
         *
         * @param name the parameter name (referenced as $name in query)
         * @param value the parameter value
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see #param(String, byte[])
         */
        public SearchArgs.Builder<K> param(String name, String value) {
            LettuceAssert.notNull(name, "Parameter name must not be null");
            LettuceAssert.notNull(value, "Parameter value must not be null");
            instance.params.put(name, value);
            return this;
        }

        /**
         * Add a binary parameter for parameterized queries.
         * <p>
         * Use this overload for binary values such as vector embeddings that cannot be represented as a string. Parameters can
         * be referenced in queries using {@code $name} syntax, same as the string variant. Requires
         * {@link QueryDialects#DIALECT2} or higher.
         * </p>
         *
         * @param name the parameter name (referenced as $name in query)
         * @param value the binary parameter value (e.g., a serialized vector embedding)
         * @return the instance of the current {@link SearchArgs.Builder} for the purpose of method chaining
         * @see #param(String, String)
         */
        public SearchArgs.Builder<K> param(String name, byte[] value) {
            LettuceAssert.notNull(name, "Parameter name must not be null");
            LettuceAssert.notNull(value, "Parameter value must not be null");
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
        public SearchArgs.Builder<K> dialect(QueryDialects dialect) {
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
        return noContent;
    }

    /**
     * Gets whether the WITHSCORES option is enabled.
     *
     * @return true if WITHSCORES is enabled, false otherwise
     */
    public boolean isWithScores() {
        return withScores;
    }

    /**
     * Gets whether the WITHSORTKEYS option is enabled.
     *
     * @return true if WITHSORTKEYS is enabled, false otherwise
     */
    public boolean isWithSortKeys() {
        return withSortKeys;
    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<K, ?> args) {

        if (noContent) {
            args.add(CommandKeyword.NOCONTENT);
        }

        if (verbatim) {
            args.add(CommandKeyword.VERBATIM);
        }

        if (withScores) {
            args.add(CommandKeyword.WITHSCORES);
        }

        if (withSortKeys) {
            args.add(CommandKeyword.WITHSORTKEYS);
        }

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
            // Count total number of field specifications (field + optional AS + alias)
            int count = returnFields.size();

            // Add 2 for each "AS" keyword and alias value
            count += (int) (returnFields.values().stream().filter(Optional::isPresent).count() * 2);

            args.add(count);
            returnFields.forEach((field, as) -> {
                args.addKey(field);
                if (as.isPresent()) {
                    args.add(CommandKeyword.AS);
                    args.addKey(as.get());
                }
            });
        }

        summarize.ifPresent(summarizeArgs -> summarizeArgs.build(args));
        highlight.ifPresent(highlightArgs -> highlightArgs.build(args));

        if (slop != null) {
            args.add(CommandKeyword.SLOP);
            args.add(slop);
        }

        timeout.ifPresent(timeoutDuration -> {
            args.add(CommandKeyword.TIMEOUT);
            args.add(timeoutDuration.toMillis());
        });

        if (inOrder) {
            args.add(CommandKeyword.INORDER);
        }

        language.ifPresent(documentLanguage -> {
            args.add(CommandKeyword.LANGUAGE);
            args.add(documentLanguage.toString());
        });

        expander.ifPresent(v -> {
            args.add(CommandKeyword.EXPANDER);
            args.add(v);
        });

        scorer.ifPresent(scoringFunction -> {
            args.add(CommandKeyword.SCORER);
            args.add(scoringFunction.toString());
        });

        sortBy.ifPresent(sortByArgs -> sortByArgs.build(args));

        limit.ifPresent(limitArgs -> {
            args.add(CommandKeyword.LIMIT);
            args.add(limitArgs.offset);
            args.add(limitArgs.num);
        });

        if (!params.isEmpty()) {
            args.add(CommandKeyword.PARAMS);
            args.add(params.size() * 2L);
            params.forEach((name, value) -> {
                args.add(name);
                if (value instanceof byte[]) {
                    args.add((byte[]) value);
                } else {
                    args.add((String) value);
                }
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
