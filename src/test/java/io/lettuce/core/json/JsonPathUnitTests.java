/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JsonPath}.
 */
@Tag(UNIT_TEST)
class JsonPathUnitTests {

    @Test
    void equalPathsShouldBeEqual() {
        JsonPath first = JsonPath.of("$..commuter_bikes");
        JsonPath second = JsonPath.of("$..commuter_bikes");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(JsonPath.of("$")).isEqualTo(JsonPath.ROOT_PATH);
    }

    @Test
    void differentPathsShouldNotBeEqual() {
        assertThat(JsonPath.of("$.a")).isNotEqualTo(JsonPath.of("$.b"));
    }

    @Test
    void pathShouldNotEqualItsString() {
        assertThat(JsonPath.of("$")).isNotEqualTo((Object) "$");
    }

    @Test
    void equalPathsShouldWorkAsSetElements() {
        Set<JsonPath> paths = new HashSet<>();
        paths.add(JsonPath.of("$.a"));
        paths.add(JsonPath.of("$.a"));

        assertThat(paths).hasSize(1).contains(JsonPath.of("$.a"));
    }

}
