package com.lambdaworks.redis.dynamic;

import static com.lambdaworks.redis.protocol.CommandKeyword.LIMIT;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.dynamic.parameter.MethodParametersAccessor;
import com.lambdaworks.redis.dynamic.segment.CommandSegment;
import com.lambdaworks.redis.dynamic.segment.CommandSegment.ArgumentContribution;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

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
     * @param args
     * @param commandSegments
     * @param accessor
     * @param <K>
     * @param <V>
     * @return
     */
    <K, V> CommandArgs<K, V> bind(CommandArgs<K, V> args, CommandSegments commandSegments, MethodParametersAccessor accessor) {

        int parameterCount = accessor.getParameterCount();

        Set<Integer> eatenParameters = new HashSet<>();

        for (CommandSegment commandSegment : commandSegments) {

            ArgumentContribution argumentContribution = commandSegment.contribute(accessor);
            bind(args, argumentContribution.getValue(), argumentContribution.getParameterIndex(), accessor);

            eatenParameters.add(argumentContribution.getParameterIndex());
        }

        for (int i = 0; i < parameterCount; i++) {

            if (eatenParameters.contains(i)) {
                continue;
            }

            Object bindableValue = accessor.getBindableValue(i);
            bind(args, bindableValue, i, accessor);

            eatenParameters.add(i);
        }

        return args;
    }

    @SuppressWarnings("unchecked")
    private <K, V> void bind(CommandArgs<K, V> args, Object argument, int index, MethodParametersAccessor accessor) {

        if (argument == null) {

            if (accessor.isBindableNullValue(index)) {
                args.add(new byte[0]);
            }

            return;
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
                    throw new UnsupportedOperationException("Value Range is not supported.");
                }

                if (argument instanceof Iterable) {
                    args.addValues((Iterable<V>) argument);
                } else {
                    args.addValue((V) argument);
                }
                return;
            }
        }

        if (argument instanceof String) {
            args.add((String) argument);
        }

        if (argument instanceof byte[]) {
            args.add((byte[]) argument);
        }

        if (argument instanceof Double) {
            args.add(((Double) argument));
        } else if (argument instanceof Number) {
            args.add(((Number) argument).longValue());
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

            Range<?> range = (Range<?>) argument;

            if (range.getLower().getValue() != null && !(range.getLower().getValue() instanceof Number)) {
                throw new IllegalArgumentException(
                        "Cannot bind non-numeric lower range value for a numeric Range. Annotate with @Value if the Range contains a value range.");
            }

            if (range.getUpper().getValue() != null && !(range.getUpper().getValue() instanceof Number)) {
                throw new IllegalArgumentException(
                        "Cannot bind non-numeric upper range value for a numeric Range. Annotate with @Value if the Range contains a value range.");
            }

            args.add(min((Range) range));
            args.add(max((Range) range));

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
        }
    }

    private String min(Range<? extends Number> range) {

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

    private String max(Range<? extends Number> range) {

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
}
