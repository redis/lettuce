/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.TestTags;

/**
 * Unit tests for {@link SearchReply}.
 *
 * @author Tihomir Mateev
 */
@Tag(TestTags.UNIT_TEST)
class SearchResultsTest {

    @Test
    void testEmptySearchResults() {
        SearchReply<String> results = new SearchReply<>();

        assertThat(results.getCount()).isEqualTo(0);
        assertThat(results.getResults()).isEmpty();
        assertThat(results.size()).isEqualTo(0);
        assertThat(results.isEmpty()).isTrue();
    }

    @Test
    void testSearchResultsWithData() {
        SearchReply<String> results = new SearchReply<>();
        results.setCount(10);

        // Create a search result
        SearchReply.SearchResult<String> result1 = new SearchReply.SearchResult<>("doc1");
        result1.setScore(0.95);

        Map<String, byte[]> fields1 = new HashMap<>();
        fields1.put("title", "Test Document 1".getBytes(StandardCharsets.UTF_8));
        fields1.put("content", "This is test content".getBytes(StandardCharsets.UTF_8));
        result1.addFields(fields1);

        results.addResult(result1);

        // Create another search result
        SearchReply.SearchResult<String> result2 = new SearchReply.SearchResult<>("doc2");
        result2.setScore(0.87);

        result2.addField("title", "Test Document 2".getBytes(StandardCharsets.UTF_8));
        result2.addField("content", "This is more test content".getBytes(StandardCharsets.UTF_8));

        results.addResult(result2);

        // Verify results
        assertThat(results.getCount()).isEqualTo(10);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.isEmpty()).isFalse();

        assertThat(results.getResults()).hasSize(2);

        SearchReply.SearchResult<String> firstResult = results.getResults().get(0);
        assertThat(firstResult.getId()).isEqualTo("doc1");
        assertThat(firstResult.getScore()).isEqualTo(0.95);
        assertThat(firstResult.getFields()).containsEntry("title", "Test Document 1");
        assertThat(firstResult.getFields()).containsEntry("content", "This is test content");

        SearchReply.SearchResult<String> secondResult = results.getResults().get(1);
        assertThat(secondResult.getId()).isEqualTo("doc2");
        assertThat(secondResult.getScore()).isEqualTo(0.87);
        assertThat(secondResult.getFields()).containsEntry("title", "Test Document 2");
        assertThat(secondResult.getFields()).containsEntry("content", "This is more test content");
    }

    @Test
    void testGetFieldBytesPreservesBinaryValues() {
        SearchReply.SearchResult<String> result = new SearchReply.SearchResult<>("doc1");

        // a binary value that is not valid UTF-8 (e.g. a little-endian float32 vector)
        byte[] vector = new byte[] { -51, -52, -52, 61, -51, -52, 76, 62 };
        result.addField("embedding", vector);
        result.addField("title", "Lettuce".getBytes(StandardCharsets.UTF_8));

        // getFieldBytes returns the exact bytes, untouched by UTF-8 decoding
        assertThat(result.getFieldBytes("embedding")).isEqualTo(vector);
        assertThat(result.getFieldBytes("title")).isEqualTo("Lettuce".getBytes(StandardCharsets.UTF_8));

        // absent fields yield null
        assertThat(result.getFieldBytes("missing")).isNull();

        // the UTF-8 view still serves the text field
        assertThat(result.getFields().get("title")).isEqualTo("Lettuce");

        // fields added after the decoded view was materialized are still visible through both accessors
        result.addField("category", "greens".getBytes(StandardCharsets.UTF_8));
        assertThat(result.getFields().get("category")).isEqualTo("greens");
        assertThat(result.getFieldBytes("category")).isEqualTo("greens".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testSearchResultsConstructorWithData() {
        SearchReply.SearchResult<String> result = new SearchReply.SearchResult<>("doc1");
        result.setScore(0.95);

        SearchReply<String> results = new SearchReply<>(5, java.util.Arrays.asList(result));

        assertThat(results.getCount()).isEqualTo(5);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.getResults().get(0).getId()).isEqualTo("doc1");
        assertThat(results.getResults().get(0).getScore()).isEqualTo(0.95);
    }

    @Test
    void testSearchResultImmutability() {
        SearchReply<String> results = new SearchReply<>();
        SearchReply.SearchResult<String> result = new SearchReply.SearchResult<>("doc1");
        results.addResult(result);

        // The returned list should be unmodifiable
        assertThat(results.getResults()).hasSize(1);

        // Attempting to modify the returned list should not affect the original
        try {
            results.getResults().clear();
            // If we reach here, the list is modifiable, which is unexpected
            assertThat(false).as("Expected UnsupportedOperationException").isTrue();
        } catch (UnsupportedOperationException e) {
            // This is expected - the list should be unmodifiable
            assertThat(results.getResults()).hasSize(1);
        }
    }

}
