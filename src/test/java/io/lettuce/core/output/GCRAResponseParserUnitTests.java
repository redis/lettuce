/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.GCRAResponse;

/**
 * Unit tests for {@link GCRAResponseParser}.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(UNIT_TEST)
class GCRAResponseParserUnitTests {

    @Test
    void shouldParseNotLimitedResponse() {
        ArrayComplexData data = createArrayData(0L, 6L, 5L, -1L, 12L);

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);

        assertThat(response).isNotNull();
        assertThat(response.isLimited()).isFalse();
        assertThat(response.getMaxRequests()).isEqualTo(6);
        assertThat(response.getAvailableRequests()).isEqualTo(5);
        assertThat(response.getRetryAfter()).isEqualTo(-1);
        assertThat(response.getFullBurstAfter()).isEqualTo(12);
    }

    @Test
    void shouldParseLimitedResponse() {
        ArrayComplexData data = createArrayData(1L, 6L, 0L, 30L, 60L);

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);

        assertThat(response).isNotNull();
        assertThat(response.isLimited()).isTrue();
        assertThat(response.getMaxRequests()).isEqualTo(6);
        assertThat(response.getAvailableRequests()).isEqualTo(0);
        assertThat(response.getRetryAfter()).isEqualTo(30);
        assertThat(response.getFullBurstAfter()).isEqualTo(60);
    }

    @Test
    void shouldParseStringValues() {
        ArrayComplexData data = createArrayData("0", "6", "5", "-1", "12");

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);

        assertThat(response).isNotNull();
        assertThat(response.isLimited()).isFalse();
        assertThat(response.getMaxRequests()).isEqualTo(6);
        assertThat(response.getAvailableRequests()).isEqualTo(5);
        assertThat(response.getRetryAfter()).isEqualTo(-1);
        assertThat(response.getFullBurstAfter()).isEqualTo(12);
    }

    @Test
    void shouldHandleNullInput() {
        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(null);
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleEmptyList() {
        ArrayComplexData data = new ArrayComplexData(0);

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleWrongSize() {
        ArrayComplexData data = createArrayData(0L, 6L, 5L);

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleTooManyElements() {
        ArrayComplexData data = createArrayData(0L, 6L, 5L, -1L, 12L, 99L);

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleNonListInput() {
        MapComplexData data = new MapComplexData(2);
        data.storeObject("key");
        data.storeObject("value");

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleUnparsableStringValue() {
        ArrayComplexData data = createArrayData("not_a_number", "6", "5", "-1", "12");

        GCRAResponse response = GCRAResponseParser.INSTANCE.parse(data);

        assertThat(response).isNotNull();
        // unparsable value defaults to 0, so limited = (0 != 0) = false
        assertThat(response.isLimited()).isFalse();
    }

    private ArrayComplexData createArrayData(Object... values) {
        List<Object> list = Arrays.asList(values);
        ArrayComplexData data = new ArrayComplexData(list.size());
        for (Object item : list) {
            data.storeObject(item);
        }
        return data;
    }

}
