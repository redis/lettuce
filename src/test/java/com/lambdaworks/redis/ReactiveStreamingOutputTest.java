package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import rx.observers.TestSubscriber;

import com.lambdaworks.RandomKeys;
import com.lambdaworks.redis.GeoArgs.Unit;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;

public class ReactiveStreamingOutputTest extends AbstractRedisClientTest {

    private RedisReactiveCommands<String, String> reactive;
    private TestSubscriber<String> subscriber = TestSubscriber.create();

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private StatefulRedisConnection<String, String> stateful;

    @Before
    public void openReactiveConnection() throws Exception {
        stateful = client.connect();
        reactive = stateful.reactive();
    }

    @After
    public void closeReactiveConnection() throws Exception {
        reactive.getStatefulConnection().close();
    }

    @Test
    public void keyListCommandShouldReturnAllElements() throws Exception {

        redis.mset(RandomKeys.MAP);

        reactive.keys("*").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).containsAll(RandomKeys.KEYS);
    }

    @Test
    public void valueListCommandShouldReturnAllElements() throws Exception {

        redis.mset(RandomKeys.MAP);

        reactive.mget(RandomKeys.KEYS.toArray(new String[RandomKeys.COUNT])).subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).containsAll(RandomKeys.VALUES);
    }

    @Test
    public void stringListCommandShouldReturnAllElements() throws Exception {

        reactive.configGet("*").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents().size()).isGreaterThan(120);
    }

    @Test
    public void booleanListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<Boolean> subscriber = TestSubscriber.create();

        reactive.scriptExists("a", "b", "c").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(3).doesNotContainNull();
    }

    @Test
    public void scoredValueListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<ScoredValue<String>> subscriber = TestSubscriber.create();

        redis.zadd(key, 1d, "v1", 2d, "v2", 3d, "v3");

        reactive.zrangeWithScores(key, 0, -1).subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(3).contains(sv(1, "v1"), sv(2, "v2"), sv(3, "v3"));
    }

    @Test
    public void geoWithinListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<GeoWithin<String>> subscriber = TestSubscriber.create();

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        reactive.georadius(key, 50, 20, 1000, Unit.km, new GeoArgs().withHash()).subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(2).contains(
                new GeoWithin<String>("value1", null, 3542523898362974L, null),
                new GeoWithin<>("value2", null, 3542609801095198L, null));
    }

    @Test
    public void geoCoordinatesListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<GeoCoordinates> subscriber = TestSubscriber.create();

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        reactive.geopos(key, "value1", "value2").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(2).doesNotContainNull();
    }

}
