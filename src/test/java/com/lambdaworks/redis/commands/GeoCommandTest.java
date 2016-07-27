package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.*;

public class GeoCommandTest extends AbstractRedisClientTest {

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
    public void geoaddWithTransaction() throws Exception {

        redis.multi();
        redis.geoadd(key, -73.9454966, 40.747533, "lic market");
        redis.geoadd(key, -73.9454966, 40.747533, "lic market");

        assertThat(redis.exec()).containsSequence(1L, 0L);
    }

    @Test
    public void geoaddMulti() throws Exception {

        Long result = redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302,
                "Bahn");
        assertThat(result).isEqualTo(3);
    }

    @Test
    public void geoaddMultiWithTransaction() throws Exception {

        redis.multi();
        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302, "Bahn");

        assertThat(redis.exec()).contains(3L);
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
    public void georadiusWithTransaction() throws Exception {

        prepareGeo();

        redis.multi();
        redis.georadius(key, 8.6582861, 49.5285695, 1, GeoArgs.Unit.km);
        redis.georadius(key, 8.6582861, 49.5285695, 5, GeoArgs.Unit.km);

        TransactionResult exec = redis.exec();
        Set<String> georadius = exec.get(0);
        Set<String> largerGeoradius = exec.get(1);

        assertThat(georadius).hasSize(1).contains("Weinheim");
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
    public void geodistWithTransaction() throws Exception {

        prepareGeo();

        redis.multi();
        redis.geodist(key, "Weinheim", "Bahn", GeoArgs.Unit.km);
        Double result = (Double) redis.exec().get(0);

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
    public void geoposWithTransaction() throws Exception {

        prepareGeo();

        redis.multi();
        redis.geopos(key, "Weinheim", "foobar", "Bahn");
        redis.geopos(key, "Weinheim", "foobar", "Bahn");
        List<GeoCoordinates> geopos = (List) redis.exec().get(1);

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

    @Test
    public void georadiusWithArgsAndTransaction() throws Exception {

        prepareGeo();

        redis.multi();
        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().withCount(1).desc();
        redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, geoArgs);
        redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, geoArgs);
        TransactionResult exec = redis.exec();

        assertThat(exec).hasSize(2);

        List<GeoWithin<String>> result = exec.get(1);
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

    @Test
    public void geohash() throws Exception {

        prepareGeo();

        List<String> geohash = redis.geohash(key, "Weinheim", "Bahn", "dunno");

        assertThat(geohash).containsSequence("u0y1v0kffz0", "u0y1vhvuvm0", null);
    }

    @Test
    public void geohashUnknownKey() throws Exception {

        prepareGeo();

        List<String> geohash = redis.geohash("dunno", "member");

        assertThat(geohash).isEmpty();
    }

    @Test
    public void geohashWithTransaction() throws Exception {

        prepareGeo();

        redis.multi();
        redis.geohash(key, "Weinheim", "Bahn", "dunno");
        redis.geohash(key, "Weinheim", "Bahn", "dunno");
        TransactionResult exec = redis.exec();

        List<String> geohash = exec.get(1);

        assertThat(geohash).containsSequence("u0y1v0kffz0", "u0y1vhvuvm0", null);
    }

    @Test
    public void georadiusStore() throws Exception {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withStore(resultKey));
        assertThat(result).isEqualTo(2);

        List<ScoredValue<String>> results = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(results).hasSize(2);
    }

    @Test
    public void georadiusStoreWithCountAndSort() throws Exception {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withCount(1).desc().withStore(resultKey));
        assertThat(result).isEqualTo(1);

        List<ScoredValue<String>> results = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScore()).isGreaterThan(99999);
    }

    @Test
    public void georadiusStoreDist() throws Exception {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withStoreDist("38o54"));
        assertThat(result).isEqualTo(2);

        List<ScoredValue<String>> dist = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(2);
    }

    @Test
    public void georadiusStoreDistWithCountAndSort() throws Exception {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withCount(1).desc().withStoreDist("38o54"));
        assertThat(result).isEqualTo(1);

        List<ScoredValue<String>> dist = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(1);

        assertThat(dist.get(0).getScore()).isBetween(2d, 3d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void georadiusWithNullArgs() throws Exception {
        redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, (GeoArgs) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void georadiusStoreWithNullArgs() throws Exception {
        redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, (GeoRadiusStoreArgs<String>) null);
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
    public void georadiusbymemberStoreDistWithCountAndSort() throws Exception {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withCount(1).desc().withStoreDist("38o54"));
        assertThat(result).isEqualTo(1);

        List<ScoredValue<String>> dist = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(1);

        assertThat(dist.get(0).getScore()).isBetween(2d, 3d);
    }

    @Test
    public void georadiusbymemberWithArgs() throws Exception {

        prepareGeo();

        List<GeoWithin<String>> empty = redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km,
                new GeoArgs().withHash().withCoordinates().withDistance().desc());
        assertThat(empty).isNotEmpty();

        List<GeoWithin<String>> withDistanceAndCoordinates = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoArgs().withCoordinates().withDistance().desc());
        assertThat(withDistanceAndCoordinates).hasSize(2);

        GeoWithin<String> weinheim = withDistanceAndCoordinates.get(0);
        assertThat(weinheim.member).isEqualTo("Weinheim");
        assertThat(weinheim.geohash).isNull();
        assertThat(weinheim.distance).isNotNull();
        assertThat(weinheim.coordinates).isNotNull();

        List<GeoWithin<String>> withDistanceAndHash = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoArgs().withDistance().withHash().desc());
        assertThat(withDistanceAndHash).hasSize(2);

        GeoWithin<String> weinheimDistanceHash = withDistanceAndHash.get(0);
        assertThat(weinheimDistanceHash.member).isEqualTo("Weinheim");
        assertThat(weinheimDistanceHash.geohash).isNotNull();
        assertThat(weinheimDistanceHash.distance).isNotNull();
        assertThat(weinheimDistanceHash.coordinates).isNull();

        List<GeoWithin<String>> withCoordinates = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoArgs().withCoordinates().desc());
        assertThat(withCoordinates).hasSize(2);

        GeoWithin<String> weinheimCoordinates = withCoordinates.get(0);
        assertThat(weinheimCoordinates.member).isEqualTo("Weinheim");
        assertThat(weinheimCoordinates.geohash).isNull();
        assertThat(weinheimCoordinates.distance).isNull();
        assertThat(weinheimCoordinates.coordinates).isNotNull();
    }

    @Test
    public void georadiusbymemberWithArgsAndTransaction() throws Exception {

        prepareGeo();

        redis.multi();
        redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km,
                new GeoArgs().withHash().withCoordinates().withDistance().desc());
        redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km, new GeoArgs().withCoordinates().withDistance().desc());
        redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km, new GeoArgs().withDistance().withHash().desc());
        redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km, new GeoArgs().withCoordinates().desc());

        TransactionResult exec = redis.exec();

        List<GeoWithin<String>> empty = exec.get(0);
        assertThat(empty).isNotEmpty();

        List<GeoWithin<String>> withDistanceAndCoordinates = exec.get(1);
        assertThat(withDistanceAndCoordinates).hasSize(2);

        GeoWithin<String> weinheim = withDistanceAndCoordinates.get(0);
        assertThat(weinheim.member).isEqualTo("Weinheim");
        assertThat(weinheim.geohash).isNull();
        assertThat(weinheim.distance).isNotNull();
        assertThat(weinheim.coordinates).isNotNull();

        List<GeoWithin<String>> withDistanceAndHash = exec.get(2);
        assertThat(withDistanceAndHash).hasSize(2);

        GeoWithin<String> weinheimDistanceHash = withDistanceAndHash.get(0);
        assertThat(weinheimDistanceHash.member).isEqualTo("Weinheim");
        assertThat(weinheimDistanceHash.geohash).isNotNull();
        assertThat(weinheimDistanceHash.distance).isNotNull();
        assertThat(weinheimDistanceHash.coordinates).isNull();

        List<GeoWithin<String>> withCoordinates = exec.get(3);
        assertThat(withCoordinates).hasSize(2);

        GeoWithin<String> weinheimCoordinates = withCoordinates.get(0);
        assertThat(weinheimCoordinates.member).isEqualTo("Weinheim");
        assertThat(weinheimCoordinates.geohash).isNull();
        assertThat(weinheimCoordinates.distance).isNull();
        assertThat(weinheimCoordinates.coordinates).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void georadiusbymemberWithNullArgs() throws Exception {
        redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, (GeoArgs) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void georadiusStorebymemberWithNullArgs() throws Exception {
        redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, (GeoRadiusStoreArgs<String>) null);
    }

}
