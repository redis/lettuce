/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * @author Mark Paluch
 * @author Ryosuke Hasebe
 */
public class ReadFromTest {

    private Partitions sut = new Partitions();
    private RedisClusterNode nearest = new RedisClusterNode();
    private RedisClusterNode master = new RedisClusterNode();
    private RedisClusterNode slave = new RedisClusterNode();

    @Before
    public void before() {

        master.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MASTER));
        nearest.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.SLAVE));
        slave.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.SLAVE));

        sut.addPartition(nearest);
        sut.addPartition(master);
        sut.addPartition(slave);
    }

    @Test
    public void master() {
        List<RedisNodeDescription> result = ReadFrom.MASTER.select(getNodes());
        assertThat(result).hasSize(1).containsOnly(master);
    }

    @Test
    public void masterPreferred() {
        List<RedisNodeDescription> result = ReadFrom.MASTER_PREFERRED.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(master, nearest, slave);
    }

    @Test
    public void slave() {
        List<RedisNodeDescription> result = ReadFrom.SLAVE.select(getNodes());
        assertThat(result).hasSize(2).contains(nearest, slave);
    }

    @Test
    public void slavePreferred() {
        List<RedisNodeDescription> result = ReadFrom.SLAVE_PREFERRED.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(nearest, slave, master);
    }

    @Test
    public void nearest() {
        List<RedisNodeDescription> result = ReadFrom.NEAREST.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(nearest, master, slave);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfNull() {
        ReadFrom.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfUnknown() {
        ReadFrom.valueOf("unknown");
    }

    @Test
    public void valueOfNearest() {
        assertThat(ReadFrom.valueOf("nearest")).isEqualTo(ReadFrom.NEAREST);
    }

    @Test
    public void valueOfMaster() {
        assertThat(ReadFrom.valueOf("master")).isEqualTo(ReadFrom.MASTER);
    }

    @Test
    public void valueOfMasterPreferred() {
        assertThat(ReadFrom.valueOf("masterPreferred")).isEqualTo(ReadFrom.MASTER_PREFERRED);
    }

    @Test
    public void valueOfSlave() {
        assertThat(ReadFrom.valueOf("slave")).isEqualTo(ReadFrom.SLAVE);
    }

    @Test
    public void valueOfSlavePreferred() {
        assertThat(ReadFrom.valueOf("slavePreferred")).isEqualTo(ReadFrom.SLAVE_PREFERRED);
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
