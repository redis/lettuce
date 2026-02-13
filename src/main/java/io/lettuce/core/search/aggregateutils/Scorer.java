/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Abstract scorer for text search. Instances are created via {@link Scorers}.
 *
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Scorers
 */
@Experimental
public abstract class Scorer {

    private final String name;

    /**
     * Creates a new scorer with the specified name.
     *
     * @param name the scoring algorithm name (e.g., "BM25STD", "TFIDF")
     */
    protected Scorer(String name) {
        LettuceAssert.notNull(name, "Name must not be null");
        LettuceAssert.notEmpty(name, "Name must not be empty");
        this.name = name;
    }

    /**
     * Get the scorer name.
     *
     * @return the scorer name
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the scorer-specific arguments.
     *
     * @return list of scorer-specific arguments
     */
    protected abstract List<Object> getOwnArgs();

    /**
     * Build the SCORER arguments into the command.
     * <p>
     * Format: SCORER name [arg1] [arg2] ...
     * </p>
     *
     * @param args the {@link CommandArgs} to append to
     * @param <K> key type
     * @param <V> value type
     */
    public final <K, V> void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.SCORER);
        args.add(name);
        getOwnArgs().forEach(args::add);
    }

}
