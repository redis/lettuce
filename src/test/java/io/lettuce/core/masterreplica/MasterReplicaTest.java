/*
 * Copyright 2019-2022 the original author or authors.
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
package io.lettuce.core.masterreplica;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.models.role.RoleParser;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for master/replica via {@link MasterReplica}.
 *
 * @author Mark Paluch
 */
class MasterReplicaTest extends AbstractRedisClientTest {

    private RedisURI masterURI = RedisURI.Builder.redis(host, TestSettings.port(3)).withPassword(passwd)
            .withClientName("my-client").withDatabase(5).build();

    private StatefulRedisMasterReplicaConnection<String, String> connection;

    private RedisURI upstream;
    private RedisURI replica;

    private StatefulRedisConnection<String, String> connection1;

    private RedisCommands<String, String> redis1;

    private StatefulRedisConnection<String, String> connection2;

    private RedisCommands<String, String> redis2;

    @BeforeEach
    void before() {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withDatabase(2).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withDatabase(2).build();

        this.connection1 = client.connect(node1);
        this.redis1 = connection1.sync();
        this.connection2 = client.connect(node2);
        this.redis2 = connection2.sync();

        RedisInstance node1Instance = RoleParser.parse(this.redis1.role());
        RedisInstance node2Instance = RoleParser.parse(this.redis2.role());

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

        WithPassword.enableAuthentication(this.redis1);
        this.redis1.auth(passwd);
        this.redis1.configSet("masterauth", passwd.toString());

        WithPassword.enableAuthentication(this.redis2);
        this.redis2.auth(passwd);
        this.redis2.configSet("masterauth", passwd.toString());

        connection = MasterReplica.connect(client, StringCodec.UTF8, masterURI);
        connection.setReadFrom(ReadFrom.REPLICA);
    }

    @AfterEach
    void after() {

        if (redis1 != null) {
            WithPassword.disableAuthentication(redis1);
            redis1.configRewrite();
            connection1.close();
        }

        if (redis2 != null) {
            WithPassword.disableAuthentication(redis2);
            redis2.configRewrite();
            connection2.close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testMasterReplicaReadFromMaster() {

        connection.setReadFrom(ReadFrom.UPSTREAM);
        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + upstream.getPort());
    }

    @Test
    void testMasterReplicaReadFromReplica() {

        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + replica.getPort());
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.REPLICA);
    }

    @Test
    void testMasterReplicaReadWrite() {

        RedisCommands<String, String> redisCommands = connection.sync();
        redisCommands.set(key, value);
        redisCommands.waitForReplication(1, 100);

        assertThat(redisCommands.get(key)).isEqualTo(value);
    }

    @Test
    void testConnectToReplica() {

        connection.close();

        RedisURI replicaUri = RedisURI.Builder.redis(host, TestSettings.port(4)).withPassword(passwd).build();
        connection = MasterReplica.connect(client, StringCodec.UTF8, replicaUri);

        RedisCommands<String, String> sync = connection.sync();
        sync.set(key, value);
    }

    @Test
    void noReplicaForRead() {

        connection.setReadFrom(new ReadFrom() {
            @Override
            public List<RedisNodeDescription> select(Nodes nodes) {
                return Collections.emptyList();
            }
        });

        assertThatThrownBy(() -> replicaCall(connection)).isInstanceOf(RedisException.class);
    }

    @Test
    void masterReplicaConnectionShouldSetClientName() {

        assertThat(connection.sync().clientGetname()).isEqualTo(masterURI.getClientName());
        connection.sync().quit();
        assertThat(connection.sync().clientGetname()).isEqualTo(masterURI.getClientName());

        connection.close();
    }

    @Test
    @EnabledOnCommand("ACL")
    void testConnectToReplicaWithAcl() {

        connection.close();

        RedisURI replicaUri = RedisURI.Builder.redis(host, TestSettings.port(900 + 6)).withAuthentication("default", passwd)
                .build();
        connection = MasterReplica.connect(client, StringCodec.UTF8, replicaUri);

        RedisCommands<String, String> sync = connection.sync();

        assertThat(sync.ping()).isEqualTo("PONG");
    }

    static String replicaCall(StatefulRedisMasterReplicaConnection<String, String> connection) {
        return connection.sync().info("replication");
    }
}
