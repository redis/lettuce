
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.Optional;

import org.junit.Test;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class ScoredValueTest {

    @Test
    public void shouldCreateEmptyScoredValueFromOptional() {

        ScoredValue<String> value = ScoredValue.from(42, Optional.<String> empty());

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateEmptyValue() {

        ScoredValue<String> value = ScoredValue.empty();

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateNonEmptyValueFromOptional() {

        ScoredValue<String> value = ScoredValue.from(4.2, Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
        assertThat(value.getScore()).isCloseTo(4.2, offset(0.01));
    }

    @Test
    public void shouldCreateEmptyValueFromValue() {

        ScoredValue<String> value = ScoredValue.fromNullable(42, null);

        assertThat(value.hasValue()).isFalse();
    }

    @Test
    public void shouldCreateNonEmptyValueFromValue() {

        ScoredValue<String> value = ScoredValue.fromNullable(42, "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void justShouldCreateValueFromValue() {

        ScoredValue<String> value = ScoredValue.just(42, "hello");

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void justShouldRejectEmptyValueFromValue() {
        Value.just(null);
    }

    @Test
    public void shouldCreateNonEmptyValue() {

        ScoredValue<String> value = ScoredValue.from(12, Optional.of("hello"));

        assertThat(value.hasValue()).isTrue();
        assertThat(value.getValue()).isEqualTo("hello");
    }

    @Test
    public void equals() throws Exception {
        ScoredValue<String> sv1 = ScoredValue.fromNullable(1.0, "a");
        assertThat(sv1.equals(ScoredValue.fromNullable(1.0, "a"))).isTrue();
        assertThat(sv1.equals(null)).isFalse();
        assertThat(sv1.equals(ScoredValue.fromNullable(1.1, "a"))).isFalse();
        assertThat(sv1.equals(ScoredValue.fromNullable(1.0, "b"))).isFalse();
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(ScoredValue.fromNullable(1.0, "a").hashCode() != 0).isTrue();
        assertThat(ScoredValue.fromNullable(0.0, "a").hashCode() != 0).isTrue();
        assertThat(ScoredValue.fromNullable(0.0, null).hashCode() == 0).isTrue();
    }

    @Test
    public void toStringShouldRenderCorrectly() {

        ScoredValue<String> value = ScoredValue.from(12.34, Optional.of("hello"));
        ScoredValue<String> empty = ScoredValue.fromNullable(34, null);

        assertThat(value.toString()).contains("ScoredValue[12").contains("340000, hello]");
        assertThat(empty.toString()).contains("ScoredValue[34").contains("000000].empty");
    }
}
