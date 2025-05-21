/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link Map} of values and Doubles output. This output processes Redis responses into a map where
 * keys are of type V (values from the codec) and values are of type Double.
 * <p>
 * This is particularly useful for commands that return elements with scores, such as VSIM with WITHSCORES option.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
public class ValueDoubleMapOutput<K, V> extends CommandOutput<K, V, Map<V, Double>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(ValueDoubleMapOutput.class);

    private boolean hasKey;
    private V key;

    /**
     * Initialize a new {@link ValueDoubleMapOutput}.
     *
     * @param codec Codec used to decode keys and values, must not be {@code null}.
     */
    public ValueDoubleMapOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (!hasKey) {
            key = (bytes == null) ? null : codec.decodeValue(bytes);
            hasKey = true;
            return;
        }

        // This should not happen for VSIM with WITHSCORES as scores are always numeric
        // But we handle it anyway for completeness
        String strValue = (bytes == null) ? null : codec.decodeValue(bytes).toString();
        if (strValue != null) {
            try {
                output.put(key, Double.parseDouble(strValue));
            } catch (NumberFormatException e) {
                output.put(key, 0D); // Default to 0 if parsing fails
            }
        } else {
            output.put(key, 0D);
        }

        key = null;
        hasKey = false;
    }

    @Override
    public void set(double number) {

        if(!hasKey){
            LOG.warn("Expected key but got double: " + number);
            return;
        }

        output.put(key, (Double) number);
        key = null;
        hasKey = false;
    }

    @Override
    public void multi(int count) {
        if (output == null) {
            output = new LinkedHashMap<>(count / 2, 1);
        }
    }
}
