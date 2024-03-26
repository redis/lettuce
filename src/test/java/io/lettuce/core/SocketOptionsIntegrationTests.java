package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.test.LettuceExtension;
import io.netty.channel.ConnectTimeoutException;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class SocketOptionsIntegrationTests extends TestSupport {

    private final RedisClient client;

    @Inject
    SocketOptionsIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @Test
    void testConnectTimeout() {

        SocketOptions socketOptions = SocketOptions.builder().connectTimeout(100, TimeUnit.MILLISECONDS).build();
        client.setOptions(ClientOptions.builder().socketOptions(socketOptions).build());

        try {
            client.connect(RedisURI.create("2:4:5:5::1", 60000));
            fail("Missing RedisConnectionException");
        } catch (RedisConnectionException e) {

            if (e.getCause() instanceof ConnectTimeoutException) {
                assertThat(e).hasRootCauseInstanceOf(ConnectTimeoutException.class);
                assertThat(e.getCause()).hasMessageContaining("connection timed out");
                return;
            }

            if (e.getCause() instanceof SocketException) {
                // Network is unreachable or No route to host are OK as well.
                return;
            }
        }
    }
}
