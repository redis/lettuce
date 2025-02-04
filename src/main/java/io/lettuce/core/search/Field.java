/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.protocol.CommandArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.lettuce.core.protocol.CommandKeyword.*;

/**
 * Representation of a field in a RediSearch index.
 *
 * @param <K> Key type
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">Field
 *      and type options</a>
 * @since 6.6
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Field<K> {

    /**
     * Field types
     * 
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">Field and
     *      type options</a>
     */
    public enum Type {
        /**
         * Allows full-text search queries against the value in this attribute.
         */
        TEXT,
        /**
         * Allows exact-match queries, 1 as categories or primary keys, against the value in this attribute.
         * 
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/tags/">Tag Fields</a>
         */
        TAG,
        /**
         * Allows numeric range queries against the value in this attribute. See query syntax docs for details on how to use
         * numeric ranges.
         */
        NUMERIC,
        /**
         * Allows radius range queries against the value (point) in this attribute. The value of the attribute must be a string
         * containing a longitude (first) and latitude separated by a comma.
         */
        GEO,
        /**
         * Allows vector queries against the value in this attribute. Requires query dialect 2 or above (introduced in
         * RediSearch v2.4).
         * 
         * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/">Vector
         *      Fields</a>
         * @see <a href=
         *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/dialects/#dialect-2">Query
         *      Dialect v2</a>
         */
        VECTOR,
        /**
         * Allows polygon queries against the value in this attribute. The value of the attribute must follow a
         * <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">WKT notation</a> list of 2D points
         * representing the polygon edges POLYGON((x1 y1, x2 y2, ...) separated by a comma.
         * <p/>
         * A GEOSHAPE field type can be followed by one of the following coordinate systems:
         * <ul>
         * <li>SPHERICAL for Geographic longitude and latitude coordinates</li>
         * <li>FLAT for Cartesian X Y coordinates</li>
         * <li>The default coordinate system is SPHERICAL.</li>
         * </ul>
         *
         * Currently GEOSHAPE doesn't support JSON multi-value and SORTABLE option.
         */
        GEOSHAPE
    }

    /**
     * Phonetic matchers
     *
     * @see <a href=
     *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/phonetic_matching/">Phonetic
     *      Matching</a>
     */
    public enum PhoneticMatcher {

        ENGLISH("dm:en"), FRENCH("dm:fr"), PORTUGUESE("dm:pt"), SPANISH("dm:es");

        PhoneticMatcher(String matcher) {
            this.matcher = matcher;
        }

        private final String matcher;

        /**
         * @return the {@link String} representation of the matcher
         */
        public String getMatcher() {
            return matcher;
        }

    }

    private K name;

    private Optional<K> as = Optional.empty();

    private Type type;

    private boolean sortable;

    private boolean unNormalizedForm;

    private boolean noStemming;

    private boolean noIndex;

    private Optional<PhoneticMatcher> phonetic = Optional.empty();;

    private boolean caseSensitive;

    private boolean withSuffixTrie;

    private boolean indexEmpty;

    private boolean indexMissing;

    private Optional<Long> weight = Optional.empty();;

    private Optional<String> separator = Optional.empty();;

    private Field() {
    }

    /**
     * Create a new {@link Field} using the builder pattern.
     * <p/>
     * One needs to call {@link Builder#build()} to build a single {@link Field} or {@link Builder#buildFields()} to build a
     * {@link java.util.List} of {@link Field}s.
     * 
     * @param <K> Key type
     * @return a new {@link Builder}
     */
    public static <K> Builder<K> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link Field}.
     * 
     * @param <K> Key type
     */
    public static class Builder<K> {

        private final Field<K> instance = new Field<>();

        /**
         * The name of the field in a hash the index is going to be based on.
         * 
         * @param name the name of the field
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> name(K name) {
            instance.name = name;
            return this;
        }

        // TODO handling JsonPath
        // public Builder<K> name(JsonPath path) {
        // instance.name = path.toString();
        // return this;
        // }

        /**
         * The type of the field.
         * 
         * @param type the type of the field
         * @return the instance of the {@link Builder} for the purpose of method chaining
         * @see Type
         */
        public Builder<K> type(Type type) {
            instance.type = type;
            return this;
        }

        /**
         * Defines the attribute associated to the identifier. For example, you can use this feature to alias a complex JSONPath
         * expression with more memorable (and easier to type) name.
         * 
         * @param as the field name to be used in queries
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> as(K as) {
            instance.as = Optional.of(as);
            return this;
        }

        /**
         * {@link Type#NUMERIC}, {@link Type#TAG}, {@link Type#TEXT}, or {@link Type#GEO} attributes can have an optional
         * SORTABLE argument. As the user sorts the results by the value of this attribute, the results are available with very
         * low latency. Default is false (not sortable).
         * <p/>
         * Note that this adds memory overhead, so consider not declaring it on large text attributes. You can sort an attribute
         * without the SORTABLE option, but the latency is not as good as with SORTABLE.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> sortable() {
            instance.sortable = true;
            return this;
        }

        /**
         * By default, for hashes (not with JSON) SORTABLE applies normalization to the indexed value (characters set to
         * lowercase, removal of diacritics). When using the unnormalized form (UNF), you can disable the normalization and keep
         * the original form of the value. With JSON, UNF is implicit with SORTABLE (normalization is disabled).
         * <p/>
         * Default is false (normalized form).
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> unNormalizedForm() {
            instance.sortable = true;
            instance.unNormalizedForm = true;
            return this;
        }

        /**
         * By default, the index applies stemming to {@link Type#TEXT} fields. If you don't want to apply stemming to the field,
         * you can use the NOSTEM argument. This may be ideal for things like proper names.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> noStemming() {
            instance.noStemming = true;
            return this;
        }

        /**
         * Attributes can have the NOINDEX option, which means they will not be indexed. This is useful in conjunction with
         * {@link Builder#sortable()}, to create attributes whose update using PARTIAL will not cause full reindexing of the
         * document. If an attribute has NOINDEX and doesn't have SORTABLE, it will just be ignored by the index.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> noIndex() {
            instance.noIndex = true;
            return this;
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
         * <li>PORTUGUESE</li>x
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
            return this;
        }

        /**
         * The weight of the field. Works with {@link Type#TEXT} attributes, declares the importance of this attribute when
         * calculating result accuracy. This is a multiplication factor. The default weight is 1.
         *
         * @param weight the weight of the field
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> weight(long weight) {
            instance.weight = Optional.of(weight);
            return this;
        }

        /**
         * The separator for {@link Type#TAG} attributes. The default separator is a comma.
         *
         * @param separator the separator for tag fields
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> separator(String separator) {
            instance.separator = Optional.of(separator);
            return this;
        }

        /**
         * Keeps the original letter cases of the tags. If not specified, the characters are converted to lowercase. Works with
         * {@link Type#TAG} attributes.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> caseSensitive() {
            instance.caseSensitive = true;
            return this;
        }

        /**
         * For {@link Type#TEXT} and {@link Type#TAG} attributes, keeps a suffix trie with all terms which match the suffix. It
         * is used to optimize contains (foo) and suffix (*foo) queries. Otherwise, a brute-force search on the trie is
         * performed. If the suffix trie exists for some fields, these queries will be disabled for other fields.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> withSuffixTrie() {
            instance.withSuffixTrie = true;
            return this;
        }

        /**
         * For {@link Type#TEXT} and {@link Type#TAG} attributes, introduced in v2.10, allows you to index and search for empty
         * strings. By default, empty strings are not indexed.
         * 
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public Builder<K> indexEmpty() {
            instance.indexEmpty = true;
            return this;
        }

        /**
         * For all field types, introduced in v2.10, allows you to search for missing values, that is, documents that do not
         * contain a specific field. Note the difference between a field with an empty value and a document with a missing
         * value. By default, missing values are not indexed.
         */
        public Builder<K> indexMissing() {
            instance.indexMissing = true;
            return this;
        }

        /**
         * Build a single {@link Field}.
         *
         * @return the instance of the {@link Field}
         */
        public Field<K> build() {
            return instance;
        }

        /**
         * Build a {@link java.util.List} of {@link Field}s, containing the current {@link Field} as the only element of the
         * list.
         *
         * @return the instance of the {@link Field}
         */
        public List<Field<K>> buildFields() {
            List<Field<K>> fields = new ArrayList<>();
            fields.add(instance);
            return fields;
        }

    }

    /**
     * @return the type of the field
     * @see Builder#type(Type)
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the name of the field
     * @see Builder#name(Object)
     */
    public K getName() {
        return name;
    }

    /**
     * @return the alias of the field
     * @see Builder#as(Object)
     */
    public Optional<K> getAs() {
        return as;
    }

    /**
     * @return if the field should be sortable
     * @see Builder#sortable()
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * @return if the field should be in unnormalized form
     * @see Builder#unNormalizedForm()
     */
    public boolean isUnNormalizedForm() {
        return unNormalizedForm;
    }

    /**
     * @return if the field should not be indexed
     * @see Builder#noIndex()
     */
    public boolean isNoIndex() {
        return noIndex;
    }

    /**
     * @return if the field should not be stemmed
     * @see Builder#noStemming()
     */
    public boolean isNoStemming() {
        return noStemming;
    }

    /**
     * @return the setting for phonetic matching
     * @see Builder#phonetic(PhoneticMatcher)
     */
    public Optional<PhoneticMatcher> isPhonetic() {
        return phonetic;
    }

    /**
     * @return if the field should be case sensitive
     * @see Builder#caseSensitive()
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * @return if the field should have a suffix trie
     * @see Builder#withSuffixTrie()
     */
    public boolean isWithSuffixTrie() {
        return withSuffixTrie;
    }

    /**
     * @return if the field should index empty values
     * @see Builder#indexEmpty()
     */
    public boolean isIndexEmpty() {
        return indexEmpty;
    }

    /**
     * @return if the field should index missing values
     * @see Builder#indexMissing()
     */
    public boolean isIndexMissing() {
        return indexMissing;
    }

    /**
     * @return the weight of the field
     * @see Builder#weight(long)
     */
    public Optional<Long> getWeight() {
        return weight;
    }

    /**
     * @return the separator for tag fields
     * @see Builder#separator(String)
     */
    public Optional<String> getSeparator() {
        return separator;
    }

    /**
     * Add all configured arguments to the final command
     *
     * @param args the command arguments to modify
     */
    public void build(CommandArgs<K, ?> args) {
        args.addKey(name);
        as.ifPresent(a -> args.add(AS).addKey(a));
        args.add(type.toString());
        if (sortable) {
            args.add(SORTABLE);
            if (unNormalizedForm) {
                args.add(UNF);
            }
        }
        if (noStemming) {
            args.add(NOSTEM);
        }
        if (noIndex) {
            args.add(NOINDEX);
        }
        phonetic.ifPresent(p -> args.add(PHONETIC).add(p.getMatcher()));
        weight.ifPresent(w -> args.add(WEIGHT).add(w));
        separator.ifPresent(s -> args.add(SEPARATOR).add(s));
        if (caseSensitive) {
            args.add(CASESENSITIVE);
        }
        if (withSuffixTrie) {
            args.add(WITHSUFFIXTRIE);
        }
        if (indexEmpty) {
            args.add(INDEXEMPTY);
        }
        if (indexMissing) {
            args.add(INDEXMISSING);
        }
    }

}
