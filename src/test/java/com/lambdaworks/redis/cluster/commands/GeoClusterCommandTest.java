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
package com.lambdaworks.redis.cluster.commands;

import static com.lambdaworks.redis.cluster.ClusterTestUtil.flushDatabaseOfAllNodes;
import static org.junit.Assume.assumeTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.lambdaworks.RedisConditions;
import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.commands.GeoCommandTest;

/**
 * @author Mark Paluch
 */
public class GeoClusterCommandTest extends GeoCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());

        try (StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect()) {
            assumeTrue(RedisConditions.of(connection).hasCommand("GEOADD"));
        }
    }

    @AfterClass
    public static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @Before
    public void openConnection() throws Exception {
        redis = connect();
        flushDatabaseOfAllNodes(clusterConnection);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connectCluster().getStatefulConnection();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geoaddWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geoaddMultiWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void georadiusWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geodistWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void georadiusWithArgsAndTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void georadiusbymemberWithArgsAndTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geoposWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geohashWithTransaction() throws Exception {
    }
}
