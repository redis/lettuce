/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.cluster.commands;

import static io.lettuce.core.cluster.ClusterTestUtil.flushDatabaseOfAllNodes;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.GeoCommandTest;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class GeoClusterCommandTest extends GeoCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeAll
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());

        try (StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect()) {
            assumeTrue(RedisConditions.of(connection).hasCommand("GEOADD"));
        }
    }

    @AfterAll
    static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @BeforeEach
    public void openConnection() {
        redis = connect();
        flushDatabaseOfAllNodes(clusterConnection);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connect();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void geoaddInTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void geoaddMultiInTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void georadiusInTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void geodistInTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void georadiusWithArgsAndTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void georadiusbymemberWithArgsInTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void geoposInTransaction() {
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void geohashInTransaction() {
    }
}
