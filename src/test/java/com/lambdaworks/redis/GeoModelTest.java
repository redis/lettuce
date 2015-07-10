package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 06.07.15 14:18
 */
public class GeoModelTest {

    @Test
    public void geoWithin() throws Exception {

        GeoWithin<String> sut = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1, 2));
        GeoWithin<String> equalsToSut = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1, 2));

        Map<GeoWithin<String>, String> map = ImmutableMap.of(sut, "value");

        assertThat(map.get(equalsToSut)).isEqualTo("value");
        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
        assertThat(sut.toString()).isEqualTo(equalsToSut.toString());

    }

    @Test
    public void geoWithinSlightlyDifferent() throws Exception {

        GeoWithin<String> sut = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1, 2));
        GeoWithin<String> slightlyDifferent = new GeoWithin<String>("me", 1.0, 1234L, new GeoCoordinates(1.1, 2));

        Map<GeoWithin<String>, String> map = ImmutableMap.of(sut, "value");

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

        Map<GeoCoordinates, String> map = ImmutableMap.of(sut, "value");

        assertThat(map.get(equalsToSut)).isEqualTo("value");
        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
        assertThat(sut.toString()).isEqualTo(equalsToSut.toString());

    }

    @Test
    public void geoCoordinatesSlightlyDifferent() throws Exception {

        GeoCoordinates sut = new GeoCoordinates(1, 2);
        GeoCoordinates slightlyDifferent = new GeoCoordinates(1.1, 2);

        Map<GeoCoordinates, String> map = ImmutableMap.of(sut, "value");

        assertThat(map.get(slightlyDifferent)).isNull();
        assertThat(sut).isNotEqualTo(slightlyDifferent);
        assertThat(sut.hashCode()).isNotEqualTo(slightlyDifferent.hashCode());
        assertThat(sut.toString()).isNotEqualTo(slightlyDifferent.toString());

    }

    @Test
    public void geoCoordinatesEmpty() throws Exception {

        GeoCoordinates sut = new GeoCoordinates(null, null);
        GeoCoordinates equalsToSut = new GeoCoordinates(null, null);

        assertThat(sut).isEqualTo(equalsToSut);
        assertThat(sut.hashCode()).isEqualTo(equalsToSut.hashCode());
    }

}
