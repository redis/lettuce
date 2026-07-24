/*
 * Copyright 2026-present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HybridReply}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(UNIT_TEST)
class HybridReplyTest {

    @Test
    void testEmptyHybridReply() {
        HybridReply reply = new HybridReply();

        assertThat(reply.getTotalResults()).isEqualTo(0);
        assertThat(reply.getExecutionTime()).isEqualTo(0.0);
        assertThat(reply.getResults()).isEmpty();
        assertThat(reply.getWarnings()).isEmpty();
        assertThat(reply.size()).isEqualTo(0);
        assertThat(reply.isEmpty()).isTrue();
    }

    @Test
    void testSetAndGetTotalResults() {
        HybridReply reply = new HybridReply();
        reply.setTotalResults(42L);

        assertThat(reply.getTotalResults()).isEqualTo(42L);
    }

    @Test
    void testSetAndGetExecutionTime() {
        HybridReply reply = new HybridReply();
        reply.setExecutionTime(1.23);

        assertThat(reply.getExecutionTime()).isEqualTo(1.23);
    }

    @Test
    void testAddAndGetResults() {
        HybridReply reply = new HybridReply();

        HybridReply.HybridResult result1 = new HybridReply.HybridResult();
        result1.addField("title", "Redis Search".getBytes(StandardCharsets.UTF_8));
        result1.addField("__key", "doc:1".getBytes(StandardCharsets.UTF_8));

        HybridReply.HybridResult result2 = new HybridReply.HybridResult();
        result2.addField("title", "Advanced Techniques".getBytes(StandardCharsets.UTF_8));

        reply.addResult(result1);
        reply.addResult(result2);

        assertThat(reply.size()).isEqualTo(2);
        assertThat(reply.isEmpty()).isFalse();
        assertThat(reply.getResults()).hasSize(2);
        assertThat(reply.getResults().get(0).getFields().get("title").asString()).isEqualTo("Redis Search");
        assertThat(reply.getResults().get(0).getFields().get("__key").asString()).isEqualTo("doc:1");
        assertThat(reply.getResults().get(1).getFields().get("title").asString()).isEqualTo("Advanced Techniques");
    }

    @Test
    void testFieldValuesExposeTextAndBinary() {
        HybridReply.HybridResult result = new HybridReply.HybridResult();

        // a binary value that is not valid UTF-8 (e.g. a little-endian float32 vector)
        byte[] vector = new byte[] { -51, -52, -52, 61, -51, -52, 76, 62 };
        result.addField("embedding", vector);
        result.addField("title", "Lettuce".getBytes(StandardCharsets.UTF_8));

        Map<String, FieldValue> fields = result.getFields();

        assertThat(fields.get("title").asString()).isEqualTo("Lettuce");
        assertThat(fields.get("embedding").asBytes()).isEqualTo(vector);
        assertThat(fields.get("missing")).isNull();
        assertThat(fields).containsOnlyKeys("embedding", "title");
    }

    @Test
    void testAddAndGetWarnings() {
        HybridReply reply = new HybridReply();
        reply.addWarning("Timeout limit was reached");
        reply.addWarning("Partial results returned");

        assertThat(reply.getWarnings()).containsExactly("Timeout limit was reached", "Partial results returned");
    }

    @Test
    void testGetResultsIsUnmodifiable() {
        HybridReply reply = new HybridReply();
        reply.addResult(new HybridReply.HybridResult());

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
        HybridReply reply = new HybridReply();
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
