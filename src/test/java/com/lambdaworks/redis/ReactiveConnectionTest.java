package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import rx.Observable;
import rx.Subscriber;

import com.google.common.collect.Lists;
import com.lambdaworks.Delay;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;

public class ReactiveConnectionTest extends AbstractRedisClientTest {
    private RedisReactiveCommands<String, String> reactive;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private StatefulRedisConnection<String, String> stateful;

    @Before
    public void openReactiveConnection() throws Exception {
        stateful = client.connectAsync().getStatefulConnection();
        reactive = stateful.reactive();
    }

    @After
    public void closeReactiveConnection() throws Exception {
        reactive.close();
    }

    @Test
    public void doNotFireCommandUntilObservation() throws Exception {
        Observable<String> set = reactive.set(key, value);
        Delay.delay(millis(200));
        assertThat(redis.get(key)).isNull();
        set.subscribe();
        Wait.untilEquals(value, () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void fireCommandAfterObserve() throws Exception {
        assertThat(reactive.set(key, value).toBlocking().first()).isEqualTo("OK");
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

        List<Object> result = Lists.newArrayList();
        reactive.clientPause(1000).subscribe();
        reactive.set(key, value).subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).hasSize(1).contains("completed");
    }

    @Test
    public void testEcho() throws Exception {
        String result = reactive.echo("echo").toBlocking().first();
        assertThat(result).isEqualTo("echo");
    }

    @Test
    public void testMultiCancel() throws Exception {

        List<Object> result = Lists.newArrayList();
        reactive.clientPause(1000).subscribe();

        Observable<String> set = reactive.set(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));

        Delay.delay(millis(100));
        reactive.reset();
        assertThat(result).hasSize(3).contains("completed");
    }

    @Test
    public void multiSubscribe() throws Exception {
        reactive.set(key, "1").subscribe();
        Observable<Long> incr = reactive.incr(key);
        incr.subscribe();
        incr.subscribe();
        incr.subscribe();

        Wait.untilEquals("4", () -> redis.get(key));

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
