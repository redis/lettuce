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
import io.lettuce.core.search.HybridReply;
import io.lettuce.core.search.HybridReplyParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HybridReplyParser}.
 *
 * @author Redis Ltd.
 */
@Tag(UNIT_TEST)
class HybridReplyParserUnitTests {

    private static final StringCodec CODEC = StringCodec.UTF8;

    // ===== RESP2 Tests =====

    @Test
    void shouldReturnEmptyReplyForEmptyResp2List() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);
        ArrayComplexData data = new ArrayComplexData(0);

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply).isNotNull();
        assertThat(reply.getTotalResults()).isEqualTo(0);
        assertThat(reply.getResults()).isEmpty();
        assertThat(reply.getWarnings()).isEmpty();
    }

    @Test
    void shouldParseResp2WithTotalResultsAndResults() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);

        // Build one result entry as key-value flat list
        ArrayComplexData resultEntry = new ArrayComplexData(4);
        resultEntry.storeObject(CODEC.encodeKey("title"));
        resultEntry.storeObject(CODEC.encodeValue("Redis Search"));
        resultEntry.storeObject(CODEC.encodeKey("__key"));
        resultEntry.storeObject(CODEC.encodeValue("doc:1"));

        ArrayComplexData resultsList = new ArrayComplexData(1);
        resultsList.storeObject(resultEntry);

        // Build top-level RESP2 flat key-value list
        ArrayComplexData data = new ArrayComplexData(8);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(1L);
        data.storeObject(CODEC.encodeKey("execution_time"));
        data.storeObject(CODEC.encodeKey("0.5")); // encoded as string in RESP2
        data.storeObject(CODEC.encodeKey("results"));
        data.storeObject(resultsList);
        data.storeObject(CODEC.encodeKey("warnings"));
        data.storeObject(new ArrayComplexData(0));

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply.getTotalResults()).isEqualTo(1);
        assertThat(reply.getExecutionTime()).isEqualTo(0.5);
        assertThat(reply.getResults()).hasSize(1);
        assertThat(reply.getResults().get(0)).containsEntry("title", "Redis Search").containsEntry("__key", "doc:1");
        assertThat(reply.getWarnings()).isEmpty();
    }

    @Test
    void shouldParseResp2WithExecutionTimeAsDouble() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);

        ArrayComplexData data = new ArrayComplexData(4);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(0L);
        data.storeObject(CODEC.encodeKey("execution_time"));
        data.storeObject(1.23); // Double in RESP2 (e.g. via RESP3 upgrade on same connection)

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply.getExecutionTime()).isEqualTo(1.23);
    }

    @Test
    void shouldParseResp2WithWarnings() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);

        ArrayComplexData warningList = new ArrayComplexData(1);
        warningList.storeObject(CODEC.encodeValue("Timeout limit was reached"));

        ArrayComplexData data = new ArrayComplexData(4);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(0L);
        data.storeObject(CODEC.encodeKey("warnings"));
        data.storeObject(warningList);

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached");
    }

    // ===== RESP3 Tests =====

    @Test
    void shouldReturnEmptyReplyForEmptyResp3Map() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);
        MapComplexData data = new MapComplexData(0);

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply).isNotNull();
        assertThat(reply.getTotalResults()).isEqualTo(0);
        assertThat(reply.getResults()).isEmpty();
    }

    @Test
    void shouldParseResp3WithTotalResultsAndResults() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);

        // One result: a MapComplexData with field entries
        MapComplexData resultEntry = new MapComplexData(2);
        resultEntry.storeObject(CODEC.encodeKey("title"));
        resultEntry.storeObject(CODEC.encodeValue("Redis Search"));
        resultEntry.storeObject(CODEC.encodeKey("__key"));
        resultEntry.storeObject(CODEC.encodeValue("doc:1"));

        ArrayComplexData resultsList = new ArrayComplexData(1);
        resultsList.storeObject(resultEntry);

        MapComplexData data = new MapComplexData(3);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(1L);
        data.storeObject(CODEC.encodeKey("execution_time"));
        data.storeObject(0.75);
        data.storeObject(CODEC.encodeKey("results"));
        data.storeObject(resultsList);

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply.getTotalResults()).isEqualTo(1);
        assertThat(reply.getExecutionTime()).isEqualTo(0.75);
        assertThat(reply.getResults()).hasSize(1);
        assertThat(reply.getResults().get(0)).containsEntry("title", "Redis Search").containsEntry("__key", "doc:1");
    }

    @Test
    void shouldParseResp3WithWarnings() {
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);

        ArrayComplexData warningList = new ArrayComplexData(2);
        warningList.storeObject(CODEC.encodeValue("Timeout limit was reached"));
        warningList.storeObject(CODEC.encodeValue("Results may be incomplete"));

        MapComplexData data = new MapComplexData(3);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(0L);
        data.storeObject(CODEC.encodeKey("results"));
        data.storeObject(new ArrayComplexData(0));
        data.storeObject(CODEC.encodeKey("warnings"));
        data.storeObject(warningList);

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached", "Results may be incomplete");
    }

    @Test
    void shouldReturnEmptyReplyOnMalformedInput() {
        // Passing null or an object that triggers exception path
        HybridReplyParser<String, String> parser = new HybridReplyParser<>(CODEC);
        // An empty list (not a map) with unexpected structure — should not throw
        ArrayComplexData data = new ArrayComplexData(1);
        data.storeObject("not-a-byte-buffer");

        HybridReply<String, String> reply = parser.parse(data);

        assertThat(reply).isNotNull();
    }

}
