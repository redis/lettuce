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

import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.lambdaworks.Delay;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        reactive.getStatefulConnection().close();
    }

    @Test
    public void doNotFireCommandUntilObservation() throws Exception {
        Mono<String> set = reactive.set(key, value);
        Delay.delay(millis(200));
        assertThat(redis.get(key)).isNull();
        set.subscribe();
        Wait.untilEquals(value, () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void fireCommandAfterObserve() throws Exception {
        assertThat(reactive.set(key, value).block()).isEqualTo("OK");
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
        assertThat(result).isEmpty();
    }

    @Test
    public void testEcho() throws Exception {
        String result = reactive.echo("echo").block();
        assertThat(result).isEqualTo("echo");
    }

    @Test
    public void testMonoMultiCancel() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(1000).subscribe();
        Delay.delay(millis(100));

        Mono<String> set = reactive.set(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).isEmpty();
    }

    @Test
    public void testFluxCancel() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(1000).subscribe();
        Delay.delay(millis(100));

        Flux<KeyValue<String, String>> set = reactive.mget(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).isEmpty();
    }

    @Test
    public void multiSubscribe() throws Exception {
        reactive.set(key, "1").subscribe();
        Mono<Long> incr = reactive.incr(key);
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

        reactive.mset(map).block();

        List<String> values = reactive.keys("*").flatMap(s -> reactive.get(s)).collectList().subscribeOn(Schedulers.immediate())
                .block();

        assertThat(values).hasSize(2).contains(value, "value1");
    }

    @Test
    public void auth() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        reactive.auth("error").doOnError(errors::add).subscribe(TestSubscriber.create());
        Delay.delay(millis(50));
        assertThat(errors).hasSize(1);
    }

    @Test
    public void subscriberCompletingWithExceptionShouldBeHandledSafely() throws Exception {

        Flux.concat(reactive.set("keyA", "valueA"), reactive.set("keyB", "valueB")).collectList().block();

        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());
        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());

        String valueB = reactive.get("keyB").block();
        assertThat(valueB).isEqualTo("valueB");
    }

    private static Subscriber<String> createSubscriberWithExceptionOnComplete() {
        return new Subscriber<String>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1000);
            }

            @Override
            public void onComplete() {
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

    private static class CompletionSubscriber implements Subscriber<Object> {

        private final List<Object> result;

        public CompletionSubscriber(List<Object> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(1000);
        }

        @Override
        public void onComplete() {
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
