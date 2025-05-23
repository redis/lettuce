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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link Map} of values and Doubles output. This output processes Redis responses into a map where keys are of type V (values
 * from the codec) and values are of type Double.
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

    private boolean outputError = false;

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
        if (outputError) {
            return;
        }

        if (!hasKey) {
            key = (bytes == null) ? null : codec.decodeValue(bytes);
            hasKey = true;
            return;
        }

        LOG.warn("Expected double but got bytes, discarding the result");
        output = new HashMap<>(0);
        outputError = true;
    }

    @Override
    public void set(double number) {
        if (outputError) {
            return;
        }

        if (hasKey) {
            output.put(key, number);
            key = null;
            hasKey = false;
            return;
        }

        LOG.warn("Expected bytes but got double, discarding the result");
        output = new HashMap<>(0);
        outputError = true;
    }

    @Override
    public void multi(int count) {
        if (output == null) {
            output = new LinkedHashMap<>(count / 2, 1);
        }
    }

}
