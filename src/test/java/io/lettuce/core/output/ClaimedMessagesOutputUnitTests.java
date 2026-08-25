/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.stream.ClaimedMessages;

/**
 * Unit tests for {@link ClaimedMessagesOutput}.
 * <p>
 * The call sequences replay actual {@code XAUTOCLAIM} replies. Redis 7.0 and later reply with three top-level elements (cursor,
 * claimed entries, deleted ids), Redis 6.2 replies with two. The nesting is identical under RESP2 and RESP3.
 *
 * @author big-cir
 */
@Tag(UNIT_TEST)
class ClaimedMessagesOutputUnitTests {

    @Test
    void shouldDecodeClaimedEntries() {

        ClaimedMessagesOutput<String, String> sut = new ClaimedMessagesOutput<>(StringCodec.UTF8, "stream-key", false);

        sut.multi(3);
        sut.set(buffer("0-0"));
        sut.complete(1);
        sut.multi(1);
        sut.multi(2);
        sut.set(buffer("1234-1"));
        sut.complete(3);
        sut.multi(2);
        sut.set(buffer("key"));
        sut.complete(4);
        sut.set(buffer("value"));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);
        sut.multi(2);
        sut.set(buffer("1234-2"));
        sut.complete(2);
        sut.set(buffer("1234-3"));
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        ClaimedMessages<String, String> claimed = sut.get();

        assertThat(claimed.getId()).isEqualTo("0-0");
        assertThat(claimed.getMessages()).hasSize(1);

        StreamMessage<String, String> message = claimed.getMessages().get(0);
        assertThat(message.getStream()).isEqualTo("stream-key");
        assertThat(message.getId()).isEqualTo("1234-1");
        assertThat(message.getBody()).hasSize(1).containsEntry("key", "value");
    }

    @Test
    void shouldDecodeClaimedIdsWithJustId() {

        ClaimedMessagesOutput<String, String> sut = new ClaimedMessagesOutput<>(StringCodec.UTF8, "stream-key", true);

        sut.multi(3);
        sut.set(buffer("0-0"));
        sut.complete(1);
        sut.multi(1);
        sut.set(buffer("1234-1"));
        sut.complete(2);
        sut.complete(1);
        sut.multi(2);
        sut.set(buffer("1234-2"));
        sut.complete(2);
        sut.set(buffer("1234-3"));
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        ClaimedMessages<String, String> claimed = sut.get();

        assertThat(claimed.getId()).isEqualTo("0-0");
        assertThat(claimed.getMessages()).hasSize(1);
        assertThat(claimed.getMessages().get(0).getId()).isEqualTo("1234-1");
        assertThat(claimed.getMessages().get(0).getBody()).isNull();
    }

    @Test
    void shouldNotReportDeletedIdsAsClaimedMessagesWithJustId() {

        ClaimedMessagesOutput<String, String> sut = new ClaimedMessagesOutput<>(StringCodec.UTF8, "stream-key", true);

        sut.multi(3);
        sut.set(buffer("0-0"));
        sut.complete(1);
        sut.multi(0);
        sut.complete(1);
        sut.multi(2);
        sut.set(buffer("1234-1"));
        sut.complete(2);
        sut.set(buffer("1234-2"));
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        ClaimedMessages<String, String> claimed = sut.get();

        assertThat(claimed.getId()).isEqualTo("0-0");
        assertThat(claimed.getMessages()).isEmpty();
    }

    @Test
    void shouldDecodeReplyWithoutDeletedIdsElement() {

        ClaimedMessagesOutput<String, String> sut = new ClaimedMessagesOutput<>(StringCodec.UTF8, "stream-key", false);

        sut.multi(2);
        sut.set(buffer("0-0"));
        sut.complete(1);
        sut.multi(1);
        sut.multi(2);
        sut.set(buffer("1234-1"));
        sut.complete(3);
        sut.multi(2);
        sut.set(buffer("key"));
        sut.complete(4);
        sut.set(buffer("value"));
        sut.complete(4);
        sut.complete(3);
        sut.complete(2);
        sut.complete(1);
        sut.complete(0);

        ClaimedMessages<String, String> claimed = sut.get();

        assertThat(claimed.getId()).isEqualTo("0-0");
        assertThat(claimed.getMessages()).hasSize(1);
        assertThat(claimed.getMessages().get(0).getBody()).containsEntry("key", "value");
    }

    private static ByteBuffer buffer(String value) {
        return ByteBuffer.wrap(value.getBytes());
    }

}
