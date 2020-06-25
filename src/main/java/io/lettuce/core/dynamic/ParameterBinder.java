/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import static io.lettuce.core.protocol.CommandKeyword.LIMIT;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;

import io.lettuce.core.*;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.parameter.MethodParametersAccessor;
import io.lettuce.core.dynamic.segment.CommandSegment;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.dynamic.segment.CommandSegment.ArgumentContribution;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Parameter binder for {@link CommandSegments}-based Redis Commands.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class ParameterBinder {

    private static final byte[] MINUS_BYTES = { '-' };

    private static final byte[] PLUS_BYTES = { '+' };

    /**
     * Bind {@link CommandSegments} and method parameters to {@link CommandArgs}.
     *
     * @param args the command arguments.
     * @param codec the codec.
     * @param commandSegments the command segments.
     * @param accessor the parameter accessor.
     * @return
     */
    <K, V> CommandArgs<K, V> bind(CommandArgs<K, V> args, RedisCodec<K, V> codec, CommandSegments commandSegments,
            MethodParametersAccessor accessor) {

        int parameterCount = accessor.getParameterCount();

        BitSet set = new BitSet(parameterCount);

        for (CommandSegment commandSegment : commandSegments) {

            ArgumentContribution argumentContribution = commandSegment.contribute(accessor);
            bind(args, codec, argumentContribution.getValue(), argumentContribution.getParameterIndex(), accessor);

            if (argumentContribution.getParameterIndex() != -1) {
                set.set(argumentContribution.getParameterIndex());
            }
        }

        for (int i = 0; i < parameterCount; i++) {

            if (set.get(i)) {
                continue;
            }

            Object bindableValue = accessor.getBindableValue(i);
            bind(args, codec, bindableValue, i, accessor);

            set.set(i);
        }

        return args;
    }

    /*
     * Bind key/value/byte[] arguments. Other arguments are unwound, if applicable, and bound according to their type.
     */
    @SuppressWarnings("unchecked")
    private <K, V> void bind(CommandArgs<K, V> args, RedisCodec<K, V> codec, Object argument, int index,
            MethodParametersAccessor accessor) {

        if (argument == null) {

            if (accessor.isBindableNullValue(index)) {
                args.add(new byte[0]);
            }

            return;
        }

        if (argument instanceof byte[]) {
            if (index != -1 && accessor.isKey(index)) {
                args.addKey((K) argument);
            } else {
                args.add((byte[]) argument);
            }
            return;
        }

        if (argument.getClass().isArray()) {
            argument = asIterable(argument);
        }

        if (index != -1) {

            if (accessor.isKey(index)) {

                if (argument instanceof Iterable) {
                    args.addKeys((Iterable<K>) argument);
                } else {
                    args.addKey((K) argument);
                }
                return;
            }

            if (accessor.isValue(index)) {

                if (argument instanceof Range) {
                    bindValueRange(args, codec, (Range<? extends V>) argument);
                    return;
                }

                if (argument instanceof Iterable) {
                    args.addValues((Iterable<V>) argument);
                } else {
                    args.addValue((V) argument);
                }
                return;
            }
        }

        if (argument instanceof Iterable) {

            for (Object argumentElement : (Iterable<Object>) argument) {
                bindArgument(args, argumentElement);
            }

            return;
        }

        bindArgument(args, argument);
    }

    /*
     * Bind generic-handled arguments (String, ProtocolKeyword, Double, Map, Value hierarchy, Limit, Range, GeoCoordinates,
     * Composite Arguments).
     */
    @SuppressWarnings("unchecked")
    private static <K, V> void bindArgument(CommandArgs<K, V> args, Object argument) {

        if (argument instanceof byte[]) {
            args.add((byte[]) argument);
            return;
        }

        if (argument instanceof String) {
            args.add((String) argument);
            return;
        }

        if (argument instanceof Double) {
            args.add(((Double) argument));
            return;
        } else if (argument instanceof Number) {
            args.add(((Number) argument).longValue());
            return;
        }

        if (argument instanceof ProtocolKeyword) {
            args.add((ProtocolKeyword) argument);
            return;
        }

        if (argument instanceof Map) {
            args.add((Map<K, V>) argument);
            return;
        }

        if (argument instanceof ScoredValue) {

            ScoredValue<V> scoredValue = (ScoredValue<V>) argument;
            V value = scoredValue.getValueOrElseThrow(
                    () -> new IllegalArgumentException("Cannot bind empty ScoredValue to a Redis command."));

            args.add(scoredValue.getScore());
            args.addValue(value);
            return;
        }

        if (argument instanceof KeyValue) {

            KeyValue<K, V> keyValue = (KeyValue<K, V>) argument;
            V value = keyValue
                    .getValueOrElseThrow(() -> new IllegalArgumentException("Cannot bind empty KeyValue to a Redis command."));

            args.addKey(keyValue.getKey());
            args.addValue(value);
            return;
        }

        if (argument instanceof Value) {

            Value<V> valueWrapper = (Value<V>) argument;
            V value = valueWrapper
                    .getValueOrElseThrow(() -> new IllegalArgumentException("Cannot bind empty Value to a Redis command."));

            args.addValue(value);
            return;
        }

        if (argument instanceof Limit) {

            Limit limit = (Limit) argument;
            args.add(LIMIT);
            args.add(limit.getOffset());
            args.add(limit.getCount());
            return;
        }

        if (argument instanceof Range) {

            Range<? extends Number> range = (Range<? extends Number>) argument;
            bindNumericRange(args, range);
            return;
        }

        if (argument instanceof GeoCoordinates) {

            GeoCoordinates coordinates = (GeoCoordinates) argument;
            args.add(coordinates.getX().doubleValue());
            args.add(coordinates.getY().doubleValue());
            return;
        }

        if (argument instanceof CompositeArgument) {
            ((CompositeArgument) argument).build(args);
            return;
        }

        throw new IllegalArgumentException("Cannot bind unsupported command argument " + args);
    }

    private static <K, V> void bindValueRange(CommandArgs<K, V> args, RedisCodec<K, V> codec, Range<? extends V> range) {

        args.add(minValue(codec, range));
        args.add(maxValue(codec, range));
    }

    private static <K, V> void bindNumericRange(CommandArgs<K, V> args, Range<? extends Number> range) {

        if (range.getLower().getValue() != null && !(range.getLower().getValue() instanceof Number)) {
            throw new IllegalArgumentException(
                    "Cannot bind non-numeric lower range value for a numeric Range. Annotate with @Value if the Range contains a value range.");
        }

        if (range.getUpper().getValue() != null && !(range.getUpper().getValue() instanceof Number)) {
            throw new IllegalArgumentException(
                    "Cannot bind non-numeric upper range value for a numeric Range. Annotate with @Value if the Range contains a value range.");
        }

        args.add(minNumeric(range));
        args.add(maxNumeric(range));
    }

    private static String minNumeric(Range<? extends Number> range) {

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

    private static String maxNumeric(Range<? extends Number> range) {

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

    private static <K, V> byte[] minValue(RedisCodec<K, V> codec, Range<? extends V> range) {
        return valueRange(range.getLower(), MINUS_BYTES, codec);
    }

    private static <K, V> byte[] maxValue(RedisCodec<K, V> codec, Range<? extends V> range) {
        return valueRange(range.getUpper(), PLUS_BYTES, codec);
    }

    private static <K, V> byte[] valueRange(Range.Boundary<? extends V> boundary, byte[] unbounded, RedisCodec<K, V> codec) {

        if (boundary.getValue() == null) {
            return unbounded;
        }

        ByteBuffer encodeValue = codec.encodeValue(boundary.getValue());
        byte[] argument = new byte[encodeValue.remaining() + 1];

        argument[0] = (byte) (boundary.isIncluding() ? '[' : '(');

        encodeValue.get(argument, 1, argument.length - 1);

        return argument;
    }

    private static Object asIterable(Object argument) {

        if (argument.getClass().getComponentType().isPrimitive()) {

            int length = Array.getLength(argument);

            List<Object> elements = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                elements.add(Array.get(argument, i));
            }

            return elements;
        }
        return Arrays.asList((Object[]) argument);
    }

}
