package com.lambdaworks.redis.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;

public class SentinelConnectionTest extends AbstractSentinelTest {

    private StatefulRedisSentinelConnection<String, String> connection;
    private RedisSentinelAsyncCommands<String, String> sentinelAsync;

    @BeforeClass
    public static void setupClient() {
        sentinelClient = RedisClient.create(RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        connection = sentinelClient.connectSentinel();
        sentinel = connection.sync();
        sentinelAsync = connection.async();
    }

    @Test
    public void testAsync() throws Exception {

        RedisFuture<List<Map<String, String>>> future = sentinelAsync.masters();

        assertThat(future.get()).isNotNull();
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCancelled()).isFalse();

    }

    @Test
    public void testFuture() throws Exception {

        RedisFuture<Map<String, String>> future = sentinelAsync.master("unknown master");

        AtomicBoolean state = new AtomicBoolean();

        future.exceptionally(throwable -> {
            state.set(true);
            return null;
        });

        assertThat(future.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(state.get()).isTrue();
    }

    @Test
    public void testStatefulConnection() throws Exception {

        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        assertThat(statefulConnection).isSameAs(statefulConnection.async().getStatefulConnection());

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

        Wait.untilTrue(() -> !sentinel.isOpen()).waitOrTimeout();

        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    public void testAsyncClose() throws Exception {
        StatefulRedisSentinelConnection<String, String> statefulConnection = sentinel.getStatefulConnection();
        statefulConnection.async().close();

        Wait.untilTrue(() -> !sentinel.isOpen()).waitOrTimeout();

        assertThat(sentinel.isOpen()).isFalse();
        assertThat(statefulConnection.isOpen()).isFalse();
    }

    @Test
    public void connectToOneNode() throws Exception {
        RedisSentinelCommands<String, String> connection = sentinelClient
                .connectSentinel(RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build()).sync();
        assertThat(connection.ping()).isEqualTo("PONG");
        connection.close();
    }
    @Test
    public void connectWithByteCodec() throws Exception {
        RedisSentinelCommands<byte[], byte[]> connection = sentinelClient.connectSentinel(new ByteArrayCodec()).sync();
        assertThat(connection.master(MASTER_ID.getBytes())).isNotNull();
        connection.close();
    }
}
