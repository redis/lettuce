/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SearchReply}.
 *
 * @author Tihomir Mateev
 */
class SearchResultsTest {

    @Test
    void testEmptySearchResults() {
        SearchReply<String, String> results = new SearchReply<>();

        assertThat(results.getCount()).isEqualTo(0);
        assertThat(results.getResults()).isEmpty();
        assertThat(results.size()).isEqualTo(0);
        assertThat(results.isEmpty()).isTrue();
    }

    @Test
    void testSearchResultsWithData() {
        SearchReply<String, String> results = new SearchReply<>();
        results.setCount(10);

        // Create a search result
        SearchReply.SearchResult<String, String> result1 = new SearchReply.SearchResult<>("doc1");
        result1.setScore(0.95);
        result1.setPayload("payload1");
        result1.setSortKey("sortkey1");

        Map<String, String> fields1 = new HashMap<>();
        fields1.put("title", "Test Document 1");
        fields1.put("content", "This is test content");
        result1.addFields(fields1);

        results.addResult(result1);

        // Create another search result
        SearchReply.SearchResult<String, String> result2 = new SearchReply.SearchResult<>("doc2");
        result2.setScore(0.87);

        Map<String, String> fields2 = new HashMap<>();
        fields2.put("title", "Test Document 2");
        fields2.put("content", "This is more test content");
        result2.addFields(fields2);

        results.addResult(result2);

        // Verify results
        assertThat(results.getCount()).isEqualTo(10);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.isEmpty()).isFalse();

        assertThat(results.getResults()).hasSize(2);

        SearchReply.SearchResult<String, String> firstResult = results.getResults().get(0);
        assertThat(firstResult.getId()).isEqualTo("doc1");
        assertThat(firstResult.getScore()).isEqualTo(0.95);
        assertThat(firstResult.getPayload()).isEqualTo("payload1");
        assertThat(firstResult.getSortKey()).isEqualTo("sortkey1");
        assertThat(firstResult.getFields()).containsEntry("title", "Test Document 1");
        assertThat(firstResult.getFields()).containsEntry("content", "This is test content");

        SearchReply.SearchResult<String, String> secondResult = results.getResults().get(1);
        assertThat(secondResult.getId()).isEqualTo("doc2");
        assertThat(secondResult.getScore()).isEqualTo(0.87);
        assertThat(secondResult.getPayload()).isNull();
        assertThat(secondResult.getSortKey()).isNull();
        assertThat(secondResult.getFields()).containsEntry("title", "Test Document 2");
        assertThat(secondResult.getFields()).containsEntry("content", "This is more test content");
    }

    @Test
    void testSearchResultsConstructorWithData() {
        SearchReply.SearchResult<String, String> result = new SearchReply.SearchResult<>("doc1");
        result.setScore(0.95);

        SearchReply<String, String> results = new SearchReply<>(5, java.util.Arrays.asList(result));

        assertThat(results.getCount()).isEqualTo(5);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.getResults().get(0).getId()).isEqualTo("doc1");
        assertThat(results.getResults().get(0).getScore()).isEqualTo(0.95);
    }

    @Test
    void testSearchResultImmutability() {
        SearchReply<String, String> results = new SearchReply<>();
        SearchReply.SearchResult<String, String> result = new SearchReply.SearchResult<>("doc1");
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
