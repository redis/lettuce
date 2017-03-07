/*
 * Copyright 2011-2016 the original author or authors.
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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolvers;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class RoundRobinSocketAddressSupplierTest {

    private static RedisURI hap1 = new RedisURI("127.0.0.1", 1, 1, TimeUnit.SECONDS);
    private static RedisURI hap2 = new RedisURI("127.0.0.1", 2, 1, TimeUnit.SECONDS);
    private static RedisURI hap3 = new RedisURI("127.0.0.1", 3, 1, TimeUnit.SECONDS);
    private static RedisURI hap4 = new RedisURI("127.0.0.1", 4, 1, TimeUnit.SECONDS);
    private static Partitions partitions;

    @Mock
    private ClientResources clientResourcesMock;

    @BeforeClass
    public static void beforeClass() throws Exception {

        hap1.getResolvedAddress();
        hap2.getResolvedAddress();
        hap3.getResolvedAddress();
    }

    @Before
    public void before() throws Exception {

        when(clientResourcesMock.dnsResolver()).thenReturn(DnsResolvers.JVM_DEFAULT);

        partitions = new Partitions();
        partitions.addPartition(
                new RedisClusterNode(hap1, "1", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(
                new RedisClusterNode(hap2, "2", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(
                new RedisClusterNode(hap3, "3", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        partitions.updateCache();
    }

    @Test
    public void noOffset() throws Exception {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(partitions, redisClusterNodes -> redisClusterNodes,
                clientResourcesMock);

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap3.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());

        assertThat(sut.get()).isNotEqualTo(hap3.getResolvedAddress());
    }

    @Test
    public void partitionTableChangesNewNode() throws Exception {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(partitions, redisClusterNodes -> redisClusterNodes,
                clientResourcesMock);

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());

        partitions.add(
                new RedisClusterNode(hap4, "4", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap2.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap3.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap4.getResolvedAddress());
        assertThat(sut.get()).isEqualTo(hap1.getResolvedAddress());
    }

    @Test
    public void partitionTableChangesNodeRemoved() throws Exception {

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
