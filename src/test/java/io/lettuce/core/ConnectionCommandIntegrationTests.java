/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * @author Will Glozer
 * @author Mark Paluch
 * @author Tugdual Grall
 */
@ExtendWith(LettuceExtension.class)
class ConnectionCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> redis;

    @Inject
    ConnectionCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
        this.redis = connection.sync();
    }

    @BeforeEach
    void setUp() {
        redis.flushall();
    }

    @Test
    void auth() {

        WithPassword.run(client, () -> {
            client.setOptions(
                    ClientOptions.builder().pingBeforeActivateConnection(false).protocolVersion(ProtocolVersion.RESP2).build());
            RedisCommands<String, String> connection = client.connect().sync();

            assertThatThrownBy(connection::ping).isInstanceOf(RedisException.class)
                    .hasMessageContaining("NOAUTH");

            assertThat(connection.auth(passwd)).isEqualTo("OK");
            assertThat(connection.set(key, value)).isEqualTo("OK");

            RedisURI redisURI = RedisURI.Builder.redis(host, port).withDatabase(2).withPassword(passwd).build();
            try (StatefulRedisConnection<String, String> authConnection = client.connect(redisURI)) {
                RedisCommands<String, String> sync = authConnection.sync();
                sync.ping();
            }
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void authWithUsername() {

        WithPassword.run(client, () -> {
            client.setOptions(
                    ClientOptions.builder().pingBeforeActivateConnection(false).protocolVersion(ProtocolVersion.RESP2).build());
            RedisCommands<String, String> connection = client.connect().sync();

            assertThatThrownBy(connection::ping).isInstanceOf(RedisException.class)
                    .hasMessageContaining("NOAUTH");

            assertThat(connection.auth(passwd)).isEqualTo("OK");
            assertThat(connection.set(key, value)).isEqualTo("OK");

            // Aut with the same user & password (default)
            assertThat(connection.auth(username, passwd)).isEqualTo("OK");
            assertThat(connection.set(key, value)).isEqualTo("OK");

            // Switch to another user
            assertThat(connection.auth(aclUsername, aclPasswd)).isEqualTo("OK");
            assertThat(connection.set("cached:demo", value)).isEqualTo("OK");
            assertThatThrownBy(() -> connection.get(key)).isInstanceOf(RedisCommandExecutionException.class);
            assertThat(connection.del("cached:demo")).isEqualTo(1);

            RedisURI redisURI = RedisURI.Builder.redis(host, port).withDatabase(2).withPassword(passwd).build();
            RedisCommands<String, String> sync;
            try (StatefulRedisConnection<String, String> authConnection = client.connect(redisURI)) {
                sync = authConnection.sync();
                sync.ping();
            }
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void resp2HandShakeWithUsernamePassword() {

        RedisURI redisURI = RedisURI.Builder.redis(host, port).withAuthentication(username, passwd).build();
        RedisClient clientResp2 = RedisClient.create(redisURI);
        clientResp2.setOptions(
                ClientOptions.builder().pingBeforeActivateConnection(false).protocolVersion(ProtocolVersion.RESP2).build());
        StatefulRedisConnection<String, String> connTestResp2 = null;

        try {
            connTestResp2 = clientResp2.connect();
            assertThat(connTestResp2.sync().ping()).isEqualTo("PONG");
        } catch (Exception e) {
        } finally {
            assertThat(connTestResp2).isNotNull();
            connTestResp2.close();
        }
        clientResp2.shutdown();
    }

    @Test
    void echo() {
        assertThat(redis.echo("hello")).isEqualTo("hello");
    }

    @Test
    void ping() {
        assertThat(redis.ping()).isEqualTo("PONG");
    }

    @Test
    void select() {
        redis.set(key, value);
        assertThat(redis.select(1)).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    void authNull() {
        assertThatThrownBy(() -> redis.auth(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.auth(null, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authEmpty() {
        assertThatThrownBy(() -> redis.auth("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.auth("", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authReconnect() {
        WithPassword.run(client, () -> {

            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());
            StatefulRedisConnection<String, String> connection = client.connect();
            RedisCommands<String, String> sync = connection.sync();
            assertThat(sync.auth(passwd)).isEqualTo("OK");
            assertThat(sync.set(key, value)).isEqualTo("OK");
            sync.quit();

            Delay.delay(Duration.ofMillis(100));
            assertThat(sync.get(key)).isEqualTo(value);

            connection.close();
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void authReconnectRedis6() {
        WithPassword.run(client, () -> {

            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());
            StatefulRedisConnection<String, String> connection = client.connect();
            RedisCommands<String, String> sync = connection.sync();
            assertThat(sync.auth(passwd)).isEqualTo("OK");
            assertThat(sync.set(key, value)).isEqualTo("OK");
            sync.quit();

            Delay.delay(Duration.ofMillis(100));
            assertThat(sync.get(key)).isEqualTo(value);

            // reconnect with username/password
            assertThat(sync.auth(username, passwd)).isEqualTo("OK");
            assertThat(sync.set(key, value)).isEqualTo("OK");
            sync.quit();

            Delay.delay(Duration.ofMillis(100));
            assertThat(sync.get(key)).isEqualTo(value);

            connection.close();
        });
    }

    @Test
    void selectReconnect() {
        redis.select(1);
        redis.set(key, value);
        redis.quit();

        Wait.untilTrue(connection::isOpen).waitOrTimeout();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void getSetReconnect() {
        redis.set(key, value);
        redis.quit();
        Wait.untilTrue(connection::isOpen).waitOrTimeout();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void authInvalidPassword() {
        StatefulRedisConnection<String, String> connection = client.connect();
        RedisAsyncCommands<String, String> async = connection.async();
        try {
            TestFutures.awaitOrTimeout(async.auth("invalid"));
            fail("Authenticated with invalid password");
        } catch (RedisException e) {
            assertThat(e.getMessage()).startsWith("ERR").contains("AUTH");
            StatefulRedisConnectionImpl<String, String> statefulRedisCommands = (StatefulRedisConnectionImpl) connection;
            assertThat(statefulRedisCommands.getConnectionState()).extracting("password").isNull();
        } finally {
            connection.close();
        }
    }

    @Test
    @EnabledOnCommand("ACL")
    void authInvalidUsernamePassword() {

        WithPassword.run(client, () -> {
            client.setOptions(
                    ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false).build());
            StatefulRedisConnection<String, String> connection = client.connect();
            RedisCommands<String, String> sync = connection.sync();

            assertThat(sync.auth(username, passwd)).isEqualTo("OK");

            assertThatThrownBy(() -> sync.auth(username, "invalid"))
                    .hasMessageContaining("WRONGPASS invalid username-password pair");

            assertThat(sync.auth(aclUsername, aclPasswd)).isEqualTo("OK");

            assertThatThrownBy(() -> sync.auth(aclUsername, "invalid"))
                    .hasMessageContaining("WRONGPASS invalid username-password pair");

            connection.close();
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void authInvalidDefaultPasswordNoACL() {
        StatefulRedisConnection<String, String> connection = client.connect();
        RedisAsyncCommands<String, String> async = connection.async();
        // When the database is not secured the AUTH default invalid command returns OK
        try {
            Future<String> auth = async.auth(username, "invalid");
            assertThat(TestFutures.getOrTimeout(auth)).isEqualTo("OK");
        } finally {
            connection.close();
        }
    }

    @Test
    void authInvalidUsernamePasswordNoACL() {
        StatefulRedisConnection<String, String> connection = client.connect();
        RedisAsyncCommands<String, String> async = connection.async();
        try {
            TestFutures.awaitOrTimeout(async.select(1024));
            fail("Selected invalid db index");
        } catch (RedisException e) {
            assertThat(e.getMessage()).startsWith("ERR");
            StatefulRedisConnectionImpl<String, String> statefulRedisCommands = (StatefulRedisConnectionImpl) connection;
            assertThat(statefulRedisCommands.getConnectionState()).extracting("db").isEqualTo(0);
        } finally {
            connection.close();
        }
    }

    @Test
    void testDoubleToString() {

        assertThat(LettuceStrings.string(1.1)).isEqualTo("1.1");
        assertThat(LettuceStrings.string(Double.POSITIVE_INFINITY)).isEqualTo("+inf");
        assertThat(LettuceStrings.string(Double.NEGATIVE_INFINITY)).isEqualTo("-inf");
    }
}
