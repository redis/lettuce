// Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
// SPDX-License-Identifier: MIT
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SearchReplyParser;

/**
 * Unit tests for {@link SearchReplyParser} covering the nested value shape produced by the {@code FT.AGGREGATE REDUCE COLLECT}
 * reducer. The reducer contributes one column per group whose value is an array of per-entry maps; the parser must decode this
 * in both RESP2 (each entry is a flat key/value array) and RESP3 (each entry is a map) without failing, and
 * {@link SearchReply.SearchResult#getCollectedEntries(Object)} must normalize both raw shapes to the same list of maps.
 */
@Tag(UNIT_TEST)
class SearchReplyCollectParserUnitTests {

    private static ByteBuffer v(String s) {
        return StringCodec.UTF8.encodeValue(s);
    }

    private static ByteBuffer k(String s) {
        return StringCodec.UTF8.encodeKey(s);
    }

    /**
     * Builds the RESP2 reply for the aggregation row {@code color=red, items=[ [fruit,apple,sweetness,7], [fruit,cherry] ]}.
     */
    private static ComplexData resp2Reply() {
        ArrayComplexData entry1 = new ArrayComplexData(4);
        entry1.storeObject(v("fruit"));
        entry1.storeObject(v("apple"));
        entry1.storeObject(v("sweetness"));
        entry1.storeObject(v("7"));

        ArrayComplexData entry2 = new ArrayComplexData(2); // sparse entry, no sweetness
        entry2.storeObject(v("fruit"));
        entry2.storeObject(v("cherry"));

        ArrayComplexData items = new ArrayComplexData(2);
        items.storeObject(entry1);
        items.storeObject(entry2);

        ArrayComplexData row = new ArrayComplexData(4);
        row.storeObject(v("color"));
        row.storeObject(v("red"));
        row.storeObject(v("items"));
        row.storeObject(items);

        ArrayComplexData reply = new ArrayComplexData(2);
        reply.store(1L); // total results
        reply.storeObject(row);
        return reply;
    }

    /**
     * Builds the RESP3 reply for {@code extra_attributes: { color: red, items: [ {fruit:apple, sweetness:7}, {fruit:cherry} ]
     * }}.
     */
    private static ComplexData resp3Reply() {
        MapComplexData entry1 = new MapComplexData(2);
        entry1.storeObject(k("fruit"));
        entry1.storeObject(v("apple"));
        entry1.storeObject(k("sweetness"));
        entry1.storeObject(v("7"));

        MapComplexData entry2 = new MapComplexData(1); // sparse entry
        entry2.storeObject(k("fruit"));
        entry2.storeObject(v("cherry"));

        ArrayComplexData items = new ArrayComplexData(2);
        items.storeObject(entry1);
        items.storeObject(entry2);

        MapComplexData attributes = new MapComplexData(2);
        attributes.storeObject(k("color"));
        attributes.storeObject(v("red"));
        attributes.storeObject(k("items"));
        attributes.storeObject(items);

        MapComplexData resultEntry = new MapComplexData(1);
        resultEntry.storeObject(k("extra_attributes"));
        resultEntry.storeObject(attributes);

        ArrayComplexData results = new ArrayComplexData(1);
        results.storeObject(resultEntry);

        MapComplexData reply = new MapComplexData(2);
        reply.storeObject(k("results"));
        reply.storeObject(results);
        reply.storeObject(k("total_results"));
        reply.store(1L);
        return reply;
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseCollectColumnFromResp2FlatArrays() {
        SearchReply<String, String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp2Reply());

        assertThat(parsed.getResults()).hasSize(1);
        Map<String, String> fields = parsed.getResults().get(0).getFields();
        assertThat(fields.get("color")).isEqualTo("red");

        Object collected = fields.get("items");
        assertThat(collected).isInstanceOf(List.class);
        List<Object> entries = (List<Object>) collected;
        assertThat(entries).hasSize(2);

        assertThat((List<Object>) entries.get(0)).containsExactly("fruit", "apple", "sweetness", "7");
        // Sparse entry keeps only the fields that were present on the row.
        assertThat((List<Object>) entries.get(1)).containsExactly("fruit", "cherry");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseCollectColumnFromResp3Maps() {
        SearchReply<String, String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        assertThat(parsed.getResults()).hasSize(1);
        Map<String, String> fields = parsed.getResults().get(0).getFields();
        assertThat(fields.get("color")).isEqualTo("red");

        Object collected = fields.get("items");
        assertThat(collected).isInstanceOf(List.class);
        List<Object> entries = (List<Object>) collected;
        assertThat(entries).hasSize(2);

        Map<String, String> first = (Map<String, String>) entries.get(0);
        assertThat(first).containsEntry("fruit", "apple").containsEntry("sweetness", "7");
        // Sparse entry omits the missing key rather than emitting a null placeholder.
        Map<String, String> second = (Map<String, String>) entries.get(1);
        assertThat(second).containsEntry("fruit", "cherry").doesNotContainKey("sweetness");
    }

    @Test
    void getCollectedEntriesShouldNormalizeResp2AndResp3ToTheSameShape() {
        SearchReply<String, String> resp2 = new SearchReplyParser<>(StringCodec.UTF8).parse(resp2Reply());
        SearchReply<String, String> resp3 = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        List<Map<String, String>> resp2Entries = resp2.getResults().get(0).getCollectedEntries("items");
        List<Map<String, String>> resp3Entries = resp3.getResults().get(0).getCollectedEntries("items");

        assertThat(resp2Entries).hasSize(2);
        assertThat(resp2Entries.get(0)).containsExactly(entry("fruit", "apple"), entry("sweetness", "7"));
        assertThat(resp2Entries.get(1)).containsExactly(entry("fruit", "cherry"));

        assertThat(resp3Entries).isEqualTo(resp2Entries);
    }

    @Test
    void getCollectedEntriesShouldReturnEmptyListForAbsentField() {
        SearchReply<String, String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        assertThat(parsed.getResults().get(0).getCollectedEntries("missing")).isEmpty();
    }

    @Test
    void getCollectedEntriesShouldRejectScalarColumns() {
        SearchReply<String, String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        assertThatIllegalArgumentException().isThrownBy(() -> parsed.getResults().get(0).getCollectedEntries("color"))
                .withMessageContaining("color");
    }

}
