package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ValueTest {

    @Test
    public void shouldCreateEmptyValueFromOptional() {

        Value<String> value = Value.from(Optional.<String> empty());

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateEmptyValue() {

        Value<String> value = Value.empty();

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateNonEmptyValueFromOptional() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void shouldCreateEmptyValueFromValue() {

        Value<String> value = Value.fromNullable(null);

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateNonEmptyValueFromValue() {

        Value<String> value = Value.fromNullable("hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void justShouldCreateValueFromValue() {

        Value<String> value = Value.just("hello");

        assertThat(value.hasValue()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void justShouldRejectEmptyValueFromValue() {
        Value.just(null);
    }

    @Test
    public void shouldCreateNonEmptyValue() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void optionalShouldReturnOptional() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.optional()).hasValue("hello");
    }

    @Test
    public void emptyValueOptionalShouldReturnOptional() {

        Value<String> value = Value.from(Optional.empty());

        assertThat(value.optional()).isEmpty();
    }

    @Test
    public void getValueOrElseShouldReturnValue() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.getValueOrElse("world")).isEqualTo("hello");
    }

    @Test
    public void getValueOrElseShouldReturnOtherValue() {

        Value<String> value = Value.from(Optional.empty());

        assertThat(value.getValueOrElse("world")).isEqualTo("world");
    }

    @Test
    public void orElseThrowShouldReturnValue() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.getValueOrElseThrow(IllegalArgumentException::new)).isEqualTo("hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyValueGetValueOrElseShouldThrowException() {

        Value<String> value = Value.from(Optional.empty());

        value.getValueOrElseThrow(IllegalArgumentException::new);
    }

    @Test
    public void getValueOrElseGetShouldReturnValue() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.getValueOrElseGet(() -> "world")).isEqualTo("hello");
    }

    @Test
    public void emptyValueGetValueOrElseGetShouldReturnOtherValue() {

        Value<String> value = Value.from(Optional.empty());

        assertThat(value.getValueOrElseGet(() -> "world")).isEqualTo("world");
    }

    @Test
    public void mapShouldMapValue() {

        Value<String> value = Value.from(Optional.of("hello"));

        assertThat(value.map(s -> s + "-world").getValue()).isEqualTo("hello-world");
    }

    @Test
    public void emptyValueMapShouldNotMapEmptyValue() {

        Value<String> value = Value.from(Optional.empty());

        assertThat(value.map(s -> s + "-world")).isSameAs(value);
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyValueGetEmptyValueShouldThrowException() {
        Value.from(Optional.<String> empty()).getValue();
    }

    @Test
    public void shouldBeEquals() {

        Value<String> value = Value.from(Optional.of("hello"));
        Value<String> other = Value.fromNullable("hello");
        Value<String> different = Value.fromNullable("different");

        assertThat(value).isEqualTo(other);
        assertThat(value).isNotEqualTo(different);

        assertThat(value.hashCode()).isEqualTo(other.hashCode());
        assertThat(value.hashCode()).isNotEqualTo(different.hashCode());
    }

    @Test
    public void toStringShouldRenderCorrectly() {

        Value<String> value = Value.from(Optional.of("hello"));
        Value<String> empty = Value.fromNullable(null);

        assertThat(value.toString()).isEqualTo("Value[hello]");
        assertThat(empty.toString()).isEqualTo("Value.empty");
    }

    @Test
    public void emptyValueStreamShouldCreateEmptyStream() {

        Value<String> empty = Value.fromNullable(null);

        assertThat(empty.stream().count()).isEqualTo(0);
    }

    @Test
    public void streamShouldCreateAStream() {

        Value<String> empty = Value.fromNullable("hello");

        assertThat(empty.stream().count()).isEqualTo(1);
    }
}