/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link ErrorTolerantIntegerListOutput}, the TS.MADD output of the sync and async API: an error element of the
 * reply array becomes {@code null}, an error before the array fails the command.
 */
@Tag(UNIT_TEST)
class ErrorTolerantIntegerListOutputUnitTests {

    private static final String BLOCK_ERROR = "ERR TSDB: Error at upsert, update is not supported when DUPLICATE_POLICY is set to BLOCK mode";

    private final ErrorTolerantIntegerListOutput<String, String> sut = new ErrorTolerantIntegerListOutput<>(StringCodec.UTF8);

    private static ByteBuffer bytes(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Scenario U06: the verified MADD reply {@code *2 -ERR… :9} gives {@code [null, 9]}; the command does not fail.
     */
    @Test
    void errorElementBecomesNull() {

        sut.multi(2);
        sut.setError(bytes(BLOCK_ERROR));
        sut.set(9L);

        assertThat(sut.get()).containsExactly(null, 9L);
        assertThat(sut.hasError()).isFalse();
        assertThat(sut.getError()).isNull();
    }

    /**
     * Scenario U06: the missing-key reply {@code *2 :1 -ERR TSDB: the key is not a TSDB key} gives {@code [1, null]}.
     */
    @Test
    void errorElementAtTheEnd() {

        sut.multi(2);
        sut.set(1L);
        sut.setError(bytes("ERR TSDB: the key is not a TSDB key"));

        assertThat(sut.get()).containsExactly(1L, null);
        assertThat(sut.hasError()).isFalse();
    }

    /**
     * Scenario U06: every element rejected, and no element rejected.
     */
    @Test
    void allOrNoErrors() {

        sut.multi(3);
        sut.setError(bytes("ERR a"));
        sut.setError(bytes("ERR b"));
        sut.setError(bytes("ERR c"));
        assertThat(sut.get()).containsExactly(null, null, null);

        ErrorTolerantIntegerListOutput<String, String> ok = new ErrorTolerantIntegerListOutput<>(StringCodec.UTF8);
        ok.multi(3);
        ok.set(1000L);
        ok.set(2000L);
        ok.set(1000L);
        assertThat(ok.get()).isEqualTo(Arrays.asList(1000L, 2000L, 1000L));
    }

    /**
     * Scenario U06: an error before the array fails the command.
     */
    @Test
    void topLevelErrorFailsTheCommand() {

        sut.setError(bytes("ERR wrong number of arguments for 'ts.madd' command"));

        assertThat(sut.hasError()).isTrue();
        assertThat(sut.getError()).isEqualTo("ERR wrong number of arguments for 'ts.madd' command");
    }

    /**
     * Scenario U06: an empty reply array gives an empty list.
     */
    @Test
    void emptyArray() {

        sut.multi(0);

        assertThat(sut.get()).isEmpty();
        assertThat(sut.hasError()).isFalse();
    }

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull();
    }

}
