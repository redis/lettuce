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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class GeoModelTest {

    @Test
    public void geoWithin() throws Exception {

        GeoWithin<String> sut = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1, 2));
        GeoWithin<String> equalsToSut = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1, 2));

        Map<GeoWithin<String>, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(equalsToSut)).isEqualTo("value");
        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
        assertThat(sut.toString()).isEqualTo(equalsToSut.toString());

    }

    @Test
    public void geoWithinSlightlyDifferent() throws Exception {

        GeoWithin<String> sut = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1, 2));
        GeoWithin<String> slightlyDifferent = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1.1, 2));

        Map<GeoWithin<String>, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(slightlyDifferent)).isNull();
        assertThat(sut).isNotEqualTo(slightlyDifferent);
        assertThat(sut.hashCode()).isNotEqualTo(slightlyDifferent.hashCode());
        assertThat(sut.toString()).isNotEqualTo(slightlyDifferent.toString());

        slightlyDifferent = new GeoWithin<String>("me1", 1.0, 1234L, new GeoCoordinates(1, 2));
        assertThat(sut).isNotEqualTo(slightlyDifferent);
    }

    @Test
    public void geoWithinEmpty() throws Exception {

        GeoWithin<String> sut = new GeoWithin<String>(null, null, null, null);
        GeoWithin<String> equalsToSut = new GeoWithin<String>(null, null, null, null);

        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
    }

    @Test
    public void geoCoordinates() throws Exception {

        GeoCoordinates sut = new GeoCoordinates(1, 2);
        GeoCoordinates equalsToSut = new GeoCoordinates(1, 2);

        Map<GeoCoordinates, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(equalsToSut)).isEqualTo("value");
        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
        assertThat(sut.toString()).isEqualTo(equalsToSut.toString());

    }

    @Test
    public void geoCoordinatesSlightlyDifferent() throws Exception {

        GeoCoordinates sut = new GeoCoordinates(1, 2);
        GeoCoordinates slightlyDifferent = new GeoCoordinates(1.1, 2);

        Map<GeoCoordinates, String> map = Collections.singletonMap(sut, "value");

        assertThat(map.get(slightlyDifferent)).isNull();
        assertThat(sut).isNotEqualTo(slightlyDifferent);
        assertThat(sut.hashCode()).isNotEqualTo(slightlyDifferent.hashCode());
        assertThat(sut.toString()).isNotEqualTo(slightlyDifferent.toString());

    }
}
