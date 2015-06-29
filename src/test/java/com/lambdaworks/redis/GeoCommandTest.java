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
        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302, "Bahn");
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

        List<GeoTuple> geopos = redis.geopos(key, "Weinheim", "foobar", "Bahn");

        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).getX().doubleValue()).isEqualTo(8.6638, offset(0.001));
        assertThat(geopos.get(1)).isNull();
        assertThat(geopos.get(2)).isNotNull();
    }

    @Test
    public void georadiusWithArgs() throws Exception {

        prepareGeo();

        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().withCount(1).asc();

        List<Object> result = redis.georadius(key, 8.6582861, 49.5285695, 1, GeoArgs.Unit.km, geoArgs);
        assertThat(result).hasSize(1);

        List<Object> response = (List) result.get(0);
        assertThat(response).hasSize(4);

        result = redis.georadius(key, 8.6582861, 49.5285695, 1, GeoArgs.Unit.km, null);
        assertThat(result).hasSize(1);
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

        List<Object> empty = redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, geoArgs);
        assertThat(empty).isNotEmpty();

        List<Object> georadiusbymember = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km, geoArgs);
        assertThat(georadiusbymember).hasSize(2);

        List<Object> response = (List) georadiusbymember.get(0);
        assertThat(response).hasSize(4);
    }

    @Test
    public void geoencode() throws Exception {

        List<Object> geoencode = redis.geoencode(8.6638775, 49.5282537);

        assertThat(geoencode).hasSize(5);
        assertThat(geoencode.get(0)).isEqualTo(3666615932941099L);
        assertThat(geoencode.get(1)).isInstanceOf(GeoTuple.class);
        assertThat(geoencode.get(2)).isInstanceOf(GeoTuple.class);
        assertThat(geoencode.get(3)).isInstanceOf(GeoTuple.class);

    }

    @Test
    public void geoencodeWithDistance() throws Exception {

        List<Object> result = redis.geoencode(8.6638775, 49.5282537, 1, GeoArgs.Unit.km);

        assertThat(result).hasSize(5);
        assertThat(result.get(0)).isEqualTo(3666615929405440L);
        assertThat(result.get(1)).isInstanceOf(GeoTuple.class);
        assertThat(result.get(2)).isInstanceOf(GeoTuple.class);
        assertThat(result.get(3)).isInstanceOf(GeoTuple.class);
    }

    @Test
    public void geodecode() throws Exception {

        List<GeoTuple> result = redis.geodecode(3666615932941099L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getX().doubleValue()).isEqualTo(8.6638730764389038, offset(1d));
        assertThat(result.get(0).getY().doubleValue()).isEqualTo(49.528251210511513, offset(1d));

        assertThat(result.get(1).getX().doubleValue()).isEqualTo(8.6638784408569336, offset(1d));
        assertThat(result.get(1).getY().doubleValue()).isEqualTo(49.528253745232675, offset(1d));

        assertThat(result.get(2).getX().doubleValue()).isEqualTo(8.6638757586479187, offset(1d));
        assertThat(result.get(2).getY().doubleValue()).isEqualTo(49.528252477872094, offset(1d));

    }
}
