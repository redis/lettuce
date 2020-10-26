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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * @author Mark Paluch
 * @author Ryosuke Hasebe
 * @author Omer Cilingir
 */
class ReadFromUnitTests {

    private Partitions sut = new Partitions();
    private RedisClusterNode nearest = new RedisClusterNode();
    private RedisClusterNode master = new RedisClusterNode();
    private RedisClusterNode replica = new RedisClusterNode();

    @BeforeEach
    void before() {

        master.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.UPSTREAM));
        nearest.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.REPLICA));
        replica.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.REPLICA));

        sut.addPartition(nearest);
        sut.addPartition(master);
        sut.addPartition(replica);
    }

    @Test
    void master() {
        List<RedisNodeDescription> result = ReadFrom.UPSTREAM.select(getNodes());
        assertThat(result).hasSize(1).containsOnly(master);
    }

    @Test
    void masterPreferred() {
        List<RedisNodeDescription> result = ReadFrom.UPSTREAM_PREFERRED.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(master, nearest, replica);
    }

    @Test
    void replica() {
        List<RedisNodeDescription> result = ReadFrom.REPLICA.select(getNodes());
        assertThat(result).hasSize(2).contains(nearest, replica);
    }

    @Test
    void replicaPreferred() {
        List<RedisNodeDescription> result = ReadFrom.REPLICA_PREFERRED.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(nearest, replica, master);
    }

    @Test
    void nearest() {
        List<RedisNodeDescription> result = ReadFrom.NEAREST.select(getNodes());
        assertThat(result).hasSize(3).containsExactly(nearest, master, replica);
    }

    @Test
    void anyReplica() {
        List<RedisNodeDescription> result = ReadFrom.ANY_REPLICA.select(getNodes());
        assertThat(result).hasSize(2).containsExactly(nearest, replica);
    }

    @Test
    void valueOfNull() {
        assertThatThrownBy(() -> ReadFrom.valueOf(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valueOfUnknown() {
        assertThatThrownBy(() -> ReadFrom.valueOf("unknown")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valueOfNearest() {
        assertThat(ReadFrom.valueOf("nearest")).isEqualTo(ReadFrom.NEAREST);
    }

    @Test
    void valueOfMaster() {
        assertThat(ReadFrom.valueOf("master")).isEqualTo(ReadFrom.UPSTREAM);
    }

    @Test
    void valueOfMasterPreferred() {
        assertThat(ReadFrom.valueOf("masterPreferred")).isEqualTo(ReadFrom.UPSTREAM_PREFERRED);
    }

    @Test
    void valueOfSlave() {
        assertThat(ReadFrom.valueOf("slave")).isEqualTo(ReadFrom.REPLICA);
    }

    @Test
    void valueOfSlavePreferred() {
        assertThat(ReadFrom.valueOf("slavePreferred")).isEqualTo(ReadFrom.REPLICA_PREFERRED);
    }

    @Test
    void valueOfAnyReplica() {
        assertThat(ReadFrom.valueOf("anyReplica")).isEqualTo(ReadFrom.ANY_REPLICA);
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
