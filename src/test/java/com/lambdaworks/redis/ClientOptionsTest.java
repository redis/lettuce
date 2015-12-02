package com.lambdaworks.redis;

import static com.lambdaworks.Connections.getConnectionWatchdog;
import static com.lambdaworks.Connections.getStatefulConnection;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.StrictAssertions.assertThat;

import org.junit.Test;

import com.lambdaworks.Wait;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClientOptionsTest extends AbstractRedisClientTest {

    @Test
    public void testNew() throws Exception {
        checkAssertions(ClientOptions.create());
    }

    @Test
    public void testBuilder() throws Exception {
        checkAssertions(new ClientOptions.Builder().build());
    }

    @Test
    public void testCopy() throws Exception {
        checkAssertions(ClientOptions.copyOf(new ClientOptions.Builder().build()));
    }

    protected void checkAssertions(ClientOptions sut) {
        assertThat(sut.isAutoReconnect()).isEqualTo(true);
        assertThat(sut.isCancelCommandsOnReconnectFailure()).isEqualTo(false);
        assertThat(sut.isPingBeforeActivateConnection()).isEqualTo(false);
        assertThat(sut.isSuspendReconnectOnProtocolFailure()).isEqualTo(false);
        assertThat(sut.getDisconnectedBehavior()).isEqualTo(ClientOptions.DisconnectedBehavior.DEFAULT);
    }

    @Test
    public void variousClientOptions() throws Exception {

        RedisAsyncCommands<String, String> plain = client.connect().async();

        assertThat(getStatefulConnection(plain).getOptions().isAutoReconnect()).isTrue();

        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());
        RedisAsyncCommands<String, String> connection = client.connect().async();
        assertThat(getStatefulConnection(connection).getOptions().isAutoReconnect()).isFalse();

        assertThat(getStatefulConnection(plain).getOptions().isAutoReconnect()).isTrue();

    }

    @Test
    public void requestQueueSize() throws Exception {

        client.setOptions(new ClientOptions.Builder().requestQueueSize(10).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();
        getConnectionWatchdog(connection.getStatefulConnection()).setListenOnChannelInactive(false);

        connection.quit();

        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        for (int i = 0; i < 10; i++) {
            connection.ping();
        }

        try {
            connection.ping();
            fail("missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Request queue size exceeded");
        }

        connection.close();
    }

    @Test
    public void disconnectedWithoutReconnect() throws Exception {

        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
        try {
            connection.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            connection.close();
        }
    }

    @Test
    public void disconnectedRejectCommands() throws Exception {

        client.setOptions(new ClientOptions.Builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        getConnectionWatchdog(connection.getStatefulConnection()).setListenOnChannelInactive(false);
        connection.quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
        try {
            connection.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            connection.close();
        }
    }

    @Test
    public void disconnectedAcceptCommands() throws Exception {

        client.setOptions(new ClientOptions.Builder().autoReconnect(false)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
        connection.get(key);
        connection.close();
    }

    @Test(timeout = 10000)
    public void pingBeforeConnect() throws Exception {

        redis.set(key, value);
        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());
        RedisCommands<String, String> connection = client.connect().sync();

        try {
            String result = connection.get(key);
            assertThat(result).isEqualTo(value);
        } finally {
            connection.close();
        }
    }

}