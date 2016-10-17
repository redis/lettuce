package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Interface for composite command argument objects. Implementing classes of {@link CompositeArgument} consolidate multiple
 * arguments for a particular Redis command in to one type and reduce the amount of individual arguments passed in a method
 * signature.
 * <p>
 * Command builder call {@link #build(CommandArgs)} during command construction to contribute command arguments for command
 * invocation. A composite argument is usually stateless as it can be reused multiple times by different commands.
 * 
 * @author Mark Paluch
 * @since 5.0
 * @see CommandArgs
 * @see SetArgs
 * @see ZStoreArgs
 * @see GeoArgs
 */
public interface CompositeArgument {

    /**
     * Build command arguments and contribute arguments to {@link CommandArgs}.
     * <p>
     * Implementing classes are required to implement this method. Depending on the command nature and configured arguments,
     * this method may contribute arguments but is not required to add arguments if none are specified.
     *
     * @param args the command arguments, must not be {@literal null}.
     * @param <K> Key type.
     * @param <V> Value type.
     */
    <K, V> void build(CommandArgs<K, V> args);
}
