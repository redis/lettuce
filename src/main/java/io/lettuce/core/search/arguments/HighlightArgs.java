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
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/highlighting/">Highlighting</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class HighlightArgs {

    private final List<String> fields = new ArrayList<>();

    private Optional<Tags> tags = Optional.empty();

    /**
     * Used to build a new instance of the {@link HighlightArgs}.
     *
     * @return a {@link HighlightArgs.Builder} that provides the option to build up a new instance of the {@link SearchArgs}
     */
    public static HighlightArgs.Builder builder() {
        return new HighlightArgs.Builder();
    }

    /**
     * Builder for {@link HighlightArgs}.
     * <p>
     * As a final step the {@link HighlightArgs.Builder#build()} method needs to be executed to create the final
     * {@link SortByArgs} instance.
     *
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder {

        private final HighlightArgs highlightArgs = new HighlightArgs();

        /**
         * Add a field to highlight. If no FIELDS directive is passed, then all returned fields are highlighted.
         *
         * @param field the field to summarize
         * @return the instance of the current {@link HighlightArgs.Builder} for the purpose of method chaining
         */
        public HighlightArgs.Builder field(String field) {
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
        public HighlightArgs.Builder tags(String startTag, String endTag) {
            highlightArgs.tags = Optional.of(new Tags(startTag, endTag));
            return this;
        }

        /**
         * Build the {@link HighlightArgs}.
         *
         * @return the {@link HighlightArgs}
         */
        public HighlightArgs build() {
            return highlightArgs;
        }

    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<?, ?> args) {
        args.add(CommandKeyword.HIGHLIGHT);

        if (!fields.isEmpty()) {
            args.add(CommandKeyword.FIELDS);
            args.add(fields.size());
            fields.forEach(args::add);
        }

        tags.ifPresent(tags -> {
            args.add(CommandKeyword.TAGS);
            args.add(tags.startTag);
            args.add(tags.endTag);
        });

    }

    static class Tags {

        private final String startTag;

        private final String endTag;

        Tags(String startTag, String endTag) {
            this.startTag = startTag;
            this.endTag = endTag;
        }

    }

}
