package com.lambdaworks.redis;


import java.util.List;
import java.util.stream.Collectors;

import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.RandomKeys;
import com.lambdaworks.redis.GeoArgs.Unit;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

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
        subscriber.await();

        assertThat(getValues()).containsAll(RandomKeys.KEYS);
    }

    public  List<String> getValues() {
        return (List) ReflectionTestUtils.getField(subscriber, "values");
    }

    public  <T> List<T> getValues(TestSubscriber<T> subscriber) {
        return (List) ReflectionTestUtils.getField(subscriber, "values");
    }

    @Test
    public void valueListCommandShouldReturnAllElements() throws Exception {

        redis.mset(RandomKeys.MAP);

        TestSubscriber<KeyValue<String, String>> subscriber = TestSubscriber.create();

        reactive.mget(RandomKeys.KEYS.toArray(new String[RandomKeys.COUNT])).subscribe(subscriber);
        subscriber.await();

        assertThat(getValues(subscriber).stream().map(KeyValue::getValue).collect(Collectors.toList()))
                .containsAll(RandomKeys.VALUES);
        assertThat(getValues(subscriber).stream().map(KeyValue::getKey).collect(Collectors.toList()))
                .containsAll(RandomKeys.KEYS);
    }

    @Test
    public void stringListCommandShouldReturnAllElements() throws Exception {

        reactive.configGet("*").subscribe(subscriber);
        subscriber.await();

        assertThat(getValues().size()).isGreaterThan(120);
    }

    @Test
    public void booleanListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<Boolean> subscriber = TestSubscriber.create();

        reactive.scriptExists("a", "b", "c").subscribe(subscriber);
        subscriber.awaitAndAssertNextValueCount(3);
    }

    @Test
    public void scoredValueListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<List<ScoredValue<String>>> subscriber = TestSubscriber.create();

        redis.zadd(key, 1d, "v1", 2d, "v2", 3d, "v3");

        reactive.zrangeWithScores(key, 0, -1).collectList().subscribe(subscriber);
        subscriber.awaitAndAssertNextValuesWith(scoredValues -> {
            assertThat(scoredValues).hasSize(3).contains(sv(1, "v1"), sv(2, "v2"), sv(3, "v3"));
        });
    }

    @Test
    public void geoWithinListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<GeoWithin<String>> subscriber = TestSubscriber.create();

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        reactive.georadius(key, 50, 20, 1000, Unit.km, new GeoArgs().withHash()).subscribe(subscriber);
        subscriber.await();

        assertThat(getValues(subscriber)).hasSize(2).contains(
                new GeoWithin<String>("value1", null, 3542523898362974L, null),
                new GeoWithin<>("value2", null, 3542609801095198L, null));
    }

    @Test
    public void geoCoordinatesListCommandShouldReturnAllElements() throws Exception {

        TestSubscriber<Value<GeoCoordinates>> subscriber = TestSubscriber.create();

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        reactive.geopos(key, "value1", "value2").subscribe(subscriber);
        subscriber.awaitAndAssertNextValueCount(2);
    }

}
