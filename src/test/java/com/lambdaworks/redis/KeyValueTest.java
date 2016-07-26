
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class KeyValueTest {

    @Test
    public void shouldCreateEmptyKeyValueFromOptional() {

        KeyValue<String, String> value = KeyValue.from("key", Optional.<String> empty());

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateEmptyValue() {

        KeyValue<String, String> value = KeyValue.empty("key");

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateNonEmptyValueFromOptional() {

        KeyValue<Long, String> value = KeyValue.from(1L, Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void shouldCreateEmptyValueFromValue() {

        KeyValue<String, String> value = KeyValue.fromNullable("key", null);

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateNonEmptyValueFromValue() {

        KeyValue<String, String> value = KeyValue.fromNullable("key", "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void justShouldCreateValueFromValue() {

        KeyValue<String, String> value = KeyValue.just("key", "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getKey()).isEqualTo("key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void justShouldRejectEmptyValueFromValue() {
        Value.just(null);
    }

    @Test
    public void shouldCreateNonEmptyValue() {

        KeyValue<String, String> value = KeyValue.from("key", Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void equals() throws Exception {
        KeyValue<String, String> kv = kv("key", "value");
        assertThat(kv.equals(kv("key", "value"))).isTrue();
        assertThat(kv.equals(null)).isFalse();
        assertThat(kv.equals(kv("a", "value"))).isFalse();
        assertThat(kv.equals(kv("key", "b"))).isFalse();
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(kv("key", "value").hashCode() != 0).isTrue();
    }

    @Test
    public void toStringShouldRenderCorrectly() {

        KeyValue<String, String> value = KeyValue.from("key", Optional.of("hello"));
        KeyValue<String, String> empty = KeyValue.fromNullable("key", null);

        assertThat(value.toString()).isEqualTo("KeyValue[key, hello]");
        assertThat(empty.toString()).isEqualTo("KeyValue[key].empty");
    }

    protected KeyValue<String, String> kv(String key, String value) {
        return KeyValue.just(key, value);
    }
}
