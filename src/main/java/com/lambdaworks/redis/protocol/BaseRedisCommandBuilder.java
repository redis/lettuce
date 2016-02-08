package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.ScriptOutputType;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.*;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class BaseRedisCommandBuilder<K, V> {
    protected RedisCodec<K, V> codec;

    public BaseRedisCommandBuilder(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output) {
        return createCommand(type, output, (CommandArgs<K, V>) null);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, K key) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key);
        return createCommand(type, output, args);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, K key, V value) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValue(value);
        return createCommand(type, output, args);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, K key, V[] values) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec).addKey(key).addValues(values);
        return createCommand(type, output, args);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return new Command<K, V, T>(type, output, args);
    }

    @SuppressWarnings("unchecked")
    protected <T> CommandOutput<K, V, T> newScriptOutput(RedisCodec<K, V> codec, ScriptOutputType type) {
        switch (type) {
            case BOOLEAN:
                return (CommandOutput<K, V, T>) new BooleanOutput<K, V>(codec);
            case INTEGER:
                return (CommandOutput<K, V, T>) new IntegerOutput<K, V>(codec);
            case STATUS:
                return (CommandOutput<K, V, T>) new StatusOutput<K, V>(codec);
            case MULTI:
                return (CommandOutput<K, V, T>) new NestedMultiOutput<K, V>(codec);
            case VALUE:
                return (CommandOutput<K, V, T>) new ValueOutput<K, V>(codec);
            default:
                throw new RedisException("Unsupported script output type");
        }
    }

    /**
     * Assert that a string is not empty, it must not be {@code null} and it must not be empty.
     *
     * @param string the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null} or the underlying string is empty
     */
    protected static void assertNotEmpty(String string, String message) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an object is not {@code null} .
     *
     * @param object the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is {@code null}
     */
    public static void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     */
    protected void assertNotEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array is {@code null} or has no elements
     */
    protected static void assertNotEmpty(int[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an array has no null elements.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     */
    public static void assertNoNullElements(Object[] array, String message) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    /**
     * Assert that {@code value} is {@literal true}.
     *
     * @param value the value to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object array contains a {@code null} element
     */
    public static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }

}
