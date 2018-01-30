/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.netty.channel.ConnectTimeoutException;

/**
 * @author Mark Paluch
 */
public class SocketOptionsTest extends AbstractRedisClientTest {

    @Test
    public void testNew() throws Exception {
        checkAssertions(SocketOptions.create());
    }

    @Test
    public void testBuilder() throws Exception {

        SocketOptions sut = SocketOptions.builder().connectTimeout(1, TimeUnit.MINUTES).keepAlive(true).tcpNoDelay(true)
                .build();

        assertThat(sut.isKeepAlive()).isEqualTo(true);
        assertThat(sut.isTcpNoDelay()).isEqualTo(true);
        assertThat(sut.getConnectTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    public void testCopy() throws Exception {
        checkAssertions(SocketOptions.copyOf(SocketOptions.builder().build()));
    }

    protected void checkAssertions(SocketOptions sut) {
        assertThat(sut.isKeepAlive()).isEqualTo(false);
        assertThat(sut.isTcpNoDelay()).isEqualTo(false);
        assertThat(sut.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test(timeout = 1000)
    public void testConnectTimeout() {

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
