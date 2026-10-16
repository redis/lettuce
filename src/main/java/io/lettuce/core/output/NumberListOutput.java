/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link List} of Number output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.5
 */
public class NumberListOutput<K, V> extends CommandOutput<K, V, List<Number>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(NumberListOutput.class);

    private static final String NULL_LITERAL = "null";

    private boolean initialized;

    public NumberListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            output.add(null);
            return;
        }

        String value = decodeString(bytes).trim();

        if (isJsonArray(value)) {
            addJsonArray(value);
            return;
        }

        output.add(parseNumber(value));
    }

    @Override
    public void set(double number) {
        output.add(number);
    }

    @Override
    public void set(long integer) {
        output.add(integer);
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        output.add(bytes != null ? parseNumber(decodeString(bytes).trim()) : null);
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    private static boolean isJsonArray(String value) {
        return value.length() >= 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']';
    }

    private void addJsonArray(String value) {

        String body = value.substring(1, value.length() - 1).trim();

        if (body.isEmpty()) {
            return;
        }

        for (String element : body.split(",")) {

            String trimmed = element.trim();

            if (NULL_LITERAL.equals(trimmed)) {
                output.add(null);
            } else {
                output.add(parseNumber(trimmed));
            }
        }
    }

    private Number parseNumber(String value) {

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignore) {
            // fall through to floating point parsing
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse " + value, e);
            return 0;
        }
    }

}
