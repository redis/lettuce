/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

package io.lettuce.core.json;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JsonParserRegistry {

    private JsonParserRegistry() {
    }

    // TODO make this configurable with ClientOptions to enable other types of parsers
    public static <K, V> JsonParser<K, V> getJsonParser(RedisCodec<K, V> codec) {
        return new DefaultJsonParser<>(codec);
    }

    private static class DefaultJsonParser<K, V> implements JsonParser<K, V> {

        private final RedisCodec<K, V> codec;

        private DefaultJsonParser(RedisCodec<K, V> codec) {
            this.codec = codec;
        }

        @Override
        public JsonValue<V> createJsonValue(ByteBuffer bytes) {
            return new DefaultJsonValue<>(bytes, codec::decodeValue, codec::encodeValue);
        }

        @Override
        public JsonValue<V> createJsonValue(V value) {
            return new DefaultJsonValue<>(codec.encodeValue(value), codec::decodeValue, codec::encodeValue);
        }

        @Override
        public JsonObject<K, V> createJsonObject() {
            return new DefaultJsonObject<K, V>(codec::decodeValue, codec::encodeValue);
        }

        @Override
        public JsonArray<V> createJsonArray() {
            return new DefaultJsonArray<V>(codec::decodeValue, codec::encodeValue);
        }

    }

    private static class DefaultJsonValue<V> implements JsonValue<V> {

        private final Function<ByteBuffer, V> decodeValue;

        private final Function<V, ByteBuffer> encodeValue;

        private final ByteBuffer unprocessedData;

        private DefaultJsonValue(ByteBuffer bytes, Function<ByteBuffer, V> decodeValue, Function<V, ByteBuffer> encodeValue) {
            unprocessedData = bytes;
            this.encodeValue = encodeValue;
            this.decodeValue = decodeValue;
        }

        @Override
        public V toValue() {
            return decodeValue.apply(unprocessedData);
        }

        @Override
        public ByteBuffer getUnprocessedValue() {
            return unprocessedData;
        }

        @Override
        public void commit() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

    }

    private static class DefaultJsonObject<K, V> extends DefaultJsonValue<V> implements JsonObject<K, V> {

        private DefaultJsonObject(Function<ByteBuffer, V> decodeValue, Function<V, ByteBuffer> encodeValue) {
            super(ByteBuffer.allocate(0), decodeValue, encodeValue);
        }

        @Override
        public void add(K key, JsonValue<V> element) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Map<String, JsonValue<V>> asMap() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public JsonValue<V> get(K key) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public JsonValue<V> remove(K key) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

    }

    private static class DefaultJsonArray<V> extends DefaultJsonValue<V> implements JsonArray<V> {

        private DefaultJsonArray(Function<ByteBuffer, V> decodeValue, Function<V, ByteBuffer> encodeValue) {
            super(ByteBuffer.allocate(0), decodeValue, encodeValue);
        }

        @Override
        public void add(JsonValue<V> element) {
            throw new UnsupportedOperationException("Not implemented yet");

        }

        @Override
        public void addAll(JsonArray<V> element) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<JsonValue<V>> asList() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Iterator<JsonValue<V>> iterator() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public JsonValue<V> remove(int index) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public boolean remove(JsonValue<V> element) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public JsonValue<V> replace(int index, JsonValue<V> newElement) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("Not implemented yet");
        }

    }

}
