package com.lambdaworks.redis.sentinel;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.code.tempusfugit.temporal.Duration;
import com.lambdaworks.Delay;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisSentinelCommands;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;

public class SentinelConnectionTest extends AbstractSentinelTest {

    @BeforeClass
    public static void setupClient() {
        sentinelClient = new RedisClient(RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();
    }

    @Test
    public void testAsync() throws Exception {

        RedisFuture<List<Map<String, String>>> future = sentinel.masters();

        assertThat(future.get()).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCancelled()).isFalse();

    }

    @Test
    public void testFuture() throws Exception {

        RedisFuture<Map<String, String>> future = sentinel.master("unknown master");

        AtomicBoolean state = new AtomicBoolean();

        future.exceptionally(throwable -> {
            state.set(true);
            return null;
        });
        future.await(5, TimeUnit.SECONDS);

        assertThat(state.get()).isTrue();
    }

    @Test
    public void testStatefulConnection() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(sentinel).isSameAs(statefulConnection.async());

    }

    @Test
    public void testSyncConnection() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        RedisSentinelCommands<String, String> sync = statefulConnection.sync();
        assertThat(sync.ping()).isEqualTo("PONG");

    }

    @Test
    public void testSyncAsyncConversion() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(statefulConnection.sync().getStatefulConnection()).isSameAs(statefulConnection);
        assertThat(statefulConnection.sync().getStatefulConnection().sync()).isSameAs(statefulConnection.sync());

    }

    @Test
    public void testSyncClose() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.sync().close();
        Delay.delay(seconds(1));
        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    public void testAsyncClose() throws Exception {
        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.async().close();
        Delay.delay(seconds(1));
        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();

    }
}
