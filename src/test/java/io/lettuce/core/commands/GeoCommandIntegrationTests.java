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
package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("GEOADD")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GeoCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected GeoCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void geoadd() {

        Long result = redis.geoadd(key, -73.9454966, 40.747533, "lic market");
        assertThat(result).isEqualTo(1);

        Long readd = redis.geoadd(key, -73.9454966, 40.747533, "lic market");
        assertThat(readd).isEqualTo(0);
    }

    @Test
    public void geoaddInTransaction() {

        redis.multi();
        redis.geoadd(key, -73.9454966, 40.747533, "lic market");
        redis.geoadd(key, -73.9454966, 40.747533, "lic market");

        assertThat(redis.exec()).containsSequence(1L, 0L);
    }

    @Test
    void geoaddMulti() {

        Long result = redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302,
                "Bahn");
        assertThat(result).isEqualTo(3);
    }

    @Test
    public void geoaddMultiInTransaction() {

        redis.multi();
        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302, "Bahn");

        assertThat(redis.exec()).contains(3L);
    }

    @Test
    void geoaddMultiWrongArgument() {
        assertThatThrownBy(() -> redis.geoadd(key, 49.528253)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void georadius() {

        prepareGeo();

        Set<String> georadius = redis.georadius(key, 8.6582861, 49.5285695, 1, GeoArgs.Unit.km);
        assertThat(georadius).hasSize(1).contains("Weinheim");

        Set<String> largerGeoradius = redis.georadius(key, 8.6582861, 49.5285695, 5, GeoArgs.Unit.km);
        assertThat(largerGeoradius).hasSize(2).contains("Weinheim").contains("Bahn");
    }

    @Test
    public void georadiusInTransaction() {

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
    void georadiusWithCoords() {

        prepareGeo();

        List<GeoWithin<String>> georadius = redis.georadius(key, 8.6582861, 49.5285695, 100, GeoArgs.Unit.km,
                GeoArgs.Builder.coordinates());

        assertThat(georadius).hasSize(3);
        assertThat(getX(georadius, 0)).isBetween(8.66, 8.67);
        assertThat(getY(georadius, 0)).isBetween(49.52, 49.53);

        assertThat(getX(georadius, 2)).isBetween(8.37, 8.38);
        assertThat(getY(georadius, 2)).isBetween(48.99, 49.00);
    }

    @Test
    void geodist() {

        prepareGeo();

        Double result = redis.geodist(key, "Weinheim", "Bahn", GeoArgs.Unit.km);
        // 10 mins with the bike
        assertThat(result).isGreaterThan(2.5).isLessThan(2.9);
    }

    @Test
    void geodistMissingElements() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("3.4"));
        prepareGeo();

        assertThat(redis.geodist("Unknown", "Unknown", "Bahn", GeoArgs.Unit.km)).isNull();
        assertThat(redis.geodist(key, "Unknown", "Bahn", GeoArgs.Unit.km)).isNull();
        assertThat(redis.geodist(key, "Weinheim", "Unknown", GeoArgs.Unit.km)).isNull();
    }

    @Test
    public void geodistInTransaction() {

        prepareGeo();

        redis.multi();
        redis.geodist(key, "Weinheim", "Bahn", GeoArgs.Unit.km);
        Double result = (Double) redis.exec().get(0);

        // 10 mins with the bike
        assertThat(result).isGreaterThan(2.5).isLessThan(2.9);
    }

    @Test
    public void geopos() {

        prepareGeo();

        List<GeoCoordinates> geopos = redis.geopos(key, "Weinheim");

        assertThat(geopos).hasSize(1);
        assertThat(geopos.get(0).getX().doubleValue()).isEqualTo(8.6638, offset(0.001));

        geopos = redis.geopos(key, "Weinheim", "foobar", "Bahn");

        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).getX().doubleValue()).isEqualTo(8.6638, offset(0.001));
        assertThat(geopos.get(1)).isNull();
        assertThat(geopos.get(2)).isNotNull();
    }

    @Test
    public void geoposInTransaction() {

        prepareGeo();

        redis.multi();
        redis.geopos(key, "Weinheim", "foobar", "Bahn");
        redis.geopos(key, "Weinheim", "foobar", "Bahn");
        List<GeoCoordinates> geopos = redis.exec().get(1);

        assertThat(geopos).hasSize(3);
        assertThat(geopos.get(0).getX().doubleValue()).isEqualTo(8.6638, offset(0.001));
        assertThat(geopos.get(1)).isNull();
        assertThat(geopos.get(2)).isNotNull();
    }

    @Test
    void georadiusWithArgs() {

        prepareGeo();

        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().withCount(1).desc();

        List<GeoWithin<String>> result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, geoArgs);
        assertThat(result).hasSize(1);

        GeoWithin<String> weinheim = result.get(0);

        assertThat(weinheim.getMember()).isEqualTo("Weinheim");
        assertThat(weinheim.getGeohash()).isEqualTo(3666615932941099L);

        assertThat(weinheim.getDistance()).isEqualTo(2.7882, offset(0.5));
        assertThat(weinheim.getCoordinates().getX().doubleValue()).isEqualTo(8.663875, offset(0.5));
        assertThat(weinheim.getCoordinates().getY().doubleValue()).isEqualTo(49.52825, offset(0.5));

        result = redis.georadius(key, 8.665351, 49.553302, 1, GeoArgs.Unit.km, new GeoArgs());
        assertThat(result).hasSize(1);

        GeoWithin<String> bahn = result.get(0);

        assertThat(bahn.getMember()).isEqualTo("Bahn");
        assertThat(bahn.getGeohash()).isNull();

        assertThat(bahn.getDistance()).isNull();
        assertThat(bahn.getCoordinates()).isNull();
    }

    @Test
    public void georadiusWithArgsAndTransaction() {

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

        assertThat(weinheim.getMember()).isEqualTo("Weinheim");
        assertThat(weinheim.getGeohash()).isEqualTo(3666615932941099L);

        assertThat(weinheim.getDistance()).isEqualTo(2.7882, offset(0.5));
        assertThat(weinheim.getCoordinates().getX().doubleValue()).isEqualTo(8.663875, offset(0.5));
        assertThat(weinheim.getCoordinates().getY().doubleValue()).isEqualTo(49.52825, offset(0.5));

        result = redis.georadius(key, 8.665351, 49.553302, 1, GeoArgs.Unit.km, new GeoArgs());
        assertThat(result).hasSize(1);

        GeoWithin<String> bahn = result.get(0);

        assertThat(bahn.getMember()).isEqualTo("Bahn");
        assertThat(bahn.getGeohash()).isNull();

        assertThat(bahn.getDistance()).isNull();
        assertThat(bahn.getCoordinates()).isNull();
    }

    @Test
    void geohash() {

        prepareGeo();

        List<Value<String>> geohash = redis.geohash(key, "Weinheim", "Bahn", "dunno");

        assertThat(geohash).containsSequence(Value.just("u0y1v0kffz0"), Value.just("u0y1vhvuvm0"), Value.empty());
    }

    @Test
    void geohashUnknownKey() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("3.4"));

        prepareGeo();

        List<Value<String>> geohash = redis.geohash("dunno", "member");

        assertThat(geohash).hasSize(1);
        assertThat(geohash.get(0)).isIn(null, Value.empty());
    }

    @Test
    public void geohashInTransaction() {

        prepareGeo();

        redis.multi();
        redis.geohash(key, "Weinheim", "Bahn", "dunno");
        redis.geohash(key, "Weinheim", "Bahn", "dunno");
        TransactionResult exec = redis.exec();

        List<Value<String>> geohash = exec.get(1);

        assertThat(geohash).containsSequence(Value.just("u0y1v0kffz0"), Value.just("u0y1vhvuvm0"), Value.empty());
    }

    @Test
    void georadiusStore() {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withStore(resultKey));
        assertThat(result).isEqualTo(2);

        List<ScoredValue<String>> results = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(results).hasSize(2);
    }

    @Test
    void georadiusStoreWithCountAndSort() {

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
    void georadiusStoreDist() {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withStoreDist("38o54"));
        assertThat(result).isEqualTo(2);

        List<ScoredValue<String>> dist = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(2);
    }

    @Test
    void georadiusStoreDistWithCountAndSort() {

        prepareGeo();

        String resultKey = "38o54"; // yields in same slot as "key"
        Long result = redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km,
                new GeoRadiusStoreArgs<>().withCount(1).desc().withStoreDist("38o54"));
        assertThat(result).isEqualTo(1);

        List<ScoredValue<String>> dist = redis.zrangeWithScores(resultKey, 0, -1);
        assertThat(dist).hasSize(1);

        assertThat(dist.get(0).getScore()).isBetween(2d, 3d);
    }

    @Test
    void georadiusWithNullArgs() {
        assertThatThrownBy(() -> redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, (GeoArgs) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void georadiusStoreWithNullArgs() {
        assertThatThrownBy(
                () -> redis.georadius(key, 8.665351, 49.553302, 5, GeoArgs.Unit.km, (GeoRadiusStoreArgs<String>) null))
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void georadiusbymember() {

        prepareGeo();

        Set<String> empty = redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km);
        assertThat(empty).hasSize(1).contains("Bahn");

        Set<String> georadiusbymember = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km);
        assertThat(georadiusbymember).hasSize(2).contains("Bahn", "Weinheim");
    }

    @Test
    void georadiusbymemberStoreDistWithCountAndSort() {

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
    void georadiusbymemberWithArgs() {

        prepareGeo();

        List<GeoWithin<String>> empty = redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km,
                new GeoArgs().withHash().withCoordinates().withDistance().desc());
        assertThat(empty).isNotEmpty();

        List<GeoWithin<String>> withDistanceAndCoordinates = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoArgs().withCoordinates().withDistance().desc());
        assertThat(withDistanceAndCoordinates).hasSize(2);

        GeoWithin<String> weinheim = withDistanceAndCoordinates.get(0);
        assertThat(weinheim.getMember()).isEqualTo("Weinheim");
        assertThat(weinheim.getGeohash()).isNull();
        assertThat(weinheim.getDistance()).isNotNull();
        assertThat(weinheim.getCoordinates()).isNotNull();

        List<GeoWithin<String>> withDistanceAndHash = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoArgs().withDistance().withHash().desc());
        assertThat(withDistanceAndHash).hasSize(2);

        GeoWithin<String> weinheimDistanceHash = withDistanceAndHash.get(0);
        assertThat(weinheimDistanceHash.getMember()).isEqualTo("Weinheim");
        assertThat(weinheimDistanceHash.getGeohash()).isNotNull();
        assertThat(weinheimDistanceHash.getDistance()).isNotNull();
        assertThat(weinheimDistanceHash.getCoordinates()).isNull();

        List<GeoWithin<String>> withCoordinates = redis.georadiusbymember(key, "Bahn", 5, GeoArgs.Unit.km,
                new GeoArgs().withCoordinates().desc());
        assertThat(withCoordinates).hasSize(2);

        GeoWithin<String> weinheimCoordinates = withCoordinates.get(0);
        assertThat(weinheimCoordinates.getMember()).isEqualTo("Weinheim");
        assertThat(weinheimCoordinates.getGeohash()).isNull();
        assertThat(weinheimCoordinates.getDistance()).isNull();
        assertThat(weinheimCoordinates.getCoordinates()).isNotNull();
    }

    @Test
    public void georadiusbymemberWithArgsInTransaction() {

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
        assertThat(weinheim.getMember()).isEqualTo("Weinheim");
        assertThat(weinheim.getGeohash()).isNull();
        assertThat(weinheim.getDistance()).isNotNull();
        assertThat(weinheim.getCoordinates()).isNotNull();

        List<GeoWithin<String>> withDistanceAndHash = exec.get(2);
        assertThat(withDistanceAndHash).hasSize(2);

        GeoWithin<String> weinheimDistanceHash = withDistanceAndHash.get(0);
        assertThat(weinheimDistanceHash.getMember()).isEqualTo("Weinheim");
        assertThat(weinheimDistanceHash.getGeohash()).isNotNull();
        assertThat(weinheimDistanceHash.getDistance()).isNotNull();
        assertThat(weinheimDistanceHash.getCoordinates()).isNull();

        List<GeoWithin<String>> withCoordinates = exec.get(3);
        assertThat(withCoordinates).hasSize(2);

        GeoWithin<String> weinheimCoordinates = withCoordinates.get(0);
        assertThat(weinheimCoordinates.getMember()).isEqualTo("Weinheim");
        assertThat(weinheimCoordinates.getGeohash()).isNull();
        assertThat(weinheimCoordinates.getDistance()).isNull();
        assertThat(weinheimCoordinates.getCoordinates()).isNotNull();
    }

    @Test
    void georadiusbymemberWithNullArgs() {
        assertThatThrownBy(() -> redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, (GeoArgs) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void georadiusStorebymemberWithNullArgs() {
        assertThatThrownBy(() -> redis.georadiusbymember(key, "Bahn", 1, GeoArgs.Unit.km, (GeoRadiusStoreArgs<String>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    protected void prepareGeo() {
        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim");
        redis.geoadd(key, 8.3796281, 48.9978127, "EFS9", 8.665351, 49.553302, "Bahn");
    }

    private static double getY(List<GeoWithin<String>> georadius, int i) {
        return georadius.get(i).getCoordinates().getY().doubleValue();
    }

    private static double getX(List<GeoWithin<String>> georadius, int i) {
        return georadius.get(i).getCoordinates().getX().doubleValue();
    }

}
