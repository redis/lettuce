/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import reactor.core.publisher.Mono;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterNodeConnectionFactory.ConnectionKey;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.AsyncConnectionProvider;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.ConnectTimeoutException;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class AsyncConnectionProviderIntegrationTests {

    private final ClientResources resources;

    private RedisClusterClient client;

    private ServerSocket serverSocket;

    private CountDownLatch connectInitiated = new CountDownLatch(1);

    private AsyncConnectionProvider<ConnectionKey, StatefulRedisConnection<String, String>, ConnectionFuture<StatefulRedisConnection<String, String>>> sut;

    @Inject
    AsyncConnectionProviderIntegrationTests(ClientResources resources) {
        this.resources = resources;
    }

    @BeforeEach
    void before() throws Exception {

        serverSocket = new ServerSocket(9393, 1);

        client = RedisClusterClient.create(resources, "redis://localhost");
        client.setOptions(ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        sut = new AsyncConnectionProvider<>(new AbstractClusterNodeConnectionFactory<String, String>(resources) {

            @Override
            public ConnectionFuture<StatefulRedisConnection<String, String>> apply(ConnectionKey connectionKey) {

                RedisURI redisURI = RedisURI.create(TestSettings.host(), serverSocket.getLocalPort());
                redisURI.setTimeout(Duration.ofSeconds(5));

                ConnectionFuture<StatefulRedisConnection<String, String>> future = client.connectToNodeAsync(StringCodec.UTF8,
                        "", null, Mono.just(new InetSocketAddress(connectionKey.host, serverSocket.getLocalPort())));

                connectInitiated.countDown();

                return future;
            }

        });
    }

    @AfterEach
    void after() throws Exception {
        serverSocket.close();
    }

    @Test
    void shouldCloseConnectionByKey() throws IOException {

        ConnectionKey connectionKey = new ConnectionKey(ConnectionIntent.READ, TestSettings.host(), TestSettings.port());

        sut.getConnection(connectionKey);
        sut.close(connectionKey);

        assertThat(sut.getConnectionCount()).isEqualTo(0);
        sut.close();

        serverSocket.accept();
    }

    @Test
    void shouldCloseConnections() throws IOException {

        ConnectionKey connectionKey = new ConnectionKey(ConnectionIntent.READ, TestSettings.host(), TestSettings.port());

        sut.getConnection(connectionKey);
        TestFutures.awaitOrTimeout(sut.close());

        assertThat(sut.getConnectionCount()).isEqualTo(0);
        TestFutures.awaitOrTimeout(sut.close());

        serverSocket.accept();
    }

    @Test
    void connectShouldFail() throws Exception {

        Socket socket = new Socket(TestSettings.host(), serverSocket.getLocalPort());

        ClusterClientOptions clientOptions = ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
                .socketOptions(SocketOptions.builder().connectTimeout(1, TimeUnit.SECONDS).build()).build();

        client.setOptions(clientOptions);

        ConnectionKey connectionKey = new ConnectionKey(ConnectionIntent.READ, "8.8.8.8", TestSettings.port());

        StopWatch stopWatch = new StopWatch();

        assertThatThrownBy(() -> TestFutures.awaitOrTimeout(sut.getConnection(connectionKey)))
                .hasRootCauseInstanceOf(ConnectException.class);

        stopWatch.start();

        assertThatThrownBy(() -> TestFutures.awaitOrTimeout(sut.getConnection(connectionKey)))
                .hasRootCauseInstanceOf(ConnectException.class);

        stopWatch.stop();

        assertThat(TimeUnit.NANOSECONDS.toMillis(stopWatch.getNanoTime())).isBetween(0L, 1200L);

        sut.close();

        socket.close();
    }

    @Test
    void connectShouldFailConcurrently() throws Exception {

        Socket socket = new Socket(TestSettings.host(), serverSocket.getLocalPort());

        ClusterClientOptions clientOptions = ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
                .socketOptions(SocketOptions.builder().connectTimeout(1, TimeUnit.SECONDS).build()).build();

        client.setOptions(clientOptions);

        ConnectionKey connectionKey = new ConnectionKey(ConnectionIntent.READ, "8.8.8.8", TestSettings.port());

        Thread t1 = new Thread(() -> {
            try {
                sut.getConnection(connectionKey);
            } catch (Exception e) {
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                sut.getConnection(connectionKey);
            } catch (Exception e) {
            }
        });

        t1.start();
        t2.start();

        connectInitiated.await();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        t1.join(2000);
        t2.join(2000);

        stopWatch.stop();

        assertThat(TimeUnit.NANOSECONDS.toMillis(stopWatch.getNanoTime())).isBetween(0L, 1300L);

        sut.close();
        socket.close();
    }

}
