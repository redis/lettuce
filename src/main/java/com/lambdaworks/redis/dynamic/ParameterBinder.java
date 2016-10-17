package com.lambdaworks.redis.dynamic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.lambdaworks.redis.CompositeArgument;
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
            args.add(new byte[0]);
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

        if (argument instanceof CompositeArgument) {
            ((CompositeArgument) argument).build(args);
        }
    }
}
