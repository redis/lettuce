package com.lambdaworks.redis.cluster;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class ClusterPartitionParserTest {

    private static String nodes = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
            + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999 [8000->-4213a8dabb94f92eb6a860f4d0729e6a25d43e0c] [5461-<-c37ab8396be428403d4e55c0d317348be27ed973]\n"
            + "4213a8dabb94f92eb6a860f4d0729e6a25d43e0c 127.0.0.1:7379 myself,slave 4213a8dabb94f92eb6a860f4d0729e6a25d43e0c 0 0 1 connected 0-6999 7001-7999 12001\n"
            + "5f4a2236d00008fba7ac0dd24b95762b446767bd :0 myself,master - 0 0 1 connected [5460->-5f4a2236d00008fba7ac0dd24b95762b446767bd] [5461-<-5f4a2236d00008fba7ac0dd24b95762b446767bd]";

    @Test
    public void testParse() throws Exception {

        Partitions result = ClusterPartitionParser.parse(nodes);

        assertEquals(4, result.getPartitions().size());

        RedisClusterNode p1 = result.getPartitions().get(0);

        assertEquals("c37ab8396be428403d4e55c0d317348be27ed973", p1.getNodeId());
        assertEquals("127.0.0.1", p1.getUri().getHost());
        assertEquals(7381, p1.getUri().getPort());
        assertNull(p1.getSlaveOf());
        assertEquals(ImmutableSet.of(RedisClusterNode.NodeFlag.MASTER), p1.getFlags());
        assertEquals(111, p1.getPingSentTimestamp());
        assertEquals(1401258245007L, p1.getPongReceivedTimestamp());
        assertEquals(222, p1.getConfigEpoch());
        assertTrue(p1.isConnected());

        assertThat(p1.getSlots(), hasItem(7000));
        assertThat(p1.getSlots(), hasItem(12000));
        assertThat(p1.getSlots(), hasItem(12002));
        assertThat(p1.getSlots(), hasItem(12003));
        assertThat(p1.getSlots(), hasItem(16383));

        RedisClusterNode p3 = result.getPartitions().get(2);

        assertEquals("4213a8dabb94f92eb6a860f4d0729e6a25d43e0c", p3.getSlaveOf());

    }

    @Test
    public void getNodeByHash() throws Exception {

        Partitions partitions = ClusterPartitionParser.parse(nodes);
        assertEquals("c37ab8396be428403d4e55c0d317348be27ed973", partitions.getPartitionBySlot(7000).getNodeId());
        assertEquals("4213a8dabb94f92eb6a860f4d0729e6a25d43e0c", partitions.getPartitionBySlot(5460).getNodeId());

    }
}
