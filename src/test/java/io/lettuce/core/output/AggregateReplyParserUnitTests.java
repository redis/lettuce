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
import io.lettuce.core.search.AggregateReplyParser;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.SearchReply;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AggregateReplyParser}.
 *
 * @author Redis Ltd.
 */
@Tag(UNIT_TEST)
class AggregateReplyParserUnitTests {

    private static final StringCodec CODEC = StringCodec.UTF8;

    @Test
    void shouldReturnEmptyReplyForNullData() {
        AggregateReplyParser<String, String> parser = new AggregateReplyParser<>(CODEC, false);

        AggregationReply<String, String> reply = parser.parse(null);

        assertThat(reply).isNotNull();
        assertThat(reply.getReplies()).isEmpty();
    }

    @Test
    void shouldParseResp2DataWithoutCursor() {
        // Without cursor: data is passed directly to SearchReplyParser (no-ID mode).
        // Format: [count, fields_complexData, ...]
        AggregateReplyParser<String, String> parser = new AggregateReplyParser<>(CODEC, false);

        ArrayComplexData fields = new ArrayComplexData(4);
        fields.storeObject(CODEC.encodeKey("category"));
        fields.storeObject(CODEC.encodeValue("electronics"));
        fields.storeObject(CODEC.encodeKey("count"));
        fields.storeObject(CODEC.encodeValue("5"));

        ArrayComplexData data = new ArrayComplexData(2);
        data.storeObject(1L);
        data.storeObject(fields);

        AggregationReply<String, String> reply = parser.parse(data);

        assertThat(reply.getReplies()).hasSize(1);
        SearchReply<String, String> searchReply = reply.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1);
        assertThat(searchReply.getResults().get(0).getFields()).containsEntry("category", "electronics").containsEntry("count",
                "5");
    }

    @Test
    void shouldParseResp2DataWithCursor() {
        // With cursor: format is [groupCount, resultsComplexData, cursorId].
        AggregateReplyParser<String, String> parser = new AggregateReplyParser<>(CODEC, true);

        ArrayComplexData fields = new ArrayComplexData(2);
        fields.storeObject(CODEC.encodeKey("brand"));
        fields.storeObject(CODEC.encodeValue("apple"));

        // Inner results in SearchReplyParser (no-ID) format: [count, fields_complexData]
        ArrayComplexData innerResults = new ArrayComplexData(2);
        innerResults.storeObject(1L);
        innerResults.storeObject(fields);

        ArrayComplexData data = new ArrayComplexData(3);
        data.storeObject(3L); // groupCount
        data.storeObject(innerResults);
        data.storeObject(77L); // cursorId

        AggregationReply<String, String> reply = parser.parse(data);

        assertThat(reply.getAggregationGroups()).isEqualTo(3);
        assertThat(reply.getReplies()).hasSize(1);
        assertThat(reply.getCursor()).isPresent();
        assertThat(reply.getCursor().get().getCursorId()).isEqualTo(77L);
        SearchReply<String, String> searchReply = reply.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1);
        assertThat(searchReply.getResults().get(0).getFields()).containsEntry("brand", "apple");
    }

    @Test
    void shouldReturnEmptyReplyForEmptyListWithCursor() {
        AggregateReplyParser<String, String> parser = new AggregateReplyParser<>(CODEC, true);
        ArrayComplexData data = new ArrayComplexData(0);

        AggregationReply<String, String> reply = parser.parse(data);

        assertThat(reply).isNotNull();
        assertThat(reply.getReplies()).isEmpty();
    }

    @Test
    void shouldParseResp3DataWithoutCursor() {
        // Without cursor: RESP3 map is passed directly to SearchReplyParser.
        AggregateReplyParser<String, String> parser = new AggregateReplyParser<>(CODEC, false);

        MapComplexData extraAttributes = new MapComplexData(1);
        extraAttributes.storeObject(CODEC.encodeKey("category"));
        extraAttributes.storeObject(CODEC.encodeValue("computers"));

        MapComplexData resultEntry = new MapComplexData(1);
        resultEntry.storeObject(CODEC.encodeKey("extra_attributes"));
        resultEntry.storeObject(extraAttributes);

        ArrayComplexData resultsList = new ArrayComplexData(1);
        resultsList.storeObject(resultEntry);

        MapComplexData data = new MapComplexData(2);
        data.storeObject(CODEC.encodeKey("total_results"));
        data.storeObject(1L);
        data.storeObject(CODEC.encodeKey("results"));
        data.storeObject(resultsList);

        AggregationReply<String, String> reply = parser.parse(data);

        assertThat(reply.getReplies()).hasSize(1);
        SearchReply<String, String> searchReply = reply.getReplies().get(0);
        assertThat(searchReply.getResults()).hasSize(1);
        assertThat(searchReply.getResults().get(0).getFields()).containsEntry("category", "computers");
    }

}
