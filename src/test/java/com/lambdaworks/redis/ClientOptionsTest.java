package com.lambdaworks.redis;

import static com.lambdaworks.Connections.getChannel;
import static com.lambdaworks.Connections.getConnectionWatchdog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
public class ClientOptionsTest extends AbstractCommandTest {

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
    }

    @Test
    public void pingBeforeConnectWithAuthentication() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = new RedisURI.Builder().redis(host, port).withPassword(passwd).build();

                RedisConnection<String, String> connection = client.connect(redisURI);

                try {
                    String result = connection.info();
                    assertThat(result).contains("memory");
                } finally {
                    connection.close();
                }

            }
        };
    }

    @Test
    public void pingBeforeConnectWithSslAndAuthentication() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = new RedisURI.Builder().redis(host, 6443).withPassword(passwd).withVerifyPeer(false)
                        .withSsl(true).build();

                RedisConnection<String, String> connection = client.connect(redisURI);

                try {
                    String result = connection.info();
                    assertThat(result).contains("memory");
                } finally {
                    connection.close();
                }

            }
        };
    }

    @Test
    public void pingBeforeConnectWithAuthenticationFails() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = new RedisURI.Builder().redis(host, port).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisConnectionException e) {
                    assertThat(e).hasRootCauseInstanceOf(RedisCommandExecutionException.class);
                }
            }
        };
    }

    @Test
    public void pingBeforeConnectWithSslAndAuthenticationFails() throws Exception {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = new RedisURI.Builder().redis(host, 6443).withVerifyPeer(false).withSsl(true).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisConnectionException e) {
                    assertThat(e).hasRootCauseInstanceOf(RedisCommandExecutionException.class);
                }

            }
        };
    }

    @Test(timeout = 10000)
    public void pingBeforeConnectWithQueuedCommandsAndReconnect() throws Exception {

        RedisAsyncConnectionImpl<String, String> controlConnection = (RedisAsyncConnectionImpl) client.connectAsync();

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        Utf8StringCodec codec = new Utf8StringCodec();

        RedisAsyncConnectionImpl<String, String> redisConnection = (RedisAsyncConnectionImpl) client
                .connectAsync(RedisURI.create("redis://localhost:6479/5"));
        redisConnection.set("key1", "value1");
        redisConnection.set("key2", "value2");

        RedisCommand<String, String, String> sleep = controlConnection
                .dispatch(new Command<String, String, String>(CommandType.DEBUG, new StatusOutput<String, String>(codec),
                        new CommandArgs<String, String>(codec).add("SLEEP").add(2)));

        sleep.await(100, TimeUnit.MILLISECONDS);

        Channel channel = getChannel(redisConnection);
        ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
        connectionWatchdog.setReconnectSuspended(true);

        channel.close().get();
        sleep.get();

        redisConnection.get(key).cancel(true);

        RedisFuture<String> getFuture1 = redisConnection.get("key1");
        RedisFuture<String> getFuture2 = redisConnection.get("key2");
        getFuture1.await(100, TimeUnit.MILLISECONDS);

        connectionWatchdog.setReconnectSuspended(false);
        connectionWatchdog.scheduleReconnect();

        assertThat(getFuture1.get()).isEqualTo("value1");
        assertThat(getFuture2.get()).isEqualTo("value2");
    }

    @Test(timeout = 10000)
    public void authenticatedPingBeforeConnectWithQueuedCommandsAndReconnect() throws Exception {

        new WithPasswordRequired() {

            @Override
            protected void run(RedisClient client) throws Exception {

                RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).withDatabase(5).build();
                RedisAsyncConnectionImpl<String, String> controlConnection = (RedisAsyncConnectionImpl) client
                        .connectAsync(redisURI);

                client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

                Utf8StringCodec codec = new Utf8StringCodec();

                RedisAsyncConnectionImpl<String, String> redisConnection = (RedisAsyncConnectionImpl) client
                        .connectAsync(redisURI);
                redisConnection.set("key1", "value1");
                redisConnection.set("key2", "value2");

                RedisCommand<String, String, String> sleep = controlConnection.dispatch(
                        new Command<String, String, String>(CommandType.DEBUG, new StatusOutput<String, String>(codec),
                                new CommandArgs<String, String>(codec).add("SLEEP").add(2)));

                sleep.await(100, TimeUnit.MILLISECONDS);

                Channel channel = getChannel(redisConnection);
                ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
                connectionWatchdog.setReconnectSuspended(true);

                channel.close().get();
                sleep.get();

                redisConnection.get(key).cancel(true);

                RedisFuture<String> getFuture1 = redisConnection.get("key1");
                RedisFuture<String> getFuture2 = redisConnection.get("key2");
                getFuture1.await(100, TimeUnit.MILLISECONDS);

                connectionWatchdog.setReconnectSuspended(false);
                connectionWatchdog.scheduleReconnect();

                assertThat(getFuture1.get()).isEqualTo("value1");
                assertThat(getFuture2.get()).isEqualTo("value2");
            }
        };

    }
}
