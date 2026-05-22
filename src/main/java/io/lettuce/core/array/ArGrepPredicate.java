/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Represents a single predicate for the Redis {@code ARGREP} command.
 * <p>
 * Each predicate has a {@link Type} (EXACT, MATCH, GLOB, or RE) and a pattern string. Multiple predicates can be combined with
 * AND/OR logic via {@link ArGrepArgs}.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see ArGrepArgs
 * @see <a href="https://redis.io/docs/latest/commands/argrep/">Redis Documentation: ARGREP</a>
 */
@Experimental
public class ArGrepPredicate {

    /**
     * The type of predicate matching.
     */
    public enum Type {

        /**
         * Exact string match.
         */
        EXACT(CommandKeyword.EXACT),

        /**
         * Substring match.
         */
        MATCH(CommandKeyword.MATCH),

        /**
         * Glob-style pattern match.
         */
        GLOB(CommandKeyword.GLOB),

        /**
         * Regular expression match.
         */
        RE(CommandKeyword.RE);

        private final CommandKeyword keyword;

        Type(CommandKeyword keyword) {
            this.keyword = keyword;
        }

        /**
         * Returns the corresponding {@link CommandKeyword}.
         *
         * @return the protocol keyword.
         */
        public CommandKeyword getKeyword() {
            return keyword;
        }

    }

    private final Type type;

    private final String pattern;

    /**
     * Creates a new predicate.
     *
     * @param type the predicate type, must not be {@code null}.
     * @param pattern the pattern string, must not be {@code null}.
     */
    public ArGrepPredicate(Type type, String pattern) {
        LettuceAssert.notNull(type, "Type must not be null");
        LettuceAssert.notNull(pattern, "Pattern must not be null");
        this.type = type;
        this.pattern = pattern;
    }

    /**
     * Gets the predicate type.
     *
     * @return the type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the pattern string.
     *
     * @return the pattern.
     */
    public String getPattern() {
        return pattern;
    }

}
