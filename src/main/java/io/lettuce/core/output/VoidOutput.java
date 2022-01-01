/*
 * Copyright 2020-2022 the original author or authors.
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
package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link Void} command output to consume data silently without actually processing it. Creating {@link VoidCodec} through its
 * constructor will preserve its error decoding since decoding errors is stateful. Obtaining {@link VoidOutput} through
 * {@link #create()} will return an instance that does not decode errors.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0.2
 */
public class VoidOutput<K, V> extends CommandOutput<K, V, Void> {

    private static final VoidOutput<Void, Void> INSTANCE = new VoidOutput<Void, Void>(VoidCodec.INSTANCE) {

        @Override
        public void setError(String error) {
            // no-op
        }

        @Override
        public void setError(ByteBuffer error) {
            // no-op
        }

    };

    /**
     * Initialize a new instance that decodes errors.
     */
    @SuppressWarnings("unchecked")
    public VoidOutput() {
        this((RedisCodec<K, V>) VoidCodec.INSTANCE);
    }

    /**
     * Initialize a new instance that decodes errors.
     *
     * @param codec used for type inference, must not be {@code null}.
     */
    public VoidOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    /**
     * Returns an instance of {@link VoidOutput} coerced to the expected generics. Since this codec does not decode any data at
     * all, it's safe to use this way. Note that this method is only suitable for fire-and-forget usage since errors are not
     * decoded.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the {@link VoidOutput} instance.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> VoidOutput<K, V> create() {
        return (VoidOutput) INSTANCE;
    }

    @Override
    public void set(ByteBuffer bytes) {
        // no-op
    }

    @Override
    public void set(long integer) {
        // no-op
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        // no-op
    }

    @Override
    public void setBigNumber(ByteBuffer bytes) {
        // no-op
    }

    @Override
    public void set(double number) {
        // no-op
    }

    @Override
    public void set(boolean value) {
        // no-op
    }

    enum VoidCodec implements RedisCodec<Void, Void> {

        INSTANCE;

        @Override
        public Void decodeKey(ByteBuffer bytes) {
            return null;
        }

        @Override
        public Void decodeValue(ByteBuffer bytes) {
            return null;
        }

        @Override
        public ByteBuffer encodeKey(Void key) {
            return null;
        }

        @Override
        public ByteBuffer encodeValue(Void value) {
            return null;
        }

    }

}
