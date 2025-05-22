/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceFactories;

import java.nio.ByteBuffer;
import java.util.Deque;

/**
 * An implementation of the {@link CommandOutput} that is used in combination with a given {@link ComplexDataParser} to produce
 * a domain object from the data extracted from the server. Since there already are implementations of the {@link CommandOutput}
 * interface for most simple types, this implementation is better suited to parse complex, often nested, data structures, for
 * example a map containing other maps, arrays or sets as values for one or more of its keys.
 * <p>
 * The implementation of the {@link ComplexDataParser} is responsible for mapping the data from the result to meaningful
 * properties that the user of the LEttuce driver could then use in a statically typed manner.
 *
 * @see ComplexDataParser
 * @author Tihomir Mateev
 * @since 6.5
 */
public class ComplexOutput<K, V, T> extends CommandOutput<K, V, T> {

    private final Deque<ComplexData> dataStack;

    private final ComplexDataParser<T> parser;

    private ComplexData data;

    /**
     * Constructs a new instance of the {@link ComplexOutput}
     * 
     * @param codec the {@link RedisCodec} to be applied
     */
    public ComplexOutput(RedisCodec<K, V> codec, ComplexDataParser<T> parser) {
        super(codec, null);
        dataStack = LettuceFactories.newSpScQueue();
        this.parser = parser;
    }

    @Override
    public T get() {
        return parser.parse(data);
    }

    @Override
    public void set(long integer) {
        if (data == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "Integer value should have been preceded by some sort of aggregation.");
        }

        data.store(integer);
    }

    @Override
    public void set(double number) {
        if (data == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "Double value should have been preceded by some sort of aggregation.");
        }

        data.store(number);
    }

    @Override
    public void set(boolean value) {
        if (data == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "Double value should have been preceded by some sort of aggregation.");
        }

        data.store(value);
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (data == null) {
            // Sometimes the server would return null for commands that otherwise would end up returning a complex data
            // structure.
            // In these cases we should simply return null too.
            return;
        }

        data.storeObject(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        if (data == null) {
            throw new RuntimeException("Invalid output received for dynamic aggregate output."
                    + "String value should have been preceded by some sort of aggregation.");
        }

        data.store(bytes == null ? null : StringCodec.UTF8.decodeValue(bytes));
    }

    @Override
    public void complete(int depth) {
        if (!dataStack.isEmpty() && depth == dataStack.size()) {
            data = dataStack.pop();
        }
    }

    private void multi(ComplexData newData) {
        // if there is no data set, then we are at the root object
        if (data == null) {
            data = newData;
            return;
        }

        // otherwise we need to nest the provided structure
        data.storeObject(newData);
        dataStack.push(data);
        data = newData;
    }

    @Override
    public void multiSet(int count) {
        SetComplexData dynamicData = new SetComplexData(count);
        multi(dynamicData);
    }

    @Override
    public void multiArray(int count) {
        ArrayComplexData dynamicData = new ArrayComplexData(count);
        multi(dynamicData);
    }

    @Override
    public void multiMap(int count) {
        MapComplexData dynamicData = new MapComplexData(count);
        multi(dynamicData);
    }

}
