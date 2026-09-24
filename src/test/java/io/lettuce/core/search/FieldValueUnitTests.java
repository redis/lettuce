// Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
// SPDX-License-Identifier: MIT
package io.lettuce.core.search;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FieldValue}.
 */
@Tag(UNIT_TEST)
class FieldValueUnitTests {

    private static FieldValue scalar(String value) {
        return FieldValue.of(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldDiscriminateKinds() {
        assertThat(scalar("a").getKind()).isEqualTo(FieldValue.Kind.SCALAR);
        assertThat(FieldValue.array(Collections.emptyList()).getKind()).isEqualTo(FieldValue.Kind.ARRAY);
        assertThat(FieldValue.map(Collections.emptyMap()).getKind()).isEqualTo(FieldValue.Kind.MAP);
        assertThat(FieldValue.nullValue().getKind()).isEqualTo(FieldValue.Kind.NULL);

        assertThat(scalar("a").isNull()).isFalse();
        assertThat(FieldValue.nullValue().isNull()).isTrue();
    }

    @Test
    void scalarShouldExposeBytesAndText() {
        byte[] bytes = "héllo".getBytes(StandardCharsets.UTF_8);
        FieldValue value = FieldValue.of(bytes);

        assertThat(value.asBytes()).isSameAs(bytes);
        assertThat(value.asString()).isEqualTo("héllo");
        assertThat(value.asString(StandardCharsets.ISO_8859_1)).isEqualTo(new String(bytes, StandardCharsets.ISO_8859_1));
    }

    @Test
    void binaryScalarShouldSurviveByteAccess() {
        byte[] binary = new byte[] { (byte) 0x91, (byte) 0xC3, 0x28, 0x00, (byte) 0xFF };
        assertThat(FieldValue.of(binary).asBytes()).isSameAs(binary);
    }

    @Test
    void nullValueShouldReturnNullFromAllAccessors() {
        FieldValue value = FieldValue.nullValue();

        assertThat(value.asBytes()).isNull();
        assertThat(value.asString()).isNull();
        assertThat(value.asString(StandardCharsets.US_ASCII)).isNull();
        assertThat(value.asList()).isNull();
        assertThat(value.asMap()).isNull();
    }

    @Test
    void mismatchedAccessorsShouldThrow() {
        FieldValue array = FieldValue.array(Collections.singletonList(scalar("a")));
        FieldValue map = FieldValue.map(Collections.singletonMap("k", scalar("v")));

        assertThatIllegalStateException().isThrownBy(() -> scalar("a").asList()).withMessageContaining("SCALAR");
        assertThatIllegalStateException().isThrownBy(() -> scalar("a").asMap()).withMessageContaining("SCALAR");
        assertThatIllegalStateException().isThrownBy(array::asBytes).withMessageContaining("ARRAY");
        assertThatIllegalStateException().isThrownBy(array::asString).withMessageContaining("ARRAY");
        assertThatIllegalStateException().isThrownBy(map::asBytes).withMessageContaining("MAP");
        assertThatIllegalStateException().isThrownBy(map::asString).withMessageContaining("MAP");
        assertThatIllegalStateException().isThrownBy(map::asList).withMessageContaining("MAP");
    }

    @Test
    void asMapShouldInterpretFlatPairArray() {
        FieldValue pairs = FieldValue.array(Arrays.asList(scalar("fruit"), scalar("apple"), scalar("sweetness"), scalar("7")));

        Map<String, FieldValue> map = pairs.asMap();
        assertThat(map).containsExactly(entry("fruit", scalar("apple")), entry("sweetness", scalar("7")));
    }

    @Test
    void asMapShouldRejectArraysThatAreNotPairs() {
        FieldValue oddLength = FieldValue.array(Arrays.asList(scalar("a"), scalar("b"), scalar("c")));
        assertThatIllegalStateException().isThrownBy(oddLength::asMap).withMessageContaining("key/value pairs");

        FieldValue complexKey = FieldValue.array(Arrays.asList(FieldValue.array(Collections.emptyList()), scalar("value")));
        assertThatIllegalStateException().isThrownBy(complexKey::asMap).withMessageContaining("key/value pairs");
    }

    @Test
    void asMapOnEmptyArrayShouldReturnEmptyMap() {
        assertThat(FieldValue.array(Collections.emptyList()).asMap()).isEmpty();
    }

    @Test
    void factoriesShouldRejectNullInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> FieldValue.of(null));
        assertThatIllegalArgumentException().isThrownBy(() -> FieldValue.array(null));
        assertThatIllegalArgumentException().isThrownBy(() -> FieldValue.array(Collections.singletonList(null)));
        assertThatIllegalArgumentException().isThrownBy(() -> FieldValue.map(null));
        assertThatIllegalArgumentException().isThrownBy(() -> FieldValue.map(Collections.singletonMap("k", null)));
    }

    @Test
    void complexValuesShouldBeImmutable() {
        List<FieldValue> elements = new ArrayList<>(Collections.singletonList(scalar("a")));
        FieldValue array = FieldValue.array(elements);
        elements.add(scalar("b"));
        assertThat(array.asList()).hasSize(1);
        assertThatThrownBy(() -> array.asList().add(scalar("c"))).isInstanceOf(UnsupportedOperationException.class);

        Map<String, FieldValue> entries = new LinkedHashMap<>(Collections.singletonMap("k", scalar("v")));
        FieldValue map = FieldValue.map(entries);
        entries.put("k2", scalar("v2"));
        assertThat(map.asMap()).hasSize(1);
        assertThatThrownBy(() -> map.asMap().put("k3", scalar("v3"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapShouldPreserveEntryOrder() {
        Map<String, FieldValue> entries = new LinkedHashMap<>();
        entries.put("z", scalar("1"));
        entries.put("a", scalar("2"));
        entries.put("m", scalar("3"));

        assertThat(FieldValue.map(entries).asMap().keySet()).containsExactly("z", "a", "m");
    }

    @Test
    void shouldImplementStructuralEquality() {
        assertThat(scalar("a")).isEqualTo(scalar("a")).hasSameHashCodeAs(scalar("a"));
        assertThat(scalar("a")).isNotEqualTo(scalar("b"));
        assertThat(FieldValue.nullValue()).isEqualTo(FieldValue.nullValue());
        assertThat(scalar("a")).isNotEqualTo(FieldValue.array(Collections.singletonList(scalar("a"))));

        FieldValue nested1 = FieldValue
                .array(Arrays.asList(FieldValue.map(Collections.singletonMap("k", scalar("v"))), FieldValue.nullValue()));
        FieldValue nested2 = FieldValue
                .array(Arrays.asList(FieldValue.map(Collections.singletonMap("k", scalar("v"))), FieldValue.nullValue()));
        assertThat(nested1).isEqualTo(nested2).hasSameHashCodeAs(nested2);
    }

    @Test
    void toStringShouldBeReadable() {
        assertThat(FieldValue.nullValue().toString()).isEqualTo("null");
        assertThat(scalar("abc").toString()).isEqualTo("abc");
        assertThat(FieldValue.array(Arrays.asList(scalar("a"), scalar("b"))).toString()).isEqualTo("[a, b]");
        assertThat(FieldValue.map(Collections.singletonMap("k", scalar("v"))).toString()).isEqualTo("{k=v}");
    }

}
