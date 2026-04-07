/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

import static io.lettuce.core.protocol.CommandKeyword.*;

/**
 * Argument list builder for {@code FT.CREATE}.
 *
 * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CreateArgs {

    /**
     * Possible target types for the index.
     */
    public enum TargetType {
        HASH, JSON
    }

    private Optional<TargetType> on = Optional.of(TargetType.HASH);

    private final List<String> prefixes = new ArrayList<>();

    private Optional<String> filter = Optional.empty();

    private Optional<DocumentLanguage> defaultLanguage = Optional.empty();

    private Optional<String> languageField = Optional.empty();

    private OptionalDouble defaultScore = OptionalDouble.empty();

    private Optional<String> scoreField = Optional.empty();

    private Optional<String> payloadField = Optional.empty();

    private boolean maxTextFields = false;

    private OptionalLong temporary = OptionalLong.empty();

    private boolean noOffsets = false;

    private boolean noHighlight = false;

    private boolean noFields = false;

    private boolean noFrequency = false;

    private boolean skipInitialScan = false;

    private Optional<List<String>> stopWords = Optional.empty();

    /**
     * Used to build a new instance of the {@link CreateArgs}.
     *
     * @return a {@link Builder} that provides the option to build up a new instance of the {@link CreateArgs}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CreateArgs}.
     * <p>
     * As a final step the {@link Builder#build()} method needs to be executed to create the final {@link CreateArgs} instance.
     *
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder {

        private final CreateArgs instance = new CreateArgs();

        /**
         * Set the {@link TargetType} type for the index. Defaults to {@link TargetType#HASH}.
         * 
         * @param targetType the target type
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder on(TargetType targetType) {
            instance.on = Optional.of(targetType);
            return this;
        }

        /**
         * Add a prefix to the index. You can add several prefixes to index. Default setting is * (all keys).
         * 
         * @param prefix the prefix
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder withPrefix(String prefix) {
            instance.prefixes.add(prefix);
            return this;
        }

        /**
         * Add a list of prefixes to the index. You can add several prefixes to index. Default setting is * (all keys).
         * 
         * @param prefixes a {@link List} of prefixes
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder withPrefixes(List<String> prefixes) {
            instance.prefixes.addAll(prefixes);
            return this;
        }

        /**
         * Set a filter for the index. Default setting is to have no filter.
         * <p/>
         * It is possible to use @__key to access the key that was just added/changed. A field can be used to set field name by
         * passing 'FILTER @indexName=="myindexname"'.
         * 
         * @param filter a filter expression with the full RediSearch aggregation expression language
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/query/">RediSearch Query</a>
         */
        public Builder filter(String filter) {
            instance.filter = Optional.of(filter);
            return this;
        }

        /**
         * Set the default language for the documents in the index. The default setting is English.
         * 
         * @param language the default language
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder defaultLanguage(DocumentLanguage language) {
            instance.defaultLanguage = Optional.of(language);
            return this;
        }

        /**
         * Set the field that contains the language setting for the documents in the index. The default setting is to have no
         * language field.
         * 
         * @param field the language field
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/stemming/">Stemming</a>
         */
        public Builder languageField(String field) {
            instance.languageField = Optional.of(field);
            return this;
        }

        /**
         * Set the default score for the documents in the index. The default setting is 1.0.
         * 
         * @param score the default score
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/scoring/">Scoring</a>
         */
        public Builder defaultScore(double score) {
            instance.defaultScore = OptionalDouble.of(score);
            return this;
        }

        /**
         * Set the field that contains the score setting for the documents in the index. The default setting is a score of 1.0.
         * 
         * @param field the score field
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/scoring/">Scoring</a>
         */
        public Builder scoreField(String field) {
            instance.scoreField = Optional.of(field);
            return this;
        }

        /**
         * Set the field that contains the payload setting for the documents in the index. The default setting is to have no
         * payload field.
         * <p/>
         * This should be a document attribute that you use as a binary safe payload string to the document that can be
         * evaluated at query time by a custom scoring function or retrieved to the client
         * 
         * @param field the payload field
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/scoring/">Scoring</a>
         */
        public Builder payloadField(String field) {
            instance.payloadField = Optional.of(field);
            return this;
        }

        /**
         * Set the maximum number of text fields in the index. The default setting is to have no limit.
         * <p/>
         * Forces RediSearch to encode indexes as if there were more than 32 text attributes, which allows you to add additional
         * attributes (beyond 32) using FT.ALTER. For efficiency, RediSearch encodes indexes differently if they are created
         * with less than 32 text attributes.
         *
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder maxTextFields() {
            instance.maxTextFields = true;
            return this;
        }

        /**
         * Set the temporary index expiration time in seconds. The default setting is to have no expiration time.
         * <p/>
         * Creates a lightweight temporary index that expires after a specified period of inactivity, in seconds. The internal
         * idle timer is reset whenever the index is searched or added to. Because such indexes are lightweight, you can create
         * thousands of such indexes without negative performance implications and, therefore, you should consider using
         * {@link Builder#skipInitialScan()} to avoid costly scanning.
         * <p/>
         * Warning: When temporary indexes expire, they drop all the records associated with them. FT.DROPINDEX was introduced
         * with a default of not deleting docs and a DD flag that enforced deletion. However, for temporary indexes, documents
         * are deleted along with the index. Historically, RediSearch used an FT.ADD command, which made a connection between
         * the document and the index. Then, FT.DROP, also a hystoric command, deleted documents by default. In version 2.x,
         * RediSearch indexes hashes and JSONs, and the dependency between the index and documents no longer exists.
         * 
         * @param seconds the temporary index expiration time in seconds
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder temporary(long seconds) {
            instance.temporary = OptionalLong.of(seconds);
            return this;
        }

        /**
         * Set the no offsets flag. The default setting is to have offsets.
         * <p/>
         * It saves memory, but does not allow exact searches or highlighting. It implies {@link Builder#noHighlighting()} is
         * set to true.
         *
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder noOffsets() {
            instance.noOffsets = true;
            return this;
        }

        /**
         * Set the no highlighting flag. The default setting is to have highlighting.
         * <p/>
         * Conserves storage space and memory by disabling highlighting support. If set, the corresponding byte offsets for term
         * positions are not stored. NOHL is also implied by NOOFFSETS.
         *
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder noHighlighting() {
            instance.noHighlight = true;
            return this;
        }

        /**
         * Set the no fields flag. The default setting is to have fields.
         * <p/>
         * Does not store attribute bits for each term. It saves memory, but it does not allow filtering by specific attributes.
         *
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder noFields() {
            instance.noFields = true;
            return this;
        }

        /**
         * Set the no frequency flag. The default setting is to have frequencies.
         * <p/>
         * Does not store the frequency of each term. It saves memory, but it does not allow sorting by frequency of a given
         * term.
         *
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder noFrequency() {
            instance.noFrequency = true;
            return this;
        }

        /**
         * Set the skip initial scan flag. The default setting is to scan initially.
         *
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         */
        public Builder skipInitialScan() {
            instance.skipInitialScan = true;
            return this;
        }

        /**
         * Set the index with a custom stopword list, to be ignored during indexing and search time.
         * <p/>
         * If not set, FT.CREATE takes the default list of stopwords. If {count} is set to 0, the index does not have stopwords.
         *
         * @param stopWords a list of stop words
         * @return the instance of the current {@link Builder} for the purpose of method chaining
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/stopwords/">Stop
         *      words</a>
         */
        public Builder stopWords(List<String> stopWords) {
            instance.stopWords = Optional.of(stopWords);
            return this;
        }

        public CreateArgs build() {
            return instance;
        }

    }

    /**
     * Get the target type for the index.
     *
     * @return the target type
     * @see TargetType
     * @see Builder#on(TargetType)
     */
    public Optional<TargetType> getOn() {
        return on;
    }

    /**
     * Get the prefixes for the index.
     *
     * @return the prefixes
     * @see Builder#withPrefix(String)
     * @see Builder#withPrefixes(List)
     */
    public List<String> getPrefixes() {
        return prefixes;
    }

    /**
     * Get the filter for the index.
     *
     * @return the filter
     * @see Builder#filter(String)
     */
    public Optional<String> getFilter() {
        return filter;
    }

    /**
     * Get the default language for the documents in the index.
     *
     * @return the default language
     * @see Builder#defaultLanguage(DocumentLanguage)
     */
    public Optional<DocumentLanguage> getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Get the field that contains the language setting for the documents in the index.
     *
     * @return the language field
     * @see Builder#languageField(String)
     */
    public Optional<String> getLanguageField() {
        return languageField;
    }

    /**
     * Get the default score for the documents in the index.
     *
     * @return the default score
     * @see Builder#defaultScore(double)
     */
    public OptionalDouble getDefaultScore() {
        return defaultScore;
    }

    /**
     * Get the field that contains the score setting for the documents in the index.
     *
     * @return the score field
     * @see Builder#scoreField(String)
     */
    public Optional<String> getScoreField() {
        return scoreField;
    }

    /**
     * Get the field that contains the payload setting for the documents in the index.
     *
     * @return the payload field
     * @see Builder#payloadField(String)
     */
    public Optional<String> getPayloadField() {
        return payloadField;
    }

    /**
     * Get the maximum number of text fields in the index.
     *
     * @return the maximum number of text fields
     * @see Builder#maxTextFields()
     */
    public boolean isMaxTextFields() {
        return maxTextFields;
    }

    /**
     * Get the temporary index expiration time in seconds.
     *
     * @return the temporary index expiration time in seconds
     * @see Builder#temporary(long)
     */
    public OptionalLong getTemporary() {
        return temporary;
    }

    /**
     * Get the no offsets flag.
     *
     * @return the no offsets flag
     * @see Builder#noOffsets()
     */
    public boolean isNoOffsets() {
        return noOffsets;
    }

    /**
     * Get the no highlighting flag.
     *
     * @return the no highlighting flag
     * @see Builder#noHighlighting()
     */
    public boolean isNoHighlight() {
        return noHighlight;
    }

    /**
     * Get the no fields flag.
     *
     * @return the no fields flag
     * @see Builder#noFields()
     */
    public boolean isNoFields() {
        return noFields;
    }

    /**
     * Get the no frequency flag.
     *
     * @return the no frequency flag
     * @see Builder#noFrequency()
     */
    public boolean isNoFrequency() {
        return noFrequency;
    }

    /**
     * Get the skip initial scan flag.
     *
     * @return the skip initial scan flag
     * @see Builder#skipInitialScan()
     */
    public boolean isSkipInitialScan() {
        return skipInitialScan;
    }

    /**
     * Get the stop words for the index.
     *
     * @return the stop words
     * @see Builder#stopWords(List)
     */
    public Optional<List<String>> getStopWords() {
        return stopWords;
    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<?, ?> args) {
        on.ifPresent(targetType -> args.add(ON).add(targetType.name()));
        if (!prefixes.isEmpty()) {
            args.add(PREFIX).add(prefixes.size());
            prefixes.forEach(args::add);
        }
        filter.ifPresent(filter -> args.add(FILTER).add(filter));
        defaultLanguage.ifPresent(language -> args.add(LANGUAGE).add(language.toString()));
        languageField.ifPresent(field -> args.add(LANGUAGE_FIELD).add(field));
        defaultScore.ifPresent(score -> args.add(SCORE).add(score));
        scoreField.ifPresent(field -> args.add(SCORE_FIELD).add(field));
        payloadField.ifPresent(field -> args.add(PAYLOAD_FIELD).add(field));
        if (maxTextFields) {
            args.add(MAXTEXTFIELDS);
        }
        temporary.ifPresent(seconds -> args.add(TEMPORARY).add(seconds));
        if (noOffsets) {
            args.add(NOOFFSETS);
        }
        if (noHighlight) {
            args.add(NOHL);
        }
        if (noFields) {
            args.add(NOFIELDS);
        }
        if (noFrequency) {
            args.add(NOFREQS);
        }
        if (skipInitialScan) {
            args.add(SKIPINITIALSCAN);
        }
        stopWords.ifPresent(words -> {
            args.add(STOPWORDS).add(words.size());
            words.forEach(args::add);
        });
    }

}
