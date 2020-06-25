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
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.SocketAddressResolver;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class RoundRobinSocketAddressSupplierUnitTests {

    private static RedisURI hap1 = new RedisURI("127.0.0.1", 1, Duration.ofSeconds(1));

    private static RedisURI hap2 = new RedisURI("127.0.0.1", 2, Duration.ofSeconds(1));

    private static RedisURI hap3 = new RedisURI("127.0.0.1", 3, Duration.ofSeconds(1));

    private static RedisURI hap4 = new RedisURI("127.0.0.1", 4, Duration.ofSeconds(1));

    private static Partitions partitions;

    @Mock
    private ClientResources clientResourcesMock;

    @BeforeAll
    static void beforeClass() {

        hap1.getResolvedAddress();
        hap2.getResolvedAddress();
        hap3.getResolvedAddress();
    }

    @BeforeEach
    void before() {

        when(clientResourcesMock.socketAddressResolver()).thenReturn(SocketAddressResolver.create(DnsResolvers.JVM_DEFAULT));

        partitions = new Partitions();
        partitions.addPartition(new RedisClusterNode(hap1, "1", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(new RedisClusterNode(hap2, "2", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(new RedisClusterNode(hap3, "3", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        partitions.updateCache();
    }

    @Test
    void noOffset() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap3.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());

        assertThat(sut.get()).isNotEqualTo(hap3.getResolvedAddress());
    }

    @Test
    void partitionTableChangesNewNode() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());

        partitions.add(new RedisClusterNode(hap4, "4", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap3.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap4.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
    }

    @Test
    void partitionTableChangesNodeRemoved() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());

        partitions.remove(partitions.getPartition(2));

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
    }

}
