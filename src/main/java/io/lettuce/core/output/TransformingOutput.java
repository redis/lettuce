/*
 * Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Command output that can transform the output from an underlying command output by applying a mapping function.
 * 
 * @author Mark Paluch
 * @since 6.3
 */
public class TransformingOutput<K, V, S, T> extends CommandOutput<K, V, T> {

    private final CommandOutput<K, V, S> delegate;

    private final Function<TransformingAccessor, T> mappingFunction;

    private final TransformingAccessor accessor;

    private TransformingOutput(CommandOutput<K, V, S> delegate, Function<TransformingAccessor, T> mappingFunction) {
        super(delegate.codec, null);
        this.delegate = delegate;
        this.mappingFunction = mappingFunction;
        this.accessor = new TransformingAccessor(delegate);
    }

    /**
     * Create a new transforming output.
     */
    public static <K, V, S, T> CommandOutput<K, V, T> transform(CommandOutput<K, V, S> delegate,
            Function<TransformingAccessor, T> mappingFunction) {
        return new TransformingOutput<>(delegate, mappingFunction);
    }

    @Override
    public void set(ByteBuffer bytes) {
        delegate.set(bytes);
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        delegate.setSingle(bytes);
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        delegate.setBigNumber(bytes);
    }

    @Override
    public void set(long integer) {
        delegate.set(integer);
    }

    @Override
    public void set(double number) {
        delegate.set(number);
    }

    @Override
    public void set(boolean value) {
        delegate.set(value);
    }

    @Override
    public void setError(ByteBuffer error) {
        delegate.setError(error);
    }

    @Override
    public void setError(String error) {
        delegate.setError(error);
    }

    @Override
    public boolean hasError() {
        return delegate.hasError();
    }

    @Override
    public String getError() {
        return delegate.getError();
    }

    @Override
    public T get() {
        return mappingFunction.apply(accessor);
    }

    @Override
    public void complete(int depth) {
        delegate.complete(depth);
    }

    @Override
    public void multi(int count) {
        delegate.multi(count);
    }

    @Override
    public void multiArray(int count) {
        delegate.multiArray(count);
    }

    @Override
    public void multiPush(int count) {
        delegate.multiPush(count);
    }

    @Override
    public void multiMap(int count) {
        delegate.multiMap(count);
    }

    @Override
    public void multiSet(int count) {
        delegate.multiSet(count);
    }

    /**
     * Accessor for the underlying output.
     */
    @SuppressWarnings("unchecked")
    public static class TransformingAccessor {

        private final CommandOutput<?, ?, ?> delegate;

        private TransformingAccessor(CommandOutput<?, ?, ?> delegate) {
            this.delegate = delegate;
        }

        /**
         * Obtain the output as inferred type.
         * 
         * @return
         * @param <T>
         */
        public <T> T get() {
            return (T) delegate.get();
        }

        /**
         * Obtain the output as {@link Map}.
         * 
         * @return
         * @param <K>
         * @param <V>
         */
        public <K, V> Map<K, V> map() {
            return (Map) delegate.get();
        }

        /**
         * Obtain the output as {@link List}.
         * 
         * @return
         * @param <T>
         */
        public <T> List<T> list() {
            return (List) delegate.get();
        }

        /**
         * Obtain an item by {@code index} from the underlying output {@link List}.
         * 
         * @param index
         * @return
         * @param <T>
         */
        public <T> T item(int index) {
            return (T) list().get(index);
        }

    }

}
