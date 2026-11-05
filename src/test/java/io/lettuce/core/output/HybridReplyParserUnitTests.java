/*
 * Copyright 2026-present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.FieldValue;
import io.lettuce.core.search.HybridReply;
import io.lettuce.core.search.HybridReplyParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Unit tests for {@link HybridReplyParser}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(UNIT_TEST)
class HybridReplyParserUnitTests {

    private static final StringCodec CODEC = StringCodec.UTF8;

    @Test
    void shouldParseListReply() {
        byte[] vector = new byte[] { 0, -1, 1, 2 };
        HybridReplyParser<String> parser = new HybridReplyParser<>(CODEC);
        ArrayComplexData resultData = array(buffer("title"), buffer("Redis Search"), buffer("embedding"),
                ByteBuffer.wrap(vector), buffer("city"), null, buffer("__key"), buffer("doc:1"));
        ArrayComplexData data = array(buffer("total_results"), 1L, buffer("execution_time"), buffer("0.5"), buffer("results"),
                array(resultData), buffer("warnings"), array(buffer("Timeout limit was reached")));

        HybridReply<String> reply = parser.parse(data);

        assertThat(reply.getTotalResults()).isEqualTo(1);
        assertThat(reply.getExecutionTime()).isEqualTo(0.5);
        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached");
        assertThat(reply.getResults()).singleElement().satisfies(result -> {
            Map<String, FieldValue> fields = result.getFields();
            assertThat(result.getId()).isEqualTo("doc:1");
            assertThat(fields).containsOnlyKeys("title", "embedding", "city");
            assertThat(fields.get("title").asString()).isEqualTo("Redis Search");
            assertThat(fields.get("embedding").asBytes()).isEqualTo(vector);
            assertThat(fields.get("city").isNull()).isTrue();
        });
    }

    @Test
    void shouldParseMapReply() {
        byte[] vector = new byte[] { 0, -1, 1, 2 };
        HybridReplyParser<String> parser = new HybridReplyParser<>(CODEC);
        MapComplexData resultData = map(buffer("title"), buffer("Redis Search"), buffer("embedding"), ByteBuffer.wrap(vector),
                buffer("city"), null, buffer("__key"), buffer("doc:1"));
        MapComplexData data = map(buffer("total_results"), 1L, buffer("execution_time"), 0.75, buffer("results"),
                array(resultData), buffer("warnings"),
                array(buffer("Timeout limit was reached"), buffer("Results may be incomplete")));

        HybridReply<String> reply = parser.parse(data);

        assertThat(reply.getTotalResults()).isEqualTo(1);
        assertThat(reply.getExecutionTime()).isEqualTo(0.75);
        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached", "Results may be incomplete");
        assertThat(reply.getResults()).singleElement().satisfies(result -> {
            Map<String, FieldValue> fields = result.getFields();
            assertThat(result.getId()).isEqualTo("doc:1");
            assertThat(fields).containsOnlyKeys("title", "embedding", "city");
            assertThat(fields.get("title").asString()).isEqualTo("Redis Search");
            assertThat(fields.get("embedding").asBytes()).isEqualTo(vector);
            assertThat(fields.get("city").isNull()).isTrue();
        });
    }

    @Test
    void shouldDecodeListDocumentKeyWithConnectionKeyCodec() {
        PrefixingStringCodec codec = new PrefixingStringCodec("tenant:");
        EncodedComplexOutput<String, String, HybridReply<String>> output = new EncodedComplexOutput<>(codec,
                new HybridReplyParser<>(codec));

        output.multiArray(4);
        output.set(buffer("total_results"));
        output.set(1L);
        output.set(buffer("results"));
        output.multiArray(1);
        output.multiArray(4);
        output.set(buffer("__key"));
        output.set(codec.encodeKey("doc:1"));
        output.set(buffer("title"));
        output.set(buffer("tenant:guide"));
        output.complete(2);
        output.complete(1);
        output.complete(0);

        Map<String, FieldValue> fields = output.get().getResults().get(0).getFields();

        assertThat(fields.get("title").asString()).isEqualTo("tenant:guide");
        assertThat(output.get().getResults().get(0).getId()).isEqualTo("doc:1");
        assertThat(fields).doesNotContainKey("__key");
    }

    @Test
    void shouldDecodeMapDocumentKeyWithConnectionKeyCodec() {
        PrefixingStringCodec codec = new PrefixingStringCodec("tenant:");
        EncodedComplexOutput<String, String, HybridReply<String>> output = new EncodedComplexOutput<>(codec,
                new HybridReplyParser<>(codec));

        output.multiMap(2);
        output.set(buffer("total_results"));
        output.set(1L);
        output.set(buffer("results"));
        output.multiArray(1);
        output.multiMap(2);
        output.set(buffer("__key"));
        output.set(codec.encodeKey("doc:1"));
        output.set(buffer("title"));
        output.set(buffer("tenant:guide"));
        output.complete(2);
        output.complete(1);
        output.complete(0);

        Map<String, FieldValue> fields = output.get().getResults().get(0).getFields();

        assertThat(fields.get("title").asString()).isEqualTo("tenant:guide");
        assertThat(output.get().getResults().get(0).getId()).isEqualTo("doc:1");
        assertThat(fields).doesNotContainKey("__key");
    }

    @Test
    void shouldReturnEmptyReplyWhenInputCannotBeParsed() {
        HybridReplyParser<String> parser = new HybridReplyParser<>(CODEC);

        HybridReply<String> reply = parser.parse(new SetComplexData(0));

        assertThat(reply.getTotalResults()).isZero();
        assertThat(reply.getExecutionTime()).isZero();
        assertThat(reply.getResults()).isEmpty();
        assertThat(reply.getWarnings()).isEmpty();
    }

    private static ByteBuffer buffer(String value) {
        return CODEC.encodeValue(value);
    }

    private static ArrayComplexData array(Object... values) {
        ArrayComplexData data = new ArrayComplexData(values.length);
        for (Object value : values) {
            data.storeObject(value);
        }
        return data;
    }

    private static MapComplexData map(Object... entries) {
        MapComplexData data = new MapComplexData(entries.length / 2);
        for (Object entry : entries) {
            data.storeObject(entry);
        }
        return data;
    }

    private static final class PrefixingStringCodec implements RedisCodec<String, String> {

        private final String prefix;

        private PrefixingStringCodec(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String decodeKey(ByteBuffer bytes) {
            String key = StringCodec.UTF8.decodeKey(bytes);
            return key.startsWith(prefix) ? key.substring(prefix.length()) : key;
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return StringCodec.UTF8.decodeValue(bytes);
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StringCodec.UTF8.encodeKey(prefix + key);
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return StringCodec.UTF8.encodeValue(value);
        }

    }

}
