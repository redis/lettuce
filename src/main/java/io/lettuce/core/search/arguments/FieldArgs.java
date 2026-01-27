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
 * Base class for field arguments in a RediSearch index.
 * <p>
 * This class contains common options shared by all field types. Specific field types should extend this class and add their
 * type-specific options.
 *
 * @param <K> Key type
 * @see <a href= "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">Field
 *      and type options</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class FieldArgs<K> {

    // Common field properties
    protected K name;

    protected Optional<K> as = Optional.empty();

    protected boolean sortable;

    protected boolean unNormalizedForm;

    protected boolean noIndex;

    protected boolean indexEmpty;

    protected boolean indexMissing;

    /**
     * Returns the field type. Subclasses must implement this method.
     *
     * @return the field type
     */
    public abstract String getFieldType();

    /**
     * Get the field name.
     *
     * @return the field name
     */
    public K getName() {
        return name;
    }

    /**
     * Get the field alias.
     *
     * @return the field alias
     */
    public Optional<K> getAs() {
        return as;
    }

    /**
     * Check if the field is sortable.
     *
     * @return true if sortable
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * Check if the field uses unnormalized form.
     *
     * @return true if unnormalized form
     */
    public boolean isUnNormalizedForm() {
        return unNormalizedForm;
    }

    /**
     * Check if the field is not indexed.
     *
     * @return true if not indexed
     */
    public boolean isNoIndex() {
        return noIndex;
    }

    /**
     * Check if the field indexes empty values.
     *
     * @return true if indexes empty values
     */
    public boolean isIndexEmpty() {
        return indexEmpty;
    }

    /**
     * Check if the field indexes missing values.
     *
     * @return true if indexes missing values
     */
    public boolean isIndexMissing() {
        return indexMissing;
    }

    /**
     * Build the field arguments into the command.
     *
     * @param args the command arguments to modify
     */
    public final void build(CommandArgs<K, ?> args) {
        args.add(name.toString());
        as.ifPresent(a -> args.add(AS).add(a.toString()));
        args.add(getFieldType());

        // Add type-specific arguments
        buildTypeSpecificArgs(args);

        // Add common arguments
        if (sortable) {
            args.add(SORTABLE);
            if (unNormalizedForm) {
                args.add(UNF);
            }
        }
        if (noIndex) {
            args.add(NOINDEX);
        }
        if (indexEmpty) {
            args.add(INDEXEMPTY);
        }
        if (indexMissing) {
            args.add(INDEXMISSING);
        }
    }

    /**
     * Add type-specific arguments to the command. Subclasses should override this method to add their specific arguments.
     *
     * @param args the command arguments to modify
     */
    protected abstract void buildTypeSpecificArgs(CommandArgs<K, ?> args);

    /**
     * Base builder for field arguments.
     *
     * @param <K> Key type
     * @param <T> The concrete field args type
     * @param <B> The concrete builder type
     */
    public abstract static class Builder<K, T extends FieldArgs<K>, B extends Builder<K, T, B>> {

        protected final T instance;

        /**
         * Constructor for subclasses.
         *
         * @param instance the field args instance to build
         */
        protected Builder(T instance) {
            this.instance = instance;
        }

        /**
         * Returns this builder instance for method chaining.
         *
         * @return this builder instance
         */
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        /**
         * The name of the field in a hash the index is going to be based on.
         *
         * @param name the name of the field
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public B name(K name) {
            instance.name = name;
            return self();
        }

        /**
         * Defines the attribute associated to the identifier. For example, you can use this feature to alias a complex JSONPath
         * expression with more memorable (and easier to type) name.
         *
         * @param as the field name to be used in queries
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public B as(K as) {
            instance.as = Optional.of(as);
            return self();
        }

        /**
         * NUMERIC, TAG, TEXT, or GEO attributes can have an optional SORTABLE argument. As the user sorts the results by the
         * value of this attribute, the results are available with very low latency. Default is false (not sortable).
         * <p/>
         * Note that this adds memory overhead, so consider not declaring it on large text attributes. You can sort an attribute
         * without the SORTABLE option, but the latency is not as good as with SORTABLE.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public B sortable() {
            instance.sortable = true;
            return self();
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
        public B unNormalizedForm() {
            instance.sortable = true;
            instance.unNormalizedForm = true;
            return self();
        }

        /**
         * Attributes can have the NOINDEX option, which means they will not be indexed. This is useful in conjunction with
         * {@link Builder#sortable()}, to create attributes whose update using PARTIAL will not cause full reindexing of the
         * document. If an attribute has NOINDEX and doesn't have SORTABLE, it will just be ignored by the index.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public B noIndex() {
            instance.noIndex = true;
            return self();
        }

        /**
         * For TEXT and TAG attributes, introduced in v2.10, allows you to index and search for empty strings. By default, empty
         * strings are not indexed.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public B indexEmpty() {
            instance.indexEmpty = true;
            return self();
        }

        /**
         * For all field types, introduced in v2.10, allows you to search for missing values, that is, documents that do not
         * contain a specific field. Note the difference between a field with an empty value and a document with a missing
         * value. By default, missing values are not indexed.
         *
         * @return the instance of the {@link Builder} for the purpose of method chaining
         */
        public B indexMissing() {
            instance.indexMissing = true;
            return self();
        }

        /**
         * Build the field arguments.
         *
         * @return the field arguments instance
         */
        public T build() {
            return instance;
        }

    }

}
