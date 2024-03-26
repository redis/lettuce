package io.lettuce.core.cluster.commands;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.GeoCommandIntegrationTests;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisGeoCommands} using Redis Cluster.
 *
 * @author Mark Paluch
 */
class GeoClusterCommandIntegrationTests extends GeoCommandIntegrationTests {

    @Inject
    GeoClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> clusterConnection) {
        super(ClusterTestUtil.redisCommandsOverCluster(clusterConnection));
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
