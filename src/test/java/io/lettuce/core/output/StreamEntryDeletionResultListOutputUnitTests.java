/*
 * Copyright 2025-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.stream.StreamEntryDeletionResult;

/**
 * Unit tests for {@link StreamEntryDeletionResultListOutput}.
 */
class StreamEntryDeletionResultListOutputUnitTests {

    private StreamEntryDeletionResultListOutput<String, String> output = new StreamEntryDeletionResultListOutput<>(
            StringCodec.UTF8);

    @Test
    void shouldParseEmptyList() {
        output.multi(0);

        List<StreamEntryDeletionResult> result = output.get();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldParseSingleResult() {
        output.multi(1);
        output.set(1L); // DELETED

        List<StreamEntryDeletionResult> result = output.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
    }

    @Test
    void shouldParseMultipleResults() {
        output.multi(3);
        output.set(-1L); // NOT_FOUND
        output.set(1L); // DELETED
        output.set(2L); // NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED

        List<StreamEntryDeletionResult> result = output.get();
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);
        assertThat(result.get(1)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(result.get(2)).isEqualTo(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED);
    }

    @Test
    void shouldHandleStreamingOutput() {
        TestSubscriber subscriber = new TestSubscriber();
        output.setSubscriber(subscriber);

        output.multi(2);
        output.set(1L);
        output.set(-1L);

        assertThat(subscriber.results).hasSize(2);
        assertThat(subscriber.results.get(0)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(subscriber.results.get(1)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);
    }

    private static class TestSubscriber extends StreamingOutput.Subscriber<StreamEntryDeletionResult> {

        private final List<StreamEntryDeletionResult> results = new java.util.ArrayList<>();

        @Override
        public void onNext(StreamEntryDeletionResult item) {
            results.add(item);
        }

        @Override
        public void onNext(Collection<StreamEntryDeletionResult> outputTarget, StreamEntryDeletionResult item) {
            results.add(item);
            outputTarget.add(item);
        }

    }

}
