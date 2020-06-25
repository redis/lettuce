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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class GeoModelUnitTests {

    @Test
    void geoWithin() {

        GeoWithin<String> sut = new GeoWithin<>("me", 1.0, 1234L, new GeoCoordinates(1, 2));
        GeoWithin<String> equalsToSut = new GeoWithin<>("me", 1.0, 1234L, new GeoCoordinates(1, 2));

        Map<GeoWithin<String>, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(equalsToSut)).isEqualTo("value");
        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
        assertThat(sut.toString()).isEqualTo(equalsToSut.toString());
    }

    @Test
    void geoWithinSlightlyDifferent() {

        GeoWithin<String> sut = new GeoWithin<>("me", 1.0, 1234L, new GeoCoordinates(1, 2));
        GeoWithin<String> slightlyDifferent = new GeoWithin<>("me", 1.0, 1234L, new GeoCoordinates(1.1, 2));

        Map<GeoWithin<String>, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(slightlyDifferent)).isNull();
        assertThat(sut).isNotEqualTo(slightlyDifferent);
        assertThat(sut.hashCode()).isNotEqualTo(slightlyDifferent.hashCode());
        assertThat(sut.toString()).isNotEqualTo(slightlyDifferent.toString());

        slightlyDifferent = new GeoWithin<>("me1", 1.0, 1234L, new GeoCoordinates(1, 2));
        assertThat(sut).isNotEqualTo(slightlyDifferent);
    }

    @Test
    void geoWithinEmpty() {

        GeoWithin<String> sut = new GeoWithin<>(null, null, null, null);
        GeoWithin<String> equalsToSut = new GeoWithin<>(null, null, null, null);

        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
    }

    @Test
    void geoCoordinates() {

        GeoCoordinates sut = new GeoCoordinates(1, 2);
        GeoCoordinates equalsToSut = new GeoCoordinates(1, 2);

        Map<GeoCoordinates, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(equalsToSut)).isEqualTo("value");
        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
        assertThat(sut.toString()).isEqualTo(equalsToSut.toString());

    }

    @Test
    void geoCoordinatesSlightlyDifferent() {

        GeoCoordinates sut = new GeoCoordinates(1, 2);
        GeoCoordinates slightlyDifferent = new GeoCoordinates(1.1, 2);

        Map<GeoCoordinates, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(slightlyDifferent)).isNull();
        assertThat(sut).isNotEqualTo(slightlyDifferent);
        assertThat(sut.hashCode()).isNotEqualTo(slightlyDifferent.hashCode());
        assertThat(sut.toString()).isNotEqualTo(slightlyDifferent.toString());

    }

}
