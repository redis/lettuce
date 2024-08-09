package io.lettuce.core.protocol;

import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.*;

import java.nio.ByteBuffer;

import static io.lettuce.core.protocol.CommandKeyword.LIMIT;

/**
 * Common utility methods shared by all implementations of the Redis command builder.
 *
 * @author Mark Paluch
 * @author Tihomir Mateev
 * @since 3.0
 */
public class BaseRedisCommandBuilder<K, V> {

    protected static final String MUST_NOT_CONTAIN_NULL_ELEMENTS = "must not contain null elements";

    protected static final String MUST_NOT_BE_EMPTY = "must not be empty";

    protected static final String MUST_NOT_BE_NULL = "must not be null";

    protected static final byte[] MINUS_BYTES = { '-' };

    protected static final byte[] PLUS_BYTES = { '+' };

    protected final RedisCodec<K, V> codec;

    public BaseRedisCommandBuilder(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output) {
        return createCommand(type, output, (CommandArgs<K, V>) null);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, K key) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key);
        return createCommand(type, output, args);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, K key, V value) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValue(value);
        return createCommand(type, output, args);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, K key, V[] values) {
        CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(key).addValues(values);
        return createCommand(type, output, args);
    }

    protected <T> Command<K, V, T> createCommand(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        return new Command<>(type, output, args);
    }

    @SuppressWarnings("unchecked")
    protected <T> CommandOutput<K, V, T> newScriptOutput(RedisCodec<K, V> codec, ScriptOutputType type) {
        switch (type) {
            case BOOLEAN:
                return (CommandOutput<K, V, T>) new BooleanOutput<>(codec);
            case INTEGER:
                return (CommandOutput<K, V, T>) new IntegerOutput<>(codec);
            case STATUS:
                return (CommandOutput<K, V, T>) new StatusOutput<>(codec);
            case MULTI:
                return (CommandOutput<K, V, T>) new NestedMultiOutput<>(codec);
            case VALUE:
                return (CommandOutput<K, V, T>) new ValueOutput<>(codec);
            case OBJECT:
                return (CommandOutput<K, V, T>) new ObjectOutput<>(codec);
            default:
                throw new RedisException("Unsupported script output type");
        }
    }

    protected boolean allElementsInstanceOf(Object[] objects, Class<?> expectedAssignableType) {

        for (Object object : objects) {
            if (!expectedAssignableType.isAssignableFrom(object.getClass())) {
                return false;
            }
        }

        return true;
    }

    protected byte[] maxValue(Range<? extends V> range) {

        Range.Boundary<? extends V> upper = range.getUpper();

        if (upper.getValue() == null) {
            return PLUS_BYTES;
        }

        ByteBuffer encoded = codec.encodeValue(upper.getValue());
        ByteBuffer allocated = ByteBuffer.allocate(encoded.remaining() + 1);
        allocated.put(upper.isIncluding() ? (byte) '[' : (byte) '(').put(encoded);

        return allocated.array();
    }

    protected byte[] minValue(Range<? extends V> range) {

        Range.Boundary<? extends V> lower = range.getLower();

        if (lower.getValue() == null) {
            return MINUS_BYTES;
        }

        ByteBuffer encoded = codec.encodeValue(lower.getValue());
        ByteBuffer allocated = ByteBuffer.allocate(encoded.remaining() + 1);
        allocated.put(lower.isIncluding() ? (byte) '[' : (byte) '(').put(encoded);

        return allocated.array();
    }

    protected static void notNull(ScoredValueStreamingChannel<?> channel) {
        LettuceAssert.notNull(channel, "ScoredValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    protected static void notNull(KeyStreamingChannel<?> channel) {
        LettuceAssert.notNull(channel, "KeyValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    protected static void notNull(ValueStreamingChannel<?> channel) {
        LettuceAssert.notNull(channel, "ValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    protected static void notNull(KeyValueStreamingChannel<?, ?> channel) {
        LettuceAssert.notNull(channel, "KeyValueStreamingChannel " + MUST_NOT_BE_NULL);
    }

    protected static void notNullMinMax(String min, String max) {
        LettuceAssert.notNull(min, "Min " + MUST_NOT_BE_NULL);
        LettuceAssert.notNull(max, "Max " + MUST_NOT_BE_NULL);
    }

    protected static void addLimit(CommandArgs<?, ?> args, Limit limit) {

        if (limit.isLimited()) {
            args.add(LIMIT).add(limit.getOffset()).add(limit.getCount());
        }
    }

    protected static void assertNodeId(String nodeId) {
        LettuceAssert.notNull(nodeId, "NodeId " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(nodeId, "NodeId " + MUST_NOT_BE_EMPTY);
    }

    protected static String max(Range<? extends Number> range) {

        Range.Boundary<? extends Number> upper = range.getUpper();

        if (upper.getValue() == null
                || upper.getValue() instanceof Double && upper.getValue().doubleValue() == Double.POSITIVE_INFINITY) {
            return "+inf";
        }

        if (!upper.isIncluding()) {
            return "(" + upper.getValue();
        }

        return upper.getValue().toString();
    }

    protected static String min(Range<? extends Number> range) {

        Range.Boundary<? extends Number> lower = range.getLower();

        if (lower.getValue() == null
                || lower.getValue() instanceof Double && lower.getValue().doubleValue() == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }

        if (!lower.isIncluding()) {
            return "(" + lower.getValue();
        }

        return lower.getValue().toString();
    }

    protected static void notEmpty(Object[] keys) {
        LettuceAssert.notNull(keys, "Keys " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(keys, "Keys " + MUST_NOT_BE_EMPTY);
    }

    protected static void notEmptySlots(int[] slots) {
        LettuceAssert.notNull(slots, "Slots " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(slots, "Slots " + MUST_NOT_BE_EMPTY);
    }

    protected static void notEmptyValues(Object[] values) {
        LettuceAssert.notNull(values, "Values " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(values, "Values " + MUST_NOT_BE_EMPTY);
    }

    protected static void notNullKey(Object key) {
        LettuceAssert.notNull(key, "Key " + MUST_NOT_BE_NULL);
    }

    protected static void keyAndFieldsProvided(Object key, Object[] fields) {
        LettuceAssert.notNull(key, "Key " + MUST_NOT_BE_NULL);
        LettuceAssert.notEmpty(fields, "Fields " + MUST_NOT_BE_EMPTY);
    }

    protected static void notNullLimit(Limit limit) {
        LettuceAssert.notNull(limit, "Limit " + MUST_NOT_BE_NULL);
    }

    protected static void notNullRange(Range<?> range) {
        LettuceAssert.notNull(range, "Range " + MUST_NOT_BE_NULL);
    }

    protected static void notEmptyRanges(Range<?>[] ranges) {
        LettuceAssert.notEmpty(ranges, "Ranges " + MUST_NOT_BE_NULL);
    }

}
