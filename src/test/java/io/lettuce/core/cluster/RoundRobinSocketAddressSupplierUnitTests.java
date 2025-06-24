/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
 * Unit tests for {@link RoundRobinSocketAddressSupplier}.
 *
 * @author Mark Paluch
 * @author Christian Lang
 * @author Hari Mani
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class RoundRobinSocketAddressSupplierUnitTests {

    private static final RedisURI hap1 = new RedisURI("127.0.0.1", 1, Duration.ofSeconds(1));

    private static final RedisURI hap2 = new RedisURI("127.0.0.1", 2, Duration.ofSeconds(1));

    private static final RedisURI hap3 = new RedisURI("127.0.0.1", 3, Duration.ofSeconds(1));

    private static final RedisURI hap4 = new RedisURI("127.0.0.1", 4, Duration.ofSeconds(1));

    private static final RedisURI hap5 = new RedisURI("127.0.0.0", 5, Duration.ofSeconds(1));

    private static final InetSocketAddress addr1 = InetSocketAddress.createUnresolved(hap1.getHost(), hap1.getPort());

    private static final InetSocketAddress addr2 = InetSocketAddress.createUnresolved(hap2.getHost(), hap2.getPort());

    private static final InetSocketAddress addr3 = InetSocketAddress.createUnresolved(hap3.getHost(), hap3.getPort());

    private static final InetSocketAddress addr4 = InetSocketAddress.createUnresolved(hap4.getHost(), hap4.getPort());

    private static final InetSocketAddress addr5 = InetSocketAddress.createUnresolved(hap5.getHost(), hap5.getPort());

    private static Partitions partitions;

    @Mock
    private ClientResources clientResourcesMock;

    @BeforeEach
    void before() {

        when(clientResourcesMock.socketAddressResolver()).thenReturn(SocketAddressResolver.create(DnsResolvers.UNRESOLVED));

        partitions = new Partitions();
        partitions.addPartition(new RedisClusterNode(hap1, "1", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(new RedisClusterNode(hap2, "2", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));
        partitions.addPartition(new RedisClusterNode(hap3, "3", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        partitions.updateCache();
    }

    @Test
    void noOffset() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(() -> partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(addr1);
        assertThat(sut.get()).isEqualTo(addr2);
        assertThat(sut.get()).isEqualTo(addr3);
        assertThat(sut.get()).isEqualTo(addr1);

        assertThat(sut.get()).isNotEqualTo(addr3);
    }

    @Test
    void nodeIPChanges() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(() -> partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(addr1);

        assertThat(partitions.remove(new RedisClusterNode(hap1, "2", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>())))
                .isTrue();
        assertThat(partitions.add(new RedisClusterNode(hap5, "2", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>())))
                .isTrue();

        assertThat(sut.get()).isEqualTo(addr1);
        assertThat(sut.get()).isEqualTo(addr3);
        assertThat(sut.get()).isEqualTo(addr5);
    }

    @Test
    void partitionTableChangesNewNode() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(() -> partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(addr1);
        assertThat(sut.get()).isEqualTo(addr2);

        partitions.add(new RedisClusterNode(hap4, "4", true, "", 0, 0, 0, new ArrayList<>(), new HashSet<>()));

        assertThat(sut.get()).isEqualTo(addr1);
        assertThat(sut.get()).isEqualTo(addr2);
        assertThat(sut.get()).isEqualTo(addr3);
        assertThat(sut.get()).isEqualTo(addr4);
        assertThat(sut.get()).isEqualTo(addr1);
    }

    @Test
    void partitionTableChangesNodeRemoved() {

        RoundRobinSocketAddressSupplier sut = new RoundRobinSocketAddressSupplier(() -> partitions,
                redisClusterNodes -> redisClusterNodes, clientResourcesMock);

        assertThat(sut.get()).isEqualTo(addr1);
        assertThat(sut.get()).isEqualTo(addr2);

        partitions.remove(partitions.getPartition(2));

        assertThat(sut.get()).isEqualTo(addr1);
        assertThat(sut.get()).isEqualTo(addr2);
        assertThat(sut.get()).isEqualTo(addr1);
    }

}
