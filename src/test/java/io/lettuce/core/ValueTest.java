/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public void ifHasValueShouldExecuteCallback() {

        Value<String> value = Value.just("hello");
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        value.ifHasValue(s -> atomicBoolean.set(true));

        assertThat(atomicBoolean.get()).isTrue();
    }

    @Test
    public void emptyValueShouldNotExecuteIfHasValueCallback() {

        Value<String> value = Value.empty();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        value.ifHasValue(s -> atomicBoolean.set(true));

        assertThat(atomicBoolean.get()).isFalse();
    }

    @Test
    public void ifEmptyShouldExecuteCallback() {

        Value<String> value = Value.empty();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        value.ifEmpty(() -> atomicBoolean.set(true));

        assertThat(atomicBoolean.get()).isTrue();
    }

    @Test
    public void valueShouldNotExecuteIfEmptyCallback() {

        Value<String> value = Value.just("hello");
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        value.ifEmpty(() -> atomicBoolean.set(true));

        assertThat(atomicBoolean.get()).isFalse();
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
