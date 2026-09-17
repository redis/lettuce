/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HashImport}.
 */
@Tag(UNIT_TEST)
class HashImportUnitTests {

    @Test
    void shouldCreateWithGeneratedName() {

        HashImport<String> fieldset = HashImport.of("name", "email", "age");

        assertThat(fieldset.name()).startsWith("himport:");
        assertThat(fieldset.size()).isEqualTo(3);
        assertThat(fieldset.fields()).containsExactly("name", "email", "age");

        assertThat(fieldset.retain()).as("a fresh fieldset admits imports").isTrue();
        fieldset.release();
    }

    @Test
    void shouldGenerateUniqueNames() {

        HashImport<String> first = HashImport.of("a");
        HashImport<String> second = HashImport.of("a");

        assertThat(first.name()).isNotEqualTo(second.name());
    }

    @Test
    void shouldDeriveNameFromIdCodec() {

        HashImport<String> fieldset = HashImport.of(seq -> "fs", "name");

        assertThat(fieldset.name()).isEqualTo("fs");
    }

    @Test
    void shouldReturnDefensiveCopyOfFields() {

        HashImport<String> fieldset = HashImport.of("name", "email");
        String[] fields = fieldset.fields();
        fields[0] = "mutated";

        assertThat(fieldset.fields()).containsExactly("name", "email");
    }

    @Test
    void shouldRejectEmptyFields() {
        assertThatThrownBy(() -> HashImport.of()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDuplicateFields() {
        assertThatThrownBy(() -> HashImport.of("name", "email", "name")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullField() {
        assertThatThrownBy(() -> HashImport.of("name", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRunCleanupOnCloseWithNothingInFlight() {

        HashImport<String> fieldset = HashImport.of("name");
        fieldset.close();

        assertThat(fieldset.registerConnection(new HashImportContext()))
                .as("nothing was in flight, so cleanup ran straight away and no PREPARE may follow").isFalse();
    }

    /**
     * The try-with-resources idiom over the asynchronous API: the imports are admitted on the caller's thread, but their write
     * (and therefore the {@code PREPARE} injection) happens later on the event loop, after {@code close()} has already run.
     * Cleanup must wait for them instead of pre-empting them.
     */
    @Test
    void shouldDeferCleanupUntilInFlightImportsComplete() {

        HashImport<String> fieldset = HashImport.of("name", "email");

        assertThat(fieldset.retain()).isTrue();

        fieldset.close();

        assertThat(fieldset.retain()).as("close() rejects new imports from the moment it returns").isFalse();
        assertThat(fieldset.registerConnection(new HashImportContext()))
                .as("cleanup must not pre-empt the in-flight import, which still has to declare its fieldset").isTrue();

        fieldset.release();

        assertThat(fieldset.registerConnection(new HashImportContext())).as("no PREPARE may be sent once cleanup has run")
                .isFalse();
    }

    @Test
    void shouldRejectNewImportsAfterClose() {

        HashImport<String> fieldset = HashImport.of("name");
        fieldset.close();

        assertThat(fieldset.retain()).isFalse();
    }

    @Test
    void shouldTolerateRepeatedClose() {

        HashImport<String> fieldset = HashImport.of("name");

        assertThat(fieldset.retain()).isTrue();
        fieldset.close();
        fieldset.close();

        assertThat(fieldset.registerConnection(new HashImportContext()))
                .as("a second close() must not pre-empt the in-flight import either").isTrue();

        fieldset.release();

        assertThat(fieldset.registerConnection(new HashImportContext())).isFalse();
    }

}
