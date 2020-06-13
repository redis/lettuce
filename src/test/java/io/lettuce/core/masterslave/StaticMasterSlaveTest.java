/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.masterslave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RoleParser;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
class StaticMasterSlaveTest extends AbstractRedisClientTest {

    private StatefulRedisMasterSlaveConnection<String, String> connection;

    private RedisURI upstream;
    private RedisURI replica;

    private RedisCommands<String, String> connection1;
    private RedisCommands<String, String> connection2;

    @BeforeEach
    void before() {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withClientName("my-client").withDatabase(2).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withClientName("my-client").withDatabase(2).build();

        this.connection1 = client.connect(node1).sync();
        this.connection2 = client.connect(node2).sync();

        RedisInstance node1Instance = RoleParser.parse(this.connection1.role());
        RedisInstance node2Instance = RoleParser.parse(this.connection2.role());

        if (node1Instance.getRole().isUpstream() && node2Instance.getRole().isReplica()) {
            upstream = node1;
            replica = node2;
        } else if (node2Instance.getRole().isUpstream() && node1Instance.getRole().isReplica()) {
            upstream = node2;
            replica = node1;
        } else {
            assumeTrue(false,
                    String.format("Cannot run the test because I don't have a distinct master and replica but %s and %s",
                            node1Instance, node2Instance));
        }

        WithPassword.enableAuthentication(this.connection1);
        this.connection1.auth(passwd);
        this.connection1.configSet("masterauth", passwd);

        WithPassword.enableAuthentication(this.connection2);
        this.connection2.auth(passwd);
        this.connection2.configSet("masterauth", passwd);

        upstream.setPassword(passwd);
        replica.setPassword(passwd);

        connection = MasterSlave.connect(client, StringCodec.UTF8, Arrays.asList(upstream, replica));
        connection.setReadFrom(ReadFrom.REPLICA);
    }

    @AfterEach
    void after() throws Exception {

        if (connection1 != null) {
            WithPassword.disableAuthentication(connection1);
            connection1.configSet("masterauth", "");
            connection1.configRewrite();
            connection1.getStatefulConnection().close();
        }

        if (connection2 != null) {
            WithPassword.disableAuthentication(connection2);
            connection2.configSet("masterauth", "");
            connection2.configRewrite();
            connection2.getStatefulConnection().close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testMasterSlaveStandaloneBasic() {

        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("6483");
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.REPLICA);
    }

    @Test
    void testMasterSlaveReadWrite() {

        RedisCommands<String, String> redisCommands = connection.sync();
        redisCommands.set(key, value);
        redisCommands.waitForReplication(1, 100);

        assertThat(redisCommands.get(key)).isEqualTo(value);
    }

    @Test
    void noSlaveForRead() {

        connection.close();

        connection = MasterSlave.connect(client, StringCodec.UTF8, Arrays.asList(upstream));
        connection.setReadFrom(ReadFrom.REPLICA);

        assertThatThrownBy(() -> slaveCall(connection)).isInstanceOf(RedisException.class);
    }

    @Test
    void shouldWorkWithMasterOnly() {

        connection.close();

        connection = MasterSlave.connect(client, StringCodec.UTF8, Arrays.asList(upstream));

        connection.sync().set(key, value);
        assertThat(connection.sync().get(key)).isEqualTo("value");
    }

    @Test
    void shouldWorkWithSlaveOnly() {

        connection.close();

        connection = MasterSlave.connect(client, StringCodec.UTF8, Arrays.asList(replica));
        connection.setReadFrom(ReadFrom.UPSTREAM_PREFERRED);

        assertThat(connection.sync().info()).isNotEmpty();
    }

    @Test
    void noMasterForWrite() {

        connection.close();

        connection = MasterSlave.connect(client, StringCodec.UTF8, Arrays.asList(replica));

        assertThatThrownBy(() -> connection.sync().set(key, value)).isInstanceOf(RedisException.class);
    }

    @Test
    void masterSlaveConnectionShouldSetClientName() {

        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");
        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo("my-client");

        connection.close();
    }

    static String slaveCall(StatefulRedisMasterSlaveConnection<String, String> connection) {
        return connection.sync().info("replication");
    }

}
