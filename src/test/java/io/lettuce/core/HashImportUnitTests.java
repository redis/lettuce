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
        assertThat(fieldset.isDiscarded()).isFalse();
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
    void shouldMarkDiscardedOnClose() {

        HashImport<String> fieldset = HashImport.of("name");
        fieldset.close();

        assertThat(fieldset.isDiscarded()).isTrue();
    }

}
