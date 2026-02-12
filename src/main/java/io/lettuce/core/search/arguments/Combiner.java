/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Abstract combiner for combining multiple search scores in FT.HYBRID command. Instances are created via {@link Combiners}.
 * <p>
 * Combiners define how text search scores and vector similarity scores are combined into a final ranking score using RRF
 * (Reciprocal Rank Fusion) or LINEAR strategies.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * {@code
 * // RRF with default parameters
 * Combiners.rrf()
 *
 * // RRF with custom window and constant
 * Combiners.rrf().window(10).constant(60)
 *
 * // Linear combination with weights
 * Combiners.linear().alpha(0.7).beta(0.3)
 *
 * // With score alias
 * Combiners.rrf().as("combined_score")
 * }
 * </pre>
 *
 * @param <K> Key type
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Combiners
 * @see <a href="https://redis.io/docs/latest/commands/ft.hybrid/">FT.HYBRID</a>
 */
@Experimental
public abstract class Combiner<K> {

    private final String name;

    private K scoreAlias;

    /**
     * Creates a new combiner with the specified name.
     *
     * @param name the combiner name (e.g., "RRF", "LINEAR")
     */
    protected Combiner(String name) {
        this.name = name;
    }

    /**
     * Get the combiner name.
     *
     * @return the combiner name
     */
    public final String getName() {
        return name;
    }

    /**
     * Set an alias for the combined score field using YIELD_SCORE_AS.
     *
     * @param alias the field name to use for the combined score
     * @return this instance
     */
    @SuppressWarnings("unchecked")
    public final <T extends Combiner<K>> T as(K alias) {
        this.scoreAlias = alias;
        return (T) this;
    }

    /**
     * Get the combiner-specific arguments.
     *
     * @return list of combiner-specific arguments
     */
    protected abstract List<Object> getOwnArgs();

    /**
     * Build the combiner arguments into the command.
     *
     * @param args the {@link CommandArgs} to append to
     * @param <V> value type
     */
    public final <V> void build(CommandArgs<K, V> args) {
        args.add(name);

        List<Object> ownArgs = getOwnArgs();
        args.add(ownArgs.size());
        for (Object arg : ownArgs) {
            args.add(arg);
        }

        if (scoreAlias != null) {
            args.add(CommandKeyword.YIELD_SCORE_AS);
            args.addKey(scoreAlias);
        }
    }

}
