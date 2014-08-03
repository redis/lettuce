package com.lambdaworks.redis.models.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class RoleParserTest {
    public static final long REPLICATION_OFFSET_1 = 3167038L;
    public static final long REPLICATION_OFFSET_2 = 3167039L;
    public static final String LOCALHOST = "127.0.0.1";

    @Test(expected = IllegalArgumentException.class)
    public void emptyList() throws Exception {
        RoleParser.parse(Lists.newArrayList());

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFirstElement() throws Exception {
        RoleParser.parse(Lists.newArrayList(new Object()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRole() throws Exception {
        RoleParser.parse(Lists.newArrayList("blubb"));

    }

    @Test
    public void master() throws Exception {

        List<ImmutableList<String>> slaves = ImmutableList.of(ImmutableList.of(LOCALHOST, "9001", "" + REPLICATION_OFFSET_2),
                ImmutableList.of(LOCALHOST, "9002", "3129543"));

        ImmutableList<Object> input = ImmutableList.of("master", REPLICATION_OFFSET_1, slaves);

        RedisInstance result = RoleParser.parse(input);

        assertEquals(RedisInstance.Role.MASTER, result.getRole());
        assertTrue(result instanceof RedisMasterInstance);

        RedisMasterInstance instance = (RedisMasterInstance) result;

        assertEquals(REPLICATION_OFFSET_1, instance.getReplicationOffset());
        assertEquals(2, instance.getSlaves().size());

        ReplicationPartner slave1 = instance.getSlaves().get(0);
        assertEquals(LOCALHOST, slave1.getHost().getHostText());
        assertEquals(9001, slave1.getHost().getPort());
        assertEquals(REPLICATION_OFFSET_2, slave1.getReplicationOffset());

        assertEquals(instance, instance);
        assertEquals(instance.hashCode(), instance.hashCode());

    }

    @Test
    public void slave() throws Exception {

        List<?> input = ImmutableList.of("slave", LOCALHOST, 9000L, "connected", REPLICATION_OFFSET_1);

        RedisInstance result = RoleParser.parse(input);

        assertEquals(RedisInstance.Role.SLAVE, result.getRole());
        assertTrue(result instanceof RedisSlaveInstance);

        RedisSlaveInstance instance = (RedisSlaveInstance) result;

        assertEquals(REPLICATION_OFFSET_1, instance.getMaster().getReplicationOffset());
        assertEquals(RedisSlaveInstance.State.CONNECTED, instance.getState());

        assertEquals(instance, instance);
        assertEquals(instance.hashCode(), instance.hashCode());

    }

    @Test
    public void sentinel() throws Exception {

        List<?> input = ImmutableList
                .of("sentinel", ImmutableList.of("resque-master", "html-fragments-master", "stats-master"));

        RedisInstance result = RoleParser.parse(input);

        assertEquals(RedisInstance.Role.SENTINEL, result.getRole());
        assertTrue(result instanceof RedisSentinelInstance);

        RedisSentinelInstance instance = (RedisSentinelInstance) result;

        assertEquals(3, instance.getMonitoredMasters().size());

        assertEquals(instance, instance);
        assertEquals(instance.hashCode(), instance.hashCode());

    }
}
