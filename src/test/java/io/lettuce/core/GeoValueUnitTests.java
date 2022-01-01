/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GeoValue}.
 *
 * @author Mark Paluch
 */
class GeoValueUnitTests {

    @Test
    void shouldCreateValueFromOptional() {

        Value<String> value = GeoValue.from(new GeoCoordinates(1, 2), Optional.empty());

        assertThat(value.hasValue()).isFalse();

        value = GeoValue.from(new GeoCoordinates(1, 2), Optional.of("foo"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value).isInstanceOf(GeoValue.class);
    }

    @Test
    void shouldCreateValueFromNullable() {

        Value<String> value = GeoValue.fromNullable(new GeoCoordinates(1, 2), null);

        assertThat(value.hasValue()).isFalse();

        value = GeoValue.fromNullable(new GeoCoordinates(1, 2), "foo");

        assertThat(value.hasValue()).isTrue();
        assertThat(value).isInstanceOf(GeoValue.class);
    }

    @Test
    void shouldCreateEmptyValue() {

        Value<String> value = GeoValue.empty();

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    void justShouldCreateValueFromValue() {

        GeoValue<String> value = GeoValue.just(42, 43, "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    void justShouldRejectEmptyValueFromValue() {
        assertThatThrownBy(() -> GeoValue.just(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equals() {
        GeoValue<String> sv1 = GeoValue.just(1.0, 2.0, "a");
        assertThat(sv1.equals(GeoValue.just(1.0, 2.0, "a"))).isTrue();
        assertThat(sv1.equals(null)).isFalse();
        assertThat(sv1.equals(GeoValue.just(1.1, 2.0, "a"))).isFalse();
        assertThat(sv1.equals(GeoValue.just(1.0, 2.0, "b"))).isFalse();
    }

    @Test
    void testHashCode() {
        assertThat(GeoValue.just(1.0, 2.0, "a").hashCode() != 0).isTrue();
        assertThat(GeoValue.just(0.0, 2.0, "a").hashCode() != 0).isTrue();
    }

    @Test
    void toStringShouldRenderCorrectly() {

        assertThat(GeoValue.just(12, 34, "hello")).hasToString("GeoValue[(12.0, 34.0), hello]");
    }

}
