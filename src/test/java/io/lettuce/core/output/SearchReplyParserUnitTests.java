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
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SearchReplyParser;
import io.lettuce.core.search.arguments.SearchArgs;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Unit tests for {@link SearchReplyParser}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(UNIT_TEST)
class SearchReplyParserUnitTests {

    private static final StringCodec CODEC = StringCodec.UTF8;

    @Test
    void shouldParseListReply() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData data = array(2L, buffer("doc:1"),
                array(buffer("title"), buffer("Redis Search"), buffer("views"), buffer("100")), buffer("doc:2"),
                array(buffer("title"), buffer("Advanced Techniques")));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(2);
        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults().get(0).getId()).isEqualTo("doc:1");
        assertThat(reply.getResults().get(0).getFields().get("title").asString()).isEqualTo("Redis Search");
        assertThat(reply.getResults().get(0).getFields().get("views").asString()).isEqualTo("100");
        assertThat(reply.getResults().get(1).getId()).isEqualTo("doc:2");
        assertThat(reply.getResults().get(1).getFields().get("title").asString()).isEqualTo("Advanced Techniques");
    }

    @Test
    void shouldPreserveListFieldValues() {
        byte[] vector = new byte[] { 0, -1, 1, 2 };
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData fieldsData = array(buffer("title"), buffer("Redis Search"), buffer("embedding"),
                ByteBuffer.wrap(vector), buffer("city"), null);
        ArrayComplexData data = array(1L, buffer("doc:1"), fieldsData);

        SearchReply<String> reply = parser.parse(data);

        Map<String, FieldValue> fields = reply.getResults().get(0).getFields();
        assertThat(fields).containsOnlyKeys("title", "embedding", "city");
        assertThat(fields.get("title").asString()).isEqualTo("Redis Search");
        assertThat(fields.get("embedding").asBytes()).isEqualTo(vector);
        assertThat(fields.get("city").isNull()).isTrue();
    }

    @Test
    void shouldParseScoresWhenRequested() {
        SearchArgs<String> args = SearchArgs.<String> builder().withScores().build();
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, args);
        ArrayComplexData data = array(1L, buffer("doc:1"), buffer("0.95"), array(buffer("title"), buffer("Redis Search")));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).singleElement().satisfies(result -> {
            assertThat(result.getId()).isEqualTo("doc:1");
            assertThat(result.getScore()).isEqualTo(0.95);
            assertThat(result.getFields().get("title").asString()).isEqualTo("Redis Search");
        });
    }

    @Test
    void shouldOmitContentWhenRequested() {
        SearchArgs<String> args = SearchArgs.<String> builder().noContent().build();
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, args);
        ArrayComplexData data = array(2L, buffer("doc:1"), buffer("doc:2"));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(2);
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getId).containsExactly("doc:1", "doc:2");
        assertThat(reply.getResults()).allSatisfy(result -> assertThat(result.getFields()).isEmpty());
    }

    @Test
    void shouldParseListCursorReply() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData results = array(1L, buffer("doc:1"), array(buffer("title"), buffer("Redis Search")));
        ArrayComplexData data = array(results, 42L);

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getCursorId()).isEqualTo(42L);
        assertThat(reply.getCount()).isEqualTo(1);
        assertThat(reply.getResults()).singleElement().satisfies(result -> assertThat(result.getId()).isEqualTo("doc:1"));
    }

    @Test
    void shouldParseRowsWithoutDocumentIds() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC);
        ArrayComplexData data = array(2L, array(buffer("category"), buffer("books")),
                array(buffer("category"), buffer("electronics")));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults().get(0).getFields().get("category").asString()).isEqualTo("books");
        assertThat(reply.getResults().get(1).getFields().get("category").asString()).isEqualTo("electronics");
    }

    @Test
    void shouldDecodeListDocumentIdWithKeyCodec() {
        PrefixingStringCodec codec = new PrefixingStringCodec("tenant:");
        SearchReplyParser<String> parser = new SearchReplyParser<>(codec, null);
        ArrayComplexData data = array(1L, codec.encodeKey("doc:1"), array(buffer("tenant:title"), buffer("tenant:guide")));

        SearchReply<String> reply = parser.parse(data);

        SearchReply.SearchResult<String> result = reply.getResults().get(0);
        assertThat(result.getId()).isEqualTo("doc:1");
        assertThat(result.getFields()).containsKey("tenant:title");
        assertThat(result.getFields().get("tenant:title").asString()).isEqualTo("tenant:guide");
    }

    @Test
    void shouldParseMapReply() {
        byte[] vector = new byte[] { 0, -1, 1, 2 };
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);
        MapComplexData fieldsData = map(buffer("title"), buffer("Redis Search"), buffer("embedding"), ByteBuffer.wrap(vector),
                buffer("city"), null);
        MapComplexData resultData = map(buffer("id"), buffer("doc:1"), buffer("score"), 1.0, buffer("extra_attributes"),
                fieldsData);
        MapComplexData data = map(buffer("total_results"), 1L, buffer("results"), array(resultData));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(1);
        assertThat(reply.getResults()).singleElement().satisfies(result -> {
            assertThat(result.getId()).isEqualTo("doc:1");
            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getFields()).containsOnlyKeys("title", "embedding", "city");
            assertThat(result.getFields().get("title").asString()).isEqualTo("Redis Search");
            assertThat(result.getFields().get("embedding").asBytes()).isEqualTo(vector);
            assertThat(result.getFields().get("city").isNull()).isTrue();
        });
    }

    @Test
    void shouldParseScoreArrayFromMapReply() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);
        MapComplexData resultData = map(buffer("id"), buffer("doc:1"), buffer("score"), array(0.75));
        MapComplexData data = map(buffer("total_results"), 1L, buffer("results"), array(resultData));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).singleElement().satisfies(result -> assertThat(result.getScore()).isEqualTo(0.75));
    }

    @Test
    void shouldKeepMapResultWithoutDocumentId() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC);
        MapComplexData fieldsData = map(buffer("category"), buffer("books"));
        MapComplexData resultData = map(buffer("extra_attributes"), fieldsData);
        MapComplexData data = map(buffer("total_results"), 1L, buffer("results"), array(resultData));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).singleElement().satisfies(result -> {
            assertThat(result.getId()).isNull();
            assertThat(result.getFields().get("category").asString()).isEqualTo("books");
        });
    }

    @Test
    void shouldParseWarningsAndCursorFromMapReply() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);
        MapComplexData data = map(buffer("total_results"), 0L, buffer("results"), array(), buffer("warning"),
                array(buffer("Timeout limit was reached")), buffer("cursor"), 99L);

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getCount()).isZero();
        assertThat(reply.getResults()).isEmpty();
        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached");
        assertThat(reply.getCursorId()).isEqualTo(99L);
    }

    @Test
    void shouldDecodeMapDocumentIdWithKeyCodec() {
        PrefixingStringCodec codec = new PrefixingStringCodec("tenant:");
        SearchReplyParser<String> parser = new SearchReplyParser<>(codec, null);
        MapComplexData fieldsData = map(buffer("tenant:title"), buffer("tenant:guide"));
        MapComplexData resultData = map(buffer("id"), codec.encodeKey("doc:1"), buffer("extra_attributes"), fieldsData);
        MapComplexData data = map(buffer("total_results"), 1L, buffer("results"), array(resultData));

        SearchReply<String> reply = parser.parse(data);

        SearchReply.SearchResult<String> result = reply.getResults().get(0);
        assertThat(result.getId()).isEqualTo("doc:1");
        assertThat(result.getFields()).containsKey("tenant:title");
        assertThat(result.getFields().get("tenant:title").asString()).isEqualTo("tenant:guide");
    }

    @Test
    void shouldReturnEmptyReplyWhenInputCannotBeParsed() {
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, null);

        SearchReply<String> reply = parser.parse(array("not-a-count"));

        assertThat(reply.getCount()).isZero();
        assertThat(reply.getResults()).isEmpty();
        assertThat(reply.getCursorId()).isNull();
        assertThat(reply.getWarnings()).isEmpty();
    }

    @Test
    void shouldReturnNullIdForListRowsWithoutId() {
        // FT.AGGREGATE rows carry no id
        PrefixingStringCodec codec = new PrefixingStringCodec("tenant:");
        SearchReplyParser<String> parser = new SearchReplyParser<>(codec);
        ArrayComplexData data = array(2L, array(buffer("category"), buffer("books")),
                array(buffer("category"), buffer("electronics")));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getId).containsOnlyNulls();
    }

    @Test
    void shouldParseSortKeysFromListReply() {
        SearchArgs<String> args = SearchArgs.<String> builder().withSortKeys().build();
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, args);
        // [count, id, sortkey, fields, ...]; the sort key is nil for a document without the sorting attribute
        ArrayComplexData data = array(3L, buffer("doc:2"), buffer("$another doc"),
                array(buffer("title"), buffer("another doc")), buffer("doc:1"), buffer("$hello world"),
                array(buffer("title"), buffer("Hello World")), buffer("doc:3"), null, array(buffer("price"), buffer("7")));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(3);
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getId).containsExactly("doc:2", "doc:1", "doc:3");
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getSortKey).containsExactly("$another doc",
                "$hello world", null);
        assertThat(reply.getResults().get(0).getFields().get("title").asString()).isEqualTo("another doc");
        assertThat(reply.getResults().get(2).getFields().get("price").asString()).isEqualTo("7");
    }

    @Test
    void shouldParseSortKeysWithScoresAndWithoutContentFromListReply() {
        SearchArgs<String> args = SearchArgs.<String> builder().withScores().withSortKeys().noContent().build();
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, args);
        // [count, id, score, sortkey, ...]
        ArrayComplexData data = array(2L, buffer("doc:1"), buffer("0.83"), buffer("#10"), buffer("doc:3"), buffer("1.69"),
                buffer("#7"));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getId).containsExactly("doc:1", "doc:3");
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getScore).containsExactly(0.83, 1.69);
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getSortKey).containsExactly("#10", "#7");
        assertThat(reply.getResults()).allSatisfy(result -> assertThat(result.getFields()).isEmpty());
    }

    @Test
    void shouldParseSortKeysFromMapReply() {
        SearchArgs<String> args = SearchArgs.<String> builder().withSortKeys().build();
        SearchReplyParser<String> parser = new SearchReplyParser<>(CODEC, args);
        MapComplexData sorted = map(buffer("id"), buffer("doc:1"), buffer("sortkey"), buffer("$hello world"),
                buffer("extra_attributes"), map(buffer("title"), buffer("Hello World")));
        MapComplexData unsorted = map(buffer("id"), buffer("doc:3"), buffer("sortkey"), null, buffer("extra_attributes"),
                map(buffer("price"), buffer("7")));
        MapComplexData data = map(buffer("total_results"), 2L, buffer("results"), array(sorted, unsorted));

        SearchReply<String> reply = parser.parse(data);

        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getId).containsExactly("doc:1", "doc:3");
        assertThat(reply.getResults()).extracting(SearchReply.SearchResult::getSortKey).containsExactly("$hello world", null);
        assertThat(reply.getResults().get(0).getFields().get("title").asString()).isEqualTo("Hello World");
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
