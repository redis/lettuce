/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

import java.util.Optional;

import static io.lettuce.core.protocol.CommandKeyword.*;

/**
 * Field arguments for TAG fields in a RediSearch index.
 * <p>
 * Tag fields are used to store textual data that represents a collection of data tags or labels. Tag fields are characterized
 * by their low cardinality, meaning they typically have a limited number of distinct values. Unlike text fields, tag fields are
 * stored as-is without tokenization or stemming. They are useful for organizing and categorizing data, making it easier to
 * filter and retrieve documents based on specific tags.
 *
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#tag-fields">Tag
 *      Fields</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TagFieldArgs extends FieldArgs {

    private Optional<String> separator = Optional.empty();

    private boolean caseSensitive;

    private boolean withSuffixTrie;

    /**
     * Create a new {@link TagFieldArgs} using the builder pattern.
     * 
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getFieldType() {
        return "TAG";
    }

    /**
     * Get the separator for tag fields.
     *
     * @return the separator
     */
    public Optional<String> getSeparator() {
        return separator;
    }

    /**
     * Check if the field is case sensitive.
     *
     * @return true if case sensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
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
    protected void buildTypeSpecificArgs(CommandArgs<?, ?> args) {
        separator.ifPresent(s -> args.add(SEPARATOR).add(s));
        if (caseSensitive) {
            args.add(CASESENSITIVE);
        }
        if (withSuffixTrie) {
            args.add(WITHSUFFIXTRIE);
        }
    }

    /**
     * Builder for {@link TagFieldArgs}.
     * 
     */
    public static class Builder extends FieldArgs.Builder<TagFieldArgs, Builder> {

        public Builder() {
            super(new TagFieldArgs());
        }

        /**
         * The separator for TAG attributes. The default separator is a comma.
         *
         * @param separator the separator for tag fields
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder separator(String separator) {
            instance.separator = Optional.of(separator);
            return self();
        }

        /**
         * Keeps the original letter cases of the tags. If not specified, the characters are converted to lowercase. Works with
         * TAG attributes.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder caseSensitive() {
            instance.caseSensitive = true;
            return self();
        }

        /**
         * For TAG attributes, keeps a suffix trie with all terms which match the suffix. It is used to optimize contains
         * (*foo*) and suffix (*foo) queries. Otherwise, a brute-force search on the trie is performed. If the suffix trie
         * exists for some fields, these queries will be disabled for other fields.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder withSuffixTrie() {
            instance.withSuffixTrie = true;
            return self();
        }

    }

}
