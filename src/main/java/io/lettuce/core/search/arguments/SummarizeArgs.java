/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 * limitations under the License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Argument list builder for {@code SUMMARIZE} clause.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/highlight/">Highlighing</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SummarizeArgs<K, V> {

    private final List<K> fields = new ArrayList<>();

    private Optional<Long> frags = Optional.empty();

    private Optional<Long> len = Optional.empty();

    private Optional<V> separator = Optional.empty();

    /**
     * Used to build a new instance of the {@link SummarizeArgs}.
     *
     * @return a {@link SummarizeArgs.Builder} that provides the option to build up a new instance of the {@link SearchArgs}
     * @param <K> the key type
     */
    public static <K, V> SummarizeArgs.Builder<K, V> builder() {
        return new SummarizeArgs.Builder<>();
    }

    /**
     * Builder for {@link SummarizeArgs}.
     * <p>
     * As a final step the {@link SummarizeArgs.Builder#build()} method needs to be executed to create the final
     * {@link SortByArgs} instance.
     *
     * @param <K> the key type
     * @see <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE</a>
     */
    public static class Builder<K, V> {

        private final SummarizeArgs<K, V> summarizeArgs = new SummarizeArgs<>();

        /**
         * Add a field to summarize. Each field is summarized. If no FIELDS directive is passed, then all returned fields are
         * summarized.
         *
         * @param field the field to summarize
         * @return the instance of the current {@link SummarizeArgs.Builder} for the purpose of method chaining
         */
        public SummarizeArgs.Builder<K, V> field(K field) {
            summarizeArgs.fields.add(field);
            return this;
        }

        /**
         * Set the number of fragments to be returned. If not specified, the default is 3.
         *
         * @param frags the number of fragments to return
         * @return the instance of the current {@link SummarizeArgs.Builder} for the purpose of method chaining
         */
        public SummarizeArgs.Builder<K, V> fragments(long frags) {
            summarizeArgs.frags = Optional.of(frags);
            return this;
        }

        /**
         * Set the number of context words each fragment should contain. Context words surround the found term. A higher value
         * will return a larger block of text. If not specified, the default value is 20.
         *
         * @param len the length of the fragments
         * @return the instance of the current {@link SummarizeArgs.Builder} for the purpose of method chaining
         */

        public SummarizeArgs.Builder<K, V> len(long len) {
            summarizeArgs.len = Optional.of(len);
            return this;
        }

        /**
         * The string used to divide individual summary snippets. The default is <code>...</code> which is common among search
         * engines, but you may override this with any other string if you desire to programmatically divide the snippets later
         * on. You may also use a newline sequence, as newlines are stripped from the result body during processing.
         *
         * @param separator the separator between fragments
         * @return the instance of the current {@link SummarizeArgs.Builder} for the purpose of method chaining
         */
        public SummarizeArgs.Builder<K, V> separator(V separator) {
            summarizeArgs.separator = Optional.of(separator);
            return this;
        }

        /**
         * Build the {@link SummarizeArgs}.
         *
         * @return the {@link SummarizeArgs}
         */
        public SummarizeArgs<K, V> build() {
            return summarizeArgs;
        }

    }

    /**
     * Build a {@link CommandArgs} object that contains all the arguments.
     *
     * @param args the {@link CommandArgs} object
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.SUMMARIZE);

        if (!fields.isEmpty()) {
            args.add(CommandKeyword.FIELDS);
            args.add(fields.size());
            args.addKeys(fields);
        }

        frags.ifPresent(f -> {
            args.add(CommandKeyword.FRAGS);
            args.add(f);
        });

        len.ifPresent(l -> {
            args.add(CommandKeyword.LEN);
            args.add(l);
        });

        separator.ifPresent(s -> {
            args.add(CommandKeyword.SEPARATOR);
            args.addValue(s);
        });
    }

}
