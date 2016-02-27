package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ReadFromTest {

    private Partitions sut = new Partitions();
    private RedisClusterNode nearest = new RedisClusterNode();
    private RedisClusterNode master = new RedisClusterNode();
    private RedisClusterNode slave = new RedisClusterNode();

    @Before
    public void before() throws Exception {
        master.setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MASTER));
        nearest.setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.SLAVE));
        slave.setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.SLAVE));

        sut.addPartition(nearest);
        sut.addPartition(master);
        sut.addPartition(slave);
    }

    @Test
    public void master() throws Exception {
        List<RedisNodeDescription> result = ReadFrom.MASTER.select(getNodes());
        assertThat(result).hasSize(1).containsOnly(master);
    }

    @Test
    public void masterPreferred() throws Exception {
        List<RedisNodeDescription> result = ReadFrom.MASTER_PREFERRED.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(master, nearest, slave);
    }

    @Test
    public void slave() throws Exception {
        List<RedisNodeDescription> result = ReadFrom.SLAVE.select(getNodes());
        assertThat(result).hasSize(2).contains(nearest, slave);
    }

    @Test
    public void nearest() throws Exception {
        List<RedisNodeDescription> result = ReadFrom.NEAREST.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(nearest, master, slave);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfNull() throws Exception {
        ReadFrom.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfUnknown() throws Exception {
        ReadFrom.valueOf("unknown");
    }

    @Test
    public void valueOfNearest() throws Exception {
        assertThat(ReadFrom.valueOf("nearest")).isEqualTo(ReadFrom.NEAREST);
    }

    @Test
    public void valueOfMaster() throws Exception {
        assertThat(ReadFrom.valueOf("master")).isEqualTo(ReadFrom.MASTER);
    }

    @Test
    public void valueOfMasterPreferred() throws Exception {
        assertThat(ReadFrom.valueOf("masterPreferred")).isEqualTo(ReadFrom.MASTER_PREFERRED);
    }

    @Test
    public void valueOfSlave() throws Exception {
        assertThat(ReadFrom.valueOf("slave")).isEqualTo(ReadFrom.SLAVE);
    }

    private ReadFrom.Nodes getNodes() {
        return new ReadFrom.Nodes() {
            @Override
            public List<RedisNodeDescription> getNodes() {
                return (List) sut.getPartitions();
            }

            @Override
            public Iterator<RedisNodeDescription> iterator() {
                return getNodes().iterator();
            }
        };

    }
}
