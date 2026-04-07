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
 * Unit tests for {@link HybridReply}.
 *
 * @author Redis Ltd.
 */
class HybridReplyTest {

    @Test
    void testEmptyHybridReply() {
        HybridReply<String, String> reply = new HybridReply<>();

        assertThat(reply.getTotalResults()).isEqualTo(0);
        assertThat(reply.getExecutionTime()).isEqualTo(0.0);
        assertThat(reply.getResults()).isEmpty();
        assertThat(reply.getWarnings()).isEmpty();
        assertThat(reply.size()).isEqualTo(0);
        assertThat(reply.isEmpty()).isTrue();
    }

    @Test
    void testSetAndGetTotalResults() {
        HybridReply<String, String> reply = new HybridReply<>();
        reply.setTotalResults(42L);

        assertThat(reply.getTotalResults()).isEqualTo(42L);
    }

    @Test
    void testSetAndGetExecutionTime() {
        HybridReply<String, String> reply = new HybridReply<>();
        reply.setExecutionTime(1.23);

        assertThat(reply.getExecutionTime()).isEqualTo(1.23);
    }

    @Test
    void testAddAndGetResults() {
        HybridReply<String, String> reply = new HybridReply<>();

        Map<String, String> result1 = new HashMap<>();
        result1.put("title", "Redis Search");
        result1.put("__key", "doc:1");

        Map<String, String> result2 = new HashMap<>();
        result2.put("title", "Advanced Techniques");

        reply.addResult(result1);
        reply.addResult(result2);

        assertThat(reply.size()).isEqualTo(2);
        assertThat(reply.isEmpty()).isFalse();
        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults().get(0)).containsEntry("title", "Redis Search").containsEntry("__key", "doc:1");
        assertThat(reply.getResults().get(1)).containsEntry("title", "Advanced Techniques");
    }

    @Test
    void testAddAndGetWarnings() {
        HybridReply<String, String> reply = new HybridReply<>();
        reply.addWarning("Timeout limit was reached");
        reply.addWarning("Partial results returned");

        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached", "Partial results returned");
    }

    @Test
    void testGetResultsIsUnmodifiable() {
        HybridReply<String, String> reply = new HybridReply<>();
        reply.addResult(new HashMap<>());

        assertThat(reply.getResults()).hasSize(1);
        try {
            reply.getResults().clear();
            assertThat(false).as("Expected UnsupportedOperationException").isTrue();
        } catch (UnsupportedOperationException e) {
            assertThat(reply.getResults()).hasSize(1);
        }
    }

    @Test
    void testGetWarningsIsUnmodifiable() {
        HybridReply<String, String> reply = new HybridReply<>();
        reply.addWarning("warn");

        assertThat(reply.getWarnings()).hasSize(1);
        try {
            reply.getWarnings().clear();
            assertThat(false).as("Expected UnsupportedOperationException").isTrue();
        } catch (UnsupportedOperationException e) {
            assertThat(reply.getWarnings()).hasSize(1);
        }
    }

}
