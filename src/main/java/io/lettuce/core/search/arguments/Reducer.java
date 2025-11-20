/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * REDUCE function for GROUPBY operations. Performs aggregate operations on grouped results with optional aliasing.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see ReduceFunction
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Reducer<K, V> {

    private final ReduceFunction function;

    private final List<V> args;

    private Optional<K> alias = Optional.empty();

    /**
     * Creates a new reducer.
     *
     * @param function the reducer function
     * @param args the arguments to the reducer function
     */
    private Reducer(ReduceFunction function, List<V> args) {
        this.function = function;
        this.args = new ArrayList<>(args);
    }

    /**
     * Static factory method to create a Reducer with a function and optional arguments.
     *
     * @param function the reducer function
     * @param args the arguments to the reducer function (optional, can be empty for COUNT)
     * @param <K> Key type
     * @param <V> Value type
     * @return new Reducer instance
     */
    @SafeVarargs
    public static <K, V> Reducer<K, V> of(ReduceFunction function, V... args) {
        LettuceAssert.notNull(function, "ReduceFunction must not be null");
        return new Reducer<>(function, args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }

    /**
     * Set an alias for the reducer result.
     *
     * @param alias the alias name
     * @return this reducer
     */
    public Reducer<K, V> as(K alias) {
        LettuceAssert.notNull(alias, "Alias must not be null");
        this.alias = Optional.of(alias);
        return this;
    }

    /**
     * Build the reducer into command args.
     *
     * @param args the command args to build into
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.REDUCE);
        args.add(function);
        args.add(this.args.size());
        for (V arg : this.args) {
            args.addValue(arg);
        }

        alias.ifPresent(a -> {
            args.add(CommandKeyword.AS);
            args.add(a.toString());
        });
    }

}
