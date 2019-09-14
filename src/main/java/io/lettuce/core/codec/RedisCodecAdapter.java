package io.lettuce.core.codec;

import java.nio.ByteBuffer;

/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * A {@link RedisCodecAdapter} combines different codecs to encode/decode key and value
 * to the command output.
 * 
 * @author Dimitris Mandalidis
 */
public final class RedisCodecAdapter {

    private RedisCodecAdapter() {}

    /**
     * Returns new {@link RedisCodec}
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param keyCodec the codec to encode/decode the keys
     * @param valueCodec the codec to encode/decode the values
     * @return
     */
    public static <K, V> RedisCodec<K, V> of(RedisCodec<K, ?> keyCodec, RedisCodec<?, V> valueCodec) {
        return new RedisCodecWrapper<K, V>(keyCodec, valueCodec);
    }
    
    private static class RedisCodecWrapper<K, V> implements RedisCodec<K, V> {
        private final RedisCodec<K, ?> keyCodec;
        private final RedisCodec<?, V> valueCodec;
        
        RedisCodecWrapper(RedisCodec<K, ?> keyCodec, RedisCodec<?, V> valueCodec) {
            this.keyCodec = keyCodec;
            this.valueCodec = valueCodec;
        }

        @Override
        public K decodeKey(ByteBuffer bytes) {
            return keyCodec.decodeKey(bytes);
        }

        @Override
        public V decodeValue(ByteBuffer bytes) {
            return valueCodec.decodeValue(bytes);
        }

        @Override
        public ByteBuffer encodeKey(K key) {
            return keyCodec.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(V value) {
            return valueCodec.encodeValue(value);
        }
    }
}
