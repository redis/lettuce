
/*
 * Copyright 2011-2020 the original author or authors.
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

import static io.lettuce.core.Value.just;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
class KeyValueUnitTests {

    @Test
    void shouldCreateEmptyKeyValueFromOptional() {

        KeyValue<String, String> value = KeyValue.from("key", Optional.<String> empty());

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    void shouldCreateEmptyValue() {

        KeyValue<String, String> value = KeyValue.empty("key");

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    void shouldCreateNonEmptyValueFromOptional() {

        KeyValue<Long, String> value = KeyValue.from(1L, Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    void shouldCreateEmptyValueFromValue() {

        KeyValue<String, String> value = KeyValue.fromNullable("key", null);

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    void shouldCreateNonEmptyValueFromValue() {

        KeyValue<String, String> value = KeyValue.fromNullable("key", "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    void justShouldCreateValueFromValue() {

        KeyValue<String, String> value = KeyValue.just("key", "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getKey()).isEqualTo("key");
    }

    @Test
    void justShouldRejectEmptyValueFromValue() {
        assertThatThrownBy(() -> just(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateNonEmptyValue() {

        KeyValue<String, String> value = KeyValue.from("key", Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    void equals() {
        KeyValue<String, String> kv = kv("key", "value");
        assertThat(kv.equals(kv("key", "value"))).isTrue();
        assertThat(kv.equals(null)).isFalse();
        assertThat(kv.equals(kv("a", "value"))).isFalse();
        assertThat(kv.equals(kv("key", "b"))).isFalse();
    }

    @Test
    void testHashCode() {
        assertThat(kv("key", "value").hashCode() != 0).isTrue();
    }

    @Test
    void toStringShouldRenderCorrectly() {

        KeyValue<String, String> value = KeyValue.from("key", Optional.of("hello"));
        KeyValue<String, String> empty = KeyValue.fromNullable("key", null);

        assertThat(value.toString()).isEqualTo("KeyValue[key, hello]");
        assertThat(empty.toString()).isEqualTo("KeyValue[key].empty");
    }

    KeyValue<String, String> kv(String key, String value) {
        return KeyValue.just(key, value);
    }

}
