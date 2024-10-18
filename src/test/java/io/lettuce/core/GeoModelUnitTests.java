package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
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
