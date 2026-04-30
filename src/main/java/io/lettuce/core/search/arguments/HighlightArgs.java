/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Argument list builder for {@code HIGHLIGHT} clause.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/highlighting/">Highlighting</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class HighlightArgs<K, V> {

    private final List<K> fields = new ArrayList<>();

    private Optional<Tags<V>> tags = Optional.empty();

    /**
     * Used to build a new instance of the {@link HighlightArgs}.
     *
     * @return a {@link HighlightArgs.Builder} that provides the option to build up a new instance of the {@link SearchArgs}
     * @param <K> the key type
     */
    public static <K, V> HighlightArgs.Builder<K, V> builder() {
        return new HighlightArgs.Builder<>();
    }

    /**
     * Builder for {@link HighlightArgs}.
     * <p>
     * As a final step the {@link HighlightArgs.Builder#build()} method needs to be executed to create the final
     * {@link SortByArgs} instance.
     *
     * @param <K> the key type
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder<K, V> {

        private final HighlightArgs<K, V> highlightArgs = new HighlightArgs<>();

        /**
         * Add a field to highlight. If no FIELDS directive is passed, then all returned fields are highlighted.
         *
         * @param field the field to summarize
         * @return the instance of the current {@link HighlightArgs.Builder} for the purpose of method chaining
         */
        public HighlightArgs.Builder<K, V> field(K field) {
            highlightArgs.fields.add(field);
            return this;
        }

        /**
         * Tags to surround the matched terms with. If no TAGS are specified, a built-in tag pair is prepended and appended to
         * each matched term.
         *
         * @param startTag the string is prepended to each matched term
         * @param endTag the string is appended to each matched term
         * @return the instance of the current {@link HighlightArgs.Builder} for the purpose of method chaining
         */
        public HighlightArgs.Builder<K, V> tags(V startTag, V endTag) {
            highlightArgs.tags = Optional.of(new Tags<>(startTag, endTag));
            return this;
        }

        /**
         * Build the {@link HighlightArgs}.
         *
         * @return the {@link HighlightArgs}
         */
        public HighlightArgs<K, V> build() {
            return highlightArgs;
        }

    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.HIGHLIGHT);

        if (!fields.isEmpty()) {
            args.add(CommandKeyword.FIELDS);
            args.add(fields.size());
            args.addKeys(fields);
        }

        tags.ifPresent(tags -> {
            args.add(CommandKeyword.TAGS);
            args.addValue(tags.startTag);
            args.addValue(tags.endTag);
        });

    }

    static class Tags<V> {

        private final V startTag;

        private final V endTag;

        Tags(V startTag, V endTag) {
            this.startTag = startTag;
            this.endTag = endTag;
        }

    }

}
