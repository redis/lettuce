/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SearchReplyParser;
import io.lettuce.core.search.arguments.SearchArgs;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SearchReplyParser}.
 *
 * @author Redis Ltd.
 */
@Tag(UNIT_TEST)
class SearchReplyParserUnitTests {

    private static final StringCodec CODEC = StringCodec.UTF8;

    // ===== RESP2 Tests =====

    @Test
    void shouldReturnEmptyReplyForEmptyResp2List() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData data = new ArrayComplexData(0);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply).isNotNull();
        assertThat(reply.getCount()).isEqualTo(0);
        assertThat(reply.getResults()).isEmpty();
    }

    @Test
    void shouldParseResp2WithCountAndNoResults() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData data = new ArrayComplexData(1);
        data.storeObject(0L);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(0);
        assertThat(reply.getResults()).isEmpty();
    }

    @Test
    void shouldParseResp2WithMultipleDocuments() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData data = new ArrayComplexData(5);
        data.storeObject(2L);

        data.storeObject(CODEC.encodeKey("doc:1"));
        ArrayComplexData fields1 = new ArrayComplexData(4);
        fields1.storeObject(CODEC.encodeKey("title"));
        fields1.storeObject(CODEC.encodeValue("Redis Search"));
        fields1.storeObject(CODEC.encodeKey("views"));
        fields1.storeObject(CODEC.encodeValue("100"));
        data.storeObject(fields1);

        data.storeObject(CODEC.encodeKey("doc:2"));
        ArrayComplexData fields2 = new ArrayComplexData(2);
        fields2.storeObject(CODEC.encodeKey("title"));
        fields2.storeObject(CODEC.encodeValue("Advanced Techniques"));
        data.storeObject(fields2);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(2);
        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults().get(0).getId()).isEqualTo("doc:1");
        assertThat(reply.getResults().get(0).getFields()).containsEntry("title", "Redis Search").containsEntry("views", "100");
        assertThat(reply.getResults().get(1).getId()).isEqualTo("doc:2");
        assertThat(reply.getResults().get(1).getFields()).containsEntry("title", "Advanced Techniques");
    }

    @Test
    void shouldParseResp2WithScores() {
        SearchArgs<String> args = SearchArgs.<String> builder().withScores().build();
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, args);
        ArrayComplexData data = new ArrayComplexData(4);
        data.storeObject(1L);
        data.storeObject(CODEC.encodeKey("doc:1"));
        data.storeObject(CODEC.encodeKey("0.95")); // score encoded as string
        ArrayComplexData fields = new ArrayComplexData(2);
        fields.storeObject(CODEC.encodeKey("title"));
        fields.storeObject(CODEC.encodeValue("Test"));
        data.storeObject(fields);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getResults()).hasSize(1);
        assertThat(reply.getResults().get(0).getScore()).isEqualTo(0.95);
        assertThat(reply.getResults().get(0).getId()).isEqualTo("doc:1");
    }

    @Test
    void shouldParseResp2WithNoContent() {
        SearchArgs<String> args = SearchArgs.<String> builder().noContent().build();
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, args);
        ArrayComplexData data = new ArrayComplexData(3);
        data.storeObject(2L);
        data.storeObject(CODEC.encodeKey("doc:1"));
        data.storeObject(CODEC.encodeKey("doc:2"));

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(2);
        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults().get(0).getId()).isEqualTo("doc:1");
        assertThat(reply.getResults().get(1).getId()).isEqualTo("doc:2");
        assertThat(reply.getResults().get(0).getFields()).isEmpty();
    }

    @Test
    void shouldParseResp2CursorResponse() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);
        ArrayComplexData innerResults = new ArrayComplexData(3);
        innerResults.storeObject(1L);
        innerResults.storeObject(CODEC.encodeKey("doc:1"));
        ArrayComplexData fields = new ArrayComplexData(2);
        fields.storeObject(CODEC.encodeKey("title"));
        fields.storeObject(CODEC.encodeValue("Hello"));
        innerResults.storeObject(fields);

        ArrayComplexData data = new ArrayComplexData(2);
        data.storeObject(innerResults);
        data.storeObject(42L);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getCursorId()).isEqualTo(42L);
        assertThat(reply.getCount()).isEqualTo(1);
        assertThat(reply.getResults()).hasSize(1);
        assertThat(reply.getResults().get(0).getId()).isEqualTo("doc:1");
    }

    // ===== RESP3 Tests =====

    @Test
    void shouldParseResp3SearchResult() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);

        MapComplexData extraAttributes = new MapComplexData(1);
        extraAttributes.storeObject(CODEC.encodeKey("title"));
        extraAttributes.storeObject(CODEC.encodeValue("Redis Search"));

        MapComplexData resultEntry = new MapComplexData(3);
        resultEntry.storeObject(CODEC.encodeKey("id"));
        resultEntry.storeObject(CODEC.encodeValue("doc:1"));
        resultEntry.storeObject(CODEC.encodeKey("score"));
        resultEntry.storeObject(1.0);
        resultEntry.storeObject(CODEC.encodeKey("extra_attributes"));
        resultEntry.storeObject(extraAttributes);

        ArrayComplexData resultsList = new ArrayComplexData(1);
        resultsList.storeObject(resultEntry);

        MapComplexData data = new MapComplexData(2);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(1L);
        data.storeObject(CODEC.encodeKey("results"));
        data.storeObject(resultsList);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(1);
        assertThat(reply.getResults()).hasSize(1);
        SearchReply.SearchResult<String, String> result = reply.getResults().get(0);
        assertThat(result.getId()).isEqualTo("doc:1");
        assertThat(result.getScore()).isEqualTo(1.0);
        assertThat(result.getFields()).containsEntry("title", "Redis Search");
    }

    @Test
    void shouldParseResp3WithWarningsAndCursor() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);

        ArrayComplexData warningList = new ArrayComplexData(1);
        warningList.storeObject(CODEC.encodeValue("Timeout limit was reached"));

        MapComplexData data = new MapComplexData(4);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(0L);
        data.storeObject(CODEC.encodeKey("results"));
        data.storeObject(new ArrayComplexData(0));
        data.storeObject(CODEC.encodeKey("warning"));
        data.storeObject(warningList);
        data.storeObject(CODEC.encodeKey("cursor"));
        data.storeObject(99L);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply.getCount()).isEqualTo(0);
        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached");
        assertThat(reply.getCursorId()).isEqualTo(99L);
    }

    @Test
    void shouldReturnEmptyReplyOnMalformedInput() {
        SearchReplyParser<String, String> parser = new SearchReplyParser<>(CODEC, null);
        MapComplexData data = new MapComplexData(0);

        SearchReply<String, String> reply = parser.parse(data);

        assertThat(reply).isNotNull();
        assertThat(reply.getResults()).isEmpty();
    }

}
