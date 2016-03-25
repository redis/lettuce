package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ClientOptionsTest extends AbstractCommandTest{

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
}
