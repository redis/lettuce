// Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
// SPDX-License-Identifier: MIT
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.FieldValue;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.SearchReplyParser;

/**
 * Unit tests for {@link SearchReplyParser} covering the nested value shape produced by the {@code FT.AGGREGATE REDUCE COLLECT}
 * reducer. The reducer contributes one column per group whose value is an array of per-entry maps; the parser must decode this
 * in both RESP2 (each entry is a flat key/value array) and RESP3 (each entry is a map) into {@link FieldValue} structures, and
 * {@link FieldValue#asMap()} must normalize both raw entry shapes to the same map.
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

    private static List<Map<String, FieldValue>> collectedEntries(SearchReply<String> reply, String field) {
        return reply.getResults().get(0).getFields().get(field).asList().stream().map(FieldValue::asMap)
                .collect(Collectors.toList());
    }

    @Test
    void shouldParseCollectColumnFromResp2FlatArrays() {
        SearchReply<String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp2Reply());

        assertThat(parsed.getResults()).hasSize(1);
        Map<String, FieldValue> fields = parsed.getResults().get(0).getFields();
        assertThat(fields.get("color").asString()).isEqualTo("red");

        FieldValue collected = fields.get("items");
        assertThat(collected.getKind()).isEqualTo(FieldValue.Kind.ARRAY);
        List<FieldValue> entries = collected.asList();
        assertThat(entries).hasSize(2);

        // RESP2 keeps the raw entry shape: a flat key/value array.
        assertThat(entries.get(0).getKind()).isEqualTo(FieldValue.Kind.ARRAY);
        assertThat(entries.get(0).asList().stream().map(FieldValue::asString)).containsExactly("fruit", "apple", "sweetness",
                "7");
        // Sparse entry keeps only the fields that were present on the row.
        assertThat(entries.get(1).asList().stream().map(FieldValue::asString)).containsExactly("fruit", "cherry");
    }

    @Test
    void shouldParseCollectColumnFromResp3Maps() {
        SearchReply<String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        assertThat(parsed.getResults()).hasSize(1);
        Map<String, FieldValue> fields = parsed.getResults().get(0).getFields();
        assertThat(fields.get("color").asString()).isEqualTo("red");

        FieldValue collected = fields.get("items");
        assertThat(collected.getKind()).isEqualTo(FieldValue.Kind.ARRAY);
        List<FieldValue> entries = collected.asList();
        assertThat(entries).hasSize(2);

        // RESP3 delivers each entry as a map.
        assertThat(entries.get(0).getKind()).isEqualTo(FieldValue.Kind.MAP);
        Map<String, FieldValue> first = entries.get(0).asMap();
        assertThat(first.get("fruit").asString()).isEqualTo("apple");
        assertThat(first.get("sweetness").asString()).isEqualTo("7");
        // Sparse entry omits the missing key rather than emitting a null placeholder.
        Map<String, FieldValue> second = entries.get(1).asMap();
        assertThat(second.get("fruit").asString()).isEqualTo("cherry");
        assertThat(second).doesNotContainKey("sweetness");
    }

    @Test
    void asMapShouldNormalizeResp2AndResp3EntriesToTheSameShape() {
        SearchReply<String> resp2 = new SearchReplyParser<>(StringCodec.UTF8).parse(resp2Reply());
        SearchReply<String> resp3 = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        List<Map<String, FieldValue>> resp2Entries = collectedEntries(resp2, "items");
        List<Map<String, FieldValue>> resp3Entries = collectedEntries(resp3, "items");

        assertThat(resp2Entries).hasSize(2);
        assertThat(resp2Entries.get(0)).containsExactly(entry("fruit", FieldValue.of("apple".getBytes())),
                entry("sweetness", FieldValue.of("7".getBytes())));
        assertThat(resp2Entries.get(1)).containsExactly(entry("fruit", FieldValue.of("cherry".getBytes())));

        assertThat(resp3Entries).isEqualTo(resp2Entries);
    }

    @Test
    void shouldPreserveNullValuesInsideCollectedEntries() {
        MapComplexData entry = new MapComplexData(2);
        entry.storeObject(k("fruit"));
        entry.storeObject(v("apple"));
        entry.storeObject(k("sweetness"));
        entry.storeObject(null);

        ArrayComplexData items = new ArrayComplexData(1);
        items.storeObject(entry);

        MapComplexData attributes = new MapComplexData(1);
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

        SearchReply<String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(reply);

        Map<String, FieldValue> first = parsed.getResults().get(0).getFields().get("items").asList().get(0).asMap();
        assertThat(first.get("fruit").asString()).isEqualTo("apple");
        assertThat(first.get("sweetness").isNull()).isTrue();
        assertThat(first.get("sweetness")).isSameAs(FieldValue.nullValue());
    }

    @Test
    void shouldParseToListStyleScalarArrayColumn() {
        ArrayComplexData tolist = new ArrayComplexData(3);
        tolist.storeObject(v("apple"));
        tolist.storeObject(v("cherry"));
        tolist.storeObject(v("plum"));

        ArrayComplexData row = new ArrayComplexData(2);
        row.storeObject(v("fruits"));
        row.storeObject(tolist);

        ArrayComplexData reply = new ArrayComplexData(2);
        reply.store(1L);
        reply.storeObject(row);

        SearchReply<String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(reply);

        FieldValue fruits = parsed.getResults().get(0).getFields().get("fruits");
        assertThat(fruits.getKind()).isEqualTo(FieldValue.Kind.ARRAY);
        assertThat(fruits.asList().stream().map(FieldValue::asString)).containsExactly("apple", "cherry", "plum");
        // An odd-length scalar array does not represent key/value pairs.
        assertThatIllegalStateException().isThrownBy(fruits::asMap);
    }

    @Test
    void scalarColumnsShouldRejectComplexAccessors() {
        SearchReply<String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        FieldValue color = parsed.getResults().get(0).getFields().get("color");
        assertThatIllegalStateException().isThrownBy(color::asList).withMessageContaining("SCALAR");
        assertThatIllegalStateException().isThrownBy(color::asMap).withMessageContaining("SCALAR");
    }

    @Test
    void absentFieldIsRepresentedByMissingKey() {
        SearchReply<String> parsed = new SearchReplyParser<>(StringCodec.UTF8).parse(resp3Reply());

        assertThat(parsed.getResults().get(0).getFields()).doesNotContainKey("missing");
    }

}
