package io.lettuce.core.api;

import java.util.Map;
import java.util.List;

import io.lettuce.core.FlushMode;
import io.lettuce.core.FunctionRestoreMode;
import io.lettuce.core.ScriptOutputType;

/**
 * ${intent} for Redis Functions. {@link java.lang.String Function code} is encoded by using the configured
 * {@link io.lettuce.core.ClientOptions#getScriptCharset() charset}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.3
 */
public interface RedisFunctionCommands<K, V> {

    /**
     * Invoke a function.
     *
     * @param function the function name.
     * @param type output type.
     * @param keys key names.
     * @param <T> expected return type.
     * @return function result.
     */
    <T> T fcall(String function, ScriptOutputType type, K... keys);

    /**
     * Invoke a function.
     *
     * @param function the function name.
     * @param type output type.
     * @param keys the keys.
     * @param values the values (arguments).
     * @param <T> expected return type.
     * @return function result.
     */
    <T> T fcall(String function, ScriptOutputType type, K[] keys, V... values);

    /**
     * Invoke a function in read-only mode.
     *
     * @param function the function name.
     * @param type output type.
     * @param keys key names.
     * @param <T> expected return type.
     * @return function result.
     */
    <T> T fcallReadOnly(String function, ScriptOutputType type, K... keys);

    /**
     * Invoke a function in read-only mode.
     *
     * @param function the function name.
     * @param type output type.
     * @param keys the keys.
     * @param values the values (arguments).
     * @param <T> expected return type.
     * @return function result.
     */
    <T> T fcallReadOnly(String function, ScriptOutputType type, K[] keys, V... values);

    /**
     * Load a library to Redis.
     *
     * @param functionCode code of the function.
     * @return name of the library.
     */
    String functionLoad(String functionCode);

    /**
     * Load a library to Redis.
     *
     * @param functionCode code of the function.
     * @param replace whether to replace an existing function.
     * @return name of the library.
     */
    String functionLoad(String functionCode, boolean replace);

    /**
     * Return the serialized payload of loaded libraries. You can restore the dump through {@link #functionRestore(byte[])}.
     *
     * @return the serialized payload.
     */
    byte[] functionDump();

    /**
     * You can restore the dumped payload of loaded libraries.
     *
     * @return Simple string reply
     */
    String functionRestore(byte[] dump);

    /**
     * You can restore the dumped payload of loaded libraries.
     *
     * @return Simple string reply
     */
    String functionRestore(byte[] dump, FunctionRestoreMode mode);

    /**
     * Deletes all the libraries using the specified {@link FlushMode}.
     *
     * @param flushMode the flush mode (sync/async).
     * @return String simple-string-reply.
     */
    String functionFlush(FlushMode flushMode);

    /**
     * Kill a function that is currently executing.
     *
     * @return String simple-string-reply.
     */
    String functionKill();

    /**
     * Return information about the functions and libraries.
     *
     * @return Array reply.
     */
    List<Map<String, Object>> functionList();

    /**
     * Return information about the functions and libraries.
     *
     * @param libraryName specify a pattern for matching library names.
     * @return Array reply.
     */
    List<Map<String, Object>> functionList(String libraryName);

}
