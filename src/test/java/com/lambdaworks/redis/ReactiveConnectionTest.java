package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import rx.Observable;

import com.lambdaworks.Delay;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;

public class ReactiveConnectionTest extends AbstractRedisClientTest {
    private RedisReactiveCommands<String, String> reactive;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void openReactiveConnection() throws Exception {
        reactive = client.connectAsync().getStatefulConnection().reactive();
    }

    @After
    public void closeReactiveConnection() throws Exception {
        reactive.close();
    }

    @Test
    public void doNotFireCommandUntilObservation() throws Exception {
        Observable<String> set = reactive.set(key, value);
        Delay.delay(seconds(2));
        assertThat(redis.get(key)).isNull();
        set.subscribe();
        Delay.delay(seconds(2));

        assertThat(redis.get(key)).isEqualTo(value);

    }

    @Test
    public void fireCommandAfterObserve() throws Exception {
        assertThat(reactive.set(key, value).toBlocking().first()).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
    }
}
