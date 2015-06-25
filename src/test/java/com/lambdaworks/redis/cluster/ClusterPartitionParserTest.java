package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

public class ClusterPartitionParserTest {

    private static String nodes = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
            + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999 [8000->-4213a8dabb94f92eb6a860f4d0729e6a25d43e0c] [5461-<-c37ab8396be428403d4e55c0d317348be27ed973]\n"
            + "4213a8dabb94f92eb6a860f4d0729e6a25d43e0c 127.0.0.1:7379 myself,slave 4213a8dabb94f92eb6a860f4d0729e6a25d43e0c 0 0 1 connected 0-6999 7001-7999 12001\n"
            + "5f4a2236d00008fba7ac0dd24b95762b446767bd :0 myself,master - 0 0 1 connected [5460->-5f4a2236d00008fba7ac0dd24b95762b446767bd] [5461-<-5f4a2236d00008fba7ac0dd24b95762b446767bd]";

    @Test
    public void testParse() throws Exception {

        Partitions result = ClusterPartitionParser.parse(nodes);

        assertThat(result.getPartitions()).hasSize(4);

        RedisClusterNode p1 = result.getPartitions().get(0);

        assertThat(p1.getNodeId()).isEqualTo("c37ab8396be428403d4e55c0d317348be27ed973");
        assertThat(p1.getUri().getHost()).isEqualTo("127.0.0.1");
        assertThat(p1.getUri().getPort()).isEqualTo(7381);
        assertThat(p1.getSlaveOf()).isNull();
        assertThat(p1.getFlags()).isEqualTo(ImmutableSet.of(RedisClusterNode.NodeFlag.MASTER));
        assertThat(p1.getPingSentTimestamp()).isEqualTo(111);
        assertThat(p1.getPongReceivedTimestamp()).isEqualTo(1401258245007L);
        assertThat(p1.getConfigEpoch()).isEqualTo(222);
        assertThat(p1.isConnected()).isTrue();

        assertThat(p1.getSlots(), hasItem(7000));
        assertThat(p1.getSlots(), hasItem(12000));
        assertThat(p1.getSlots(), hasItem(12002));
        assertThat(p1.getSlots(), hasItem(12003));
        assertThat(p1.getSlots(), hasItem(16383));

        RedisClusterNode p3 = result.getPartitions().get(2);

        assertThat(p3.getSlaveOf()).isEqualTo("4213a8dabb94f92eb6a860f4d0729e6a25d43e0c");
        assertThat(p3.toString()).contains(RedisClusterNode.class.getSimpleName());
        assertThat(result.toString()).contains(Partitions.class.getSimpleName());


    }

    @Test
    public void getNodeByHash() throws Exception {

        Partitions partitions = ClusterPartitionParser.parse(nodes);
        assertThat(partitions.getPartitionBySlot(7000).getNodeId()).isEqualTo("c37ab8396be428403d4e55c0d317348be27ed973");
        assertThat(partitions.getPartitionBySlot(5460).getNodeId()).isEqualTo("4213a8dabb94f92eb6a860f4d0729e6a25d43e0c");

    }

    @Test
    public void testModel() throws Exception {
        RedisClusterNode node = new RedisClusterNode();
        node.setConfigEpoch(1);
        node.setConnected(true);
        node.setFlags(Sets.<RedisClusterNode.NodeFlag> newHashSet());
        node.setNodeId("abcd");
        node.setPingSentTimestamp(2);
        node.setPongReceivedTimestamp(3);
        node.setSlaveOf("me");
        node.setSlots(Lists.<Integer> newArrayList());
        node.setUri(new RedisURI("localhost", 1, 1, TimeUnit.DAYS));

        assertThat(node.toString()).contains(RedisClusterNode.class.getSimpleName());

    }
}
