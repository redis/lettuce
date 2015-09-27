package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class GeoCommandTest extends AbstractCommandTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void geoadd() throws Exception {

        Long result = redis.geoadd(key, -73.9454966, 40.747533, "lic market");
        assertThat(result).isEqualTo(1);

        Long readd = redis.geoadd(key, -73.9454966, 40.747533, "lic market");
        assertThat(readd).isEqualTo(0);
    }

    @Test
    public void geoaddMulti() throws Exception {

        Long result = redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302,
                "Bahn");
        assertThat(result).isEqualTo(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void geoaddMultiWrongArgument() throws Exception {
        redis.geoadd(key, 49.528253);
    }

    protected void prepareGeo() {
        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim");
        redis.geoadd(key, 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302, "Bahn");
    }

    @Test
    public void georadius() throws Exception {

        prepareGeo();

        Set<String> georadius = redis.georadius(key, 8.6582861, 49.5285695, 1, GeoArgs.Unit.km);
        assertThat(georadius).hasSize(1).contains("Weinheim");

        Set<String> largerGeoradius = redis.georadius(key, 8.6582861, 49.5285695, 5, GeoArgs.Unit.km);
        assertThat(largerGeoradius).hasSize(2).contains("Weinheim").contains("Bahn");

    }

    @Test
    public void geodist() throws Exception {

        prepareGeo();

        Double result = redis.geodist(key, "Weinheim", "Bahn", GeoArgs.Unit.km);
        // 10 mins with the bike
        assertThat(result).isGreaterThan(2.5).isLessThan(2.9);

    }

    @Test
    public void geopos() throws Exception {

        prepareGeo();

        List<GeoCoordinates> geopos = redis.geopos(key, "Weinheim", "foobar", "Bahn");

        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).x.doubleValue()).isEqualTo(8.6638, offset(0.001));
        assertThat(geopos.get(1)).isNull();
        assertThat(geopos.get(2)).isNotNull();
    }

    @Test
    public void georadiusWithArgs() throws Exception {

        prepareGeo();

        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().withCount(1).desc();

        List<GeoWithin<String>> result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, geoArgs);
        assertThat(result).hasSize(1);

        GeoWithin<String> weinheim = result.get(0);

        assertThat(weinheim.member).isEqualTo("Weinheim");
        assertThat(weinheim.geohash).isEqualTo(3666615932941099L);

        assertThat(weinheim.distance).isEqualTo(2.7882, offset(0.5));
        assertThat(weinheim.coordinates.x.doubleValue()).isEqualTo(8.663875, offset(0.5));
        assertThat(weinheim.coordinates.y.doubleValue()).isEqualTo(49.52825, offset(0.5));

        result = redis.georadius(key, 8.665351, 49.553302, 1, GeoArgs.Unit.km, new GeoArgs());
        assertThat(result).hasSize(1);

        GeoWithin<String> bahn = result.get(0);

        assertThat(bahn.member).isEqualTo("Bahn");
        assertThat(bahn.geohash).isNull();

        assertThat(bahn.distance).isNull();
        assertThat(bahn.coordinates).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void georadiusWithNullArgs() throws Exception {
        redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, null);
    }

    @Test
    public void georadiusbymember() throws Exception {

        prepareGeo();

        Set<String> empty = redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km);
        assertThat(empty).hasSize(1).contains("Bahn");

        Set<String> georadiusbymember = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km);
        assertThat(georadiusbymember).hasSize(2).contains("Bahn", "Weinheim");
    }

    @Test
    public void georadiusbymemberWithArgs() throws Exception {

        prepareGeo();

        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().desc();

        List<GeoWithin<String>> empty = redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, geoArgs);
        assertThat(empty).isNotEmpty();

        List<GeoWithin<String>> georadiusbymember = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km, geoArgs);
        assertThat(georadiusbymember).hasSize(2);

        GeoWithin<String> weinheim = georadiusbymember.get(0);
        assertThat(weinheim.member).isEqualTo("Weinheim");
        assertThat(weinheim.distance).isNotNull();
        assertThat(weinheim.coordinates).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void georadiusbymemberWithNullArgs() throws Exception {
        redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, null);
    }

}
