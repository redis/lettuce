/*
 * Copyright 2025-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.models.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StreamEntryDeletionResult}.
 */
class StreamEntryDeletionResultUnitTests {

    @Test
    void testFromCode() {
        assertThat(StreamEntryDeletionResult.fromCode(-1)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);
        assertThat(StreamEntryDeletionResult.fromCode(1)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(StreamEntryDeletionResult.fromCode(2))
                .isEqualTo(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED);
    }

    @Test
    void testFromCodeInvalid() {
        assertThatThrownBy(() -> StreamEntryDeletionResult.fromCode(99)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown stream entry deletion result code: 99");
    }

    @Test
    void testFromLong() {
        assertThat(StreamEntryDeletionResult.fromLong(-1L)).isEqualTo(StreamEntryDeletionResult.NOT_FOUND);
        assertThat(StreamEntryDeletionResult.fromLong(1L)).isEqualTo(StreamEntryDeletionResult.DELETED);
        assertThat(StreamEntryDeletionResult.fromLong(2L))
                .isEqualTo(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED);
    }

    @Test
    void testFromLongNull() {
        assertThat(StreamEntryDeletionResult.fromLong(null)).isNull();
    }

    @Test
    void testGetCode() {
        assertThat(StreamEntryDeletionResult.NOT_FOUND.getCode()).isEqualTo(-1);
        assertThat(StreamEntryDeletionResult.DELETED.getCode()).isEqualTo(1);
        assertThat(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED.getCode()).isEqualTo(2);
    }

    @Test
    void testToString() {
        assertThat(StreamEntryDeletionResult.NOT_FOUND.toString()).isEqualTo("NOT_FOUND(-1)");
        assertThat(StreamEntryDeletionResult.DELETED.toString()).isEqualTo("DELETED(1)");
        assertThat(StreamEntryDeletionResult.NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED.toString())
                .isEqualTo("NOT_DELETED_UNACKNOWLEDGED_OR_STILL_REFERENCED(2)");
    }

}
