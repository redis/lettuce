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
package io.lettuce.core.models.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceLists;

/**
 * @author Mark Paluch
 */
class RoleParserUnitTests {

    private static final long REPLICATION_OFFSET_1 = 3167038L;
    private static final long REPLICATION_OFFSET_2 = 3167039L;
    private static final String LOCALHOST = "127.0.0.1";

    @Test
    void testMappings() {
        assertThat(RoleParser.REPLICA_STATE_MAPPING).hasSameSizeAs(RedisSlaveInstance.State.values());
    }

    @Test
    void emptyList() {
        assertThatThrownBy(() -> RoleParser.parse(new ArrayList<>())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidFirstElement() {
        assertThatThrownBy(() -> RoleParser.parse(LettuceLists.newList(new Object()))).isInstanceOf(
                IllegalArgumentException.class);
    }

    @Test
    void invalidRole() {
        assertThatThrownBy(() -> RoleParser.parse(LettuceLists.newList("blubb"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upstream() {

        List<List<String>> slaves = LettuceLists.newList(LettuceLists.newList(LOCALHOST, "9001", "" + REPLICATION_OFFSET_2),
                LettuceLists.newList(LOCALHOST, "9002", "3129543"));

        List<Object> input = LettuceLists.newList("master", REPLICATION_OFFSET_1, slaves);

        RedisInstance result = RoleParser.parse(input);

        assertThat(result.getRole().isUpstream()).isTrue();
        assertThat(result instanceof RedisMasterInstance).isTrue();

        RedisMasterInstance instance = (RedisMasterInstance) result;

        assertThat(instance.getReplicationOffset()).isEqualTo(REPLICATION_OFFSET_1);
        assertThat(instance.getSlaves()).hasSize(2);

        ReplicationPartner replica1 = instance.getSlaves().get(0);
        assertThat(replica1.getHost().getHostText()).isEqualTo(LOCALHOST);
        assertThat(replica1.getHost().getPort()).isEqualTo(9001);
        assertThat(replica1.getReplicationOffset()).isEqualTo(REPLICATION_OFFSET_2);

        assertThat(instance.toString()).startsWith(RedisMasterInstance.class.getSimpleName());
        assertThat(replica1.toString()).startsWith(ReplicationPartner.class.getSimpleName());
    }

    @Test
    void replica() {

        List<?> input = LettuceLists.newList("slave", LOCALHOST, 9000L, "connected", REPLICATION_OFFSET_1);

        RedisInstance result = RoleParser.parse(input);

        assertThat(result.getRole().isReplica()).isTrue();
        assertThat(result instanceof RedisReplicaInstance).isTrue();

        RedisReplicaInstance instance = (RedisReplicaInstance) result;

        assertThat(instance.getUpstream().getReplicationOffset()).isEqualTo(REPLICATION_OFFSET_1);
        assertThat(instance.getState()).isEqualTo(RedisReplicaInstance.State.CONNECTED);

        assertThat(instance.toString()).startsWith(RedisReplicaInstance.class.getSimpleName());

    }

    @Test
    void sentinel() {

        List<?> input = LettuceLists.newList("sentinel", LettuceLists.newList("resque-master", "html-fragments-master", "stats-master"));

        RedisInstance result = RoleParser.parse(input);

        assertThat(result.getRole()).isEqualTo(RedisInstance.Role.SENTINEL);
        assertThat(result instanceof RedisSentinelInstance).isTrue();

        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertThat(instance.getMonitoredMasters()).hasSize(3);

        assertThat(instance.toString()).startsWith(RedisSentinelInstance.class.getSimpleName());

    }

    @Test
    void sentinelWithoutMasters() {

        List<?> input = LettuceLists.newList("sentinel");

        RedisInstance result = RoleParser.parse(input);
        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertThat(instance.getMonitoredMasters()).hasSize(0);

    }

    @Test
    void sentinelMastersIsNotAList() {

        List<?> input = LettuceLists.newList("sentinel", "");

        RedisInstance result = RoleParser.parse(input);
        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertThat(instance.getMonitoredMasters()).hasSize(0);

    }

    @Test
    void testModelTest() {

        RedisMasterInstance master = new RedisMasterInstance();
        master.setReplicationOffset(1);
        master.setSlaves(new ArrayList<>());
        assertThat(master.toString()).contains(RedisMasterInstance.class.getSimpleName());

        RedisSlaveInstance slave = new RedisSlaveInstance();
        slave.setMaster(new ReplicationPartner());
        slave.setState(RedisSlaveInstance.State.CONNECT);
        assertThat(slave.toString()).contains(RedisSlaveInstance.class.getSimpleName());

        RedisSentinelInstance sentinel = new RedisSentinelInstance();
        sentinel.setMonitoredMasters(new ArrayList<>());
        assertThat(sentinel.toString()).contains(RedisSentinelInstance.class.getSimpleName());

        ReplicationPartner partner = new ReplicationPartner();
        partner.setHost(HostAndPort.parse("localhost"));
        partner.setReplicationOffset(12);

        assertThat(partner.toString()).contains(ReplicationPartner.class.getSimpleName());
    }
}
