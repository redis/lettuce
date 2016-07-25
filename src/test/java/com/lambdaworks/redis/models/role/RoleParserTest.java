package com.lambdaworks.redis.models.role;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.internal.HostAndPort;
import org.junit.Test;

import com.lambdaworks.redis.internal.LettuceLists;

public class RoleParserTest {
    public static final long REPLICATION_OFFSET_1 = 3167038L;
    public static final long REPLICATION_OFFSET_2 = 3167039L;
    public static final String LOCALHOST = "127.0.0.1";

    @Test
    public void testMappings() throws Exception {
        assertThat(RoleParser.ROLE_MAPPING).hasSameSizeAs(RedisInstance.Role.values());
        assertThat(RoleParser.SLAVE_STATE_MAPPING).hasSameSizeAs(RedisSlaveInstance.State.values());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyList() throws Exception {
        RoleParser.parse(new ArrayList<>());

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFirstElement() throws Exception {
        RoleParser.parse(LettuceLists.newList(new Object()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRole() throws Exception {
        RoleParser.parse(LettuceLists.newList("blubb"));

    }

    @Test
    public void master() throws Exception {

        List<List<String>> slaves = LettuceLists.newList(LettuceLists.newList(LOCALHOST, "9001", "" + REPLICATION_OFFSET_2),
                LettuceLists.newList(LOCALHOST, "9002", "3129543"));

        List<Object> input = LettuceLists.newList("master", REPLICATION_OFFSET_1, slaves);

        RedisInstance result = RoleParser.parse(input);

        assertThat(result.getRole()).isEqualTo(RedisInstance.Role.MASTER);
        assertThat(result instanceof RedisMasterInstance).isTrue();

        RedisMasterInstance instance = (RedisMasterInstance) result;

        assertThat(instance.getReplicationOffset()).isEqualTo(REPLICATION_OFFSET_1);
        assertThat(instance.getSlaves()).hasSize(2);

        ReplicationPartner slave1 = instance.getSlaves().get(0);
        assertThat(slave1.getHost().getHostText()).isEqualTo(LOCALHOST);
        assertThat(slave1.getHost().getPort()).isEqualTo(9001);
        assertThat(slave1.getReplicationOffset()).isEqualTo(REPLICATION_OFFSET_2);

        assertThat(instance.toString()).startsWith(RedisMasterInstance.class.getSimpleName());
        assertThat(slave1.toString()).startsWith(ReplicationPartner.class.getSimpleName());

    }

    @Test
    public void slave() throws Exception {

        List<?> input = LettuceLists.newList("slave", LOCALHOST, 9000L, "connected", REPLICATION_OFFSET_1);

        RedisInstance result = RoleParser.parse(input);

        assertThat(result.getRole()).isEqualTo(RedisInstance.Role.SLAVE);
        assertThat(result instanceof RedisSlaveInstance).isTrue();

        RedisSlaveInstance instance = (RedisSlaveInstance) result;

        assertThat(instance.getMaster().getReplicationOffset()).isEqualTo(REPLICATION_OFFSET_1);
        assertThat(instance.getState()).isEqualTo(RedisSlaveInstance.State.CONNECTED);

        assertThat(instance.toString()).startsWith(RedisSlaveInstance.class.getSimpleName());

    }

    @Test
    public void sentinel() throws Exception {

        List<?> input = LettuceLists.newList("sentinel", LettuceLists.newList("resque-master", "html-fragments-master", "stats-master"));

        RedisInstance result = RoleParser.parse(input);

        assertThat(result.getRole()).isEqualTo(RedisInstance.Role.SENTINEL);
        assertThat(result instanceof RedisSentinelInstance).isTrue();

        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertThat(instance.getMonitoredMasters()).hasSize(3);

        assertThat(instance.toString()).startsWith(RedisSentinelInstance.class.getSimpleName());

    }

    @Test
    public void sentinelWithoutMasters() throws Exception {

        List<?> input = LettuceLists.newList("sentinel");

        RedisInstance result = RoleParser.parse(input);
        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertThat(instance.getMonitoredMasters()).hasSize(0);

    }

    @Test
    public void sentinelMastersIsNotAList() throws Exception {

        List<?> input = LettuceLists.newList("sentinel", "");

        RedisInstance result = RoleParser.parse(input);
        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertThat(instance.getMonitoredMasters()).hasSize(0);

    }

    @Test
    public void testModelTest() throws Exception {

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
