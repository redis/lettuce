package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import rx.Observable;
import rx.Single;
import rx.Subscriber;

import com.lambdaworks.Delay;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import rx.observers.TestSubscriber;

import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class ReactiveConnectionTest extends AbstractRedisClientTest {

    private RedisReactiveCommands<String, String> reactive;

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
        reactive.close();
    }

    @Test
    public void doNotFireCommandUntilObservation() throws Exception {
        Single<String> set = reactive.set(key, value);
        Delay.delay(millis(200));
        assertThat(redis.get(key)).isNull();
        set.subscribe();
        Wait.untilEquals(value, () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void fireCommandAfterObserve() throws Exception {
        assertThat(reactive.set(key, value).toBlocking().value()).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void isOpen() throws Exception {
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void getStatefulConnection() throws Exception {
        assertThat(reactive.getStatefulConnection()).isSameAs(stateful);
    }

    @Test
    public void testCancelCommand() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(2000).subscribe(TestSubscriber.create());
        Delay.delay(millis(100));

        reactive.set(key, value).subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).hasSize(1).hasOnlyElementsOfType(CancellationException.class);
    }

    @Test
    public void testEcho() throws Exception {
        String result = reactive.echo("echo").toBlocking().value();
        assertThat(result).isEqualTo("echo");
    }

    @Test
    public void testSingleMultiCancel() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(1000).subscribe();
        Delay.delay(millis(100));

        Single<String> set = reactive.set(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).hasSize(3);
    }

    @Test
    public void testObservableMultiCancel() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(1000).subscribe();
        Delay.delay(millis(100));

        Observable<String> set = reactive.mget(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).hasSize(3);
    }

    @Test
    public void multiSubscribe() throws Exception {
        reactive.set(key, "1").subscribe();
        Single<Long> incr = reactive.incr(key);
        incr.subscribe();
        incr.subscribe();
        incr.subscribe();

        Wait.untilEquals("4", () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo("4");
    }

    @Test
    public void transactional() throws Exception {

        final CountDownLatch sync = new CountDownLatch(1);

        RedisReactiveCommands<String, String> reactive = client.connect().reactive();

        reactive.multi().subscribe(multiResponse -> {
            reactive.set(key, "1").subscribe();
            reactive.incr(key).subscribe(getResponse -> {
                sync.countDown();
            });
            reactive.exec().subscribe();
        });

        sync.await(5, TimeUnit.SECONDS);

        String result = redis.get(key);
        assertThat(result).isEqualTo("2");
    }

    @Test
    public void reactiveChain() throws Exception {

        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        map.put("key1", "value1");

        reactive.mset(map).toBlocking().value();

        List<String> values = reactive.keys("*").flatMap(s -> reactive.get(s).toObservable()).toList().subscribeOn(Schedulers.immediate())
                .toBlocking().first();

        assertThat(values).hasSize(2).contains(value, "value1");
    }

    @Test
    public void auth() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        reactive.auth("error").doOnError(errors::add).subscribe(new TestSubscriber<>());
        Delay.delay(millis(50));
        assertThat(errors).hasSize(1);
    }

    @Test
    public void subscriberCompletingWithExceptionShouldBeHandledSafely() throws Exception {

        Single.concat(reactive.set("keyA", "valueA"), reactive.set("keyB", "valueB")).toBlocking().last();

        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());
        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());

        String valueB = reactive.get("keyB").toBlocking().toFuture().get();
        assertThat(valueB).isEqualTo("valueB");
    }

    private static Subscriber<String> createSubscriberWithExceptionOnComplete() {
        return new Subscriber<String>() {
            @Override
            public void onCompleted() {
                throw new RuntimeException("throwing something");
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(String s) {
            }
        };
    }

    private static class CompletionSubscriber extends Subscriber<Object> {

        private final List<Object> result;

        public CompletionSubscriber(List<Object> result) {
            this.result = result;
        }

        @Override
        public void onCompleted() {
            result.add("completed");
        }

        @Override
        public void onError(Throwable e) {
            result.add(e);
        }

        @Override
        public void onNext(Object o) {
            result.add(o);
        }
    }
}
