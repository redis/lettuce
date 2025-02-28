/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

import java.util.Optional;

import static io.lettuce.core.protocol.CommandKeyword.*;

/**
 * Field arguments for TEXT fields in a RediSearch index.
 * <p>
 * Text fields are specifically designed for storing human language text. When indexing text fields, Redis performs several
 * transformations to optimize search capabilities. The text is transformed to lowercase, allowing case-insensitive searches.
 * The data is tokenized, meaning it is split into individual words or tokens, which enables efficient full-text search
 * functionality.
 *
 * @param <K> Key type
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#text-fields">Text
 *      Fields</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TextFieldArgs<K> extends FieldArgs<K> {

    /**
     * Phonetic matchers for text fields.
     */
    public enum PhoneticMatcher {

        ENGLISH("dm:en"), FRENCH("dm:fr"), PORTUGUESE("dm:pt"), SPANISH("dm:es");

        private final String matcher;

        PhoneticMatcher(String matcher) {
            this.matcher = matcher;
        }

        public String getMatcher() {
            return matcher;
        }

    }

    private Optional<Long> weight = Optional.empty();

    private boolean noStem;

    private Optional<PhoneticMatcher> phonetic = Optional.empty();

    private boolean withSuffixTrie;

    /**
     * Create a new {@link TextFieldArgs} using the builder pattern.
     * 
     * @param <K> Key type
     * @return a new {@link Builder}
     */
    public static <K> Builder<K> builder() {
        return new Builder<>();
    }

    @Override
    public String getFieldType() {
        return "TEXT";
    }

    /**
     * Get the weight of the field.
     *
     * @return the weight
     */
    public Optional<Long> getWeight() {
        return weight;
    }

    /**
     * Check if stemming is disabled.
     *
     * @return true if stemming is disabled
     */
    public boolean isNoStem() {
        return noStem;
    }

    /**
     * Get the phonetic matcher.
     *
     * @return the phonetic matcher
     */
    public Optional<PhoneticMatcher> getPhonetic() {
        return phonetic;
    }

    /**
     * Check if suffix trie is enabled.
     *
     * @return true if suffix trie is enabled
     */
    public boolean isWithSuffixTrie() {
        return withSuffixTrie;
    }

    @Override
    protected void buildTypeSpecificArgs(CommandArgs<K, ?> args) {
        weight.ifPresent(w -> args.add(WEIGHT).add(w));
        if (noStem) {
            args.add(NOSTEM);
        }
        phonetic.ifPresent(p -> args.add(PHONETIC).add(p.getMatcher()));
        if (withSuffixTrie) {
            args.add(WITHSUFFIXTRIE);
        }
    }

    /**
     * Builder for {@link TextFieldArgs}.
     * 
     * @param <K> Key type
     */
    public static class Builder<K> extends FieldArgs.Builder<K, TextFieldArgs<K>, Builder<K>> {

        public Builder() {
            super(new TextFieldArgs<>());
        }

        /**
         * The weight of the field. Works with TEXT attributes, declares the importance of this attribute when calculating
         * result accuracy. This is a multiplication factor. The default weight is 1.
         *
         * @param weight the weight of the field
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> weight(long weight) {
            instance.weight = Optional.of(weight);
            return self();
        }

        /**
         * By default, the index applies stemming to TEXT fields. If you don't want to apply stemming to the field, you can use
         * the NOSTEM argument. This may be ideal for things like proper names.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> noStem() {
            instance.noStem = true;
            return self();
        }

        /**
         * Phonetic matching is a feature that allows you to search for similar-sounding words. For example, a search for
         * "Smith" will also return results for "Smyth". Phonetic matching is language-specific, and you can specify the
         * language using the PHONETIC argument.
         * <p/>
         * The following languages are supported:
         * <ul>
         * <li>ENGLISH</li>
         * <li>FRENCH</li>
         * <li>PORTUGUESE</li>
         * <li>SPANISH</li>
         * </ul>
         *
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/phonetic_matching/">Phonetic
         *      Matching</a>
         * @param matcher the phonetic matcher
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> phonetic(PhoneticMatcher matcher) {
            instance.phonetic = Optional.of(matcher);
            return self();
        }

        /**
         * For TEXT attributes, keeps a suffix trie with all terms which match the suffix. It is used to optimize contains
         * (*foo*) and suffix (*foo) queries. Otherwise, a brute-force search on the trie is performed. If the suffix trie
         * exists for some fields, these queries will be disabled for other fields.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> withSuffixTrie() {
            instance.withSuffixTrie = true;
            return self();
        }

    }

}
