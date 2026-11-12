/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.SetCommandIntegrationTests;

/**
 * Integration tests for the set commands, executed over a Redis Cluster connection. The multi-key tests of the base class use
 * keys in different slots and are disabled here; the set member expiration commands are single-key.
 */
@Tag(INTEGRATION_TEST)
public class SetClusterCommandIntegrationTests extends SetCommandIntegrationTests {

    @Inject
    public SetClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sdiff() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sdiffStreaming() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sdiffcard() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sdiffcardWithLimit() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sdiffstore() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sinter() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sintercard() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sinterStreaming() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sinterstore() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void smove() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sunion() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sunionStreaming() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sunioncard() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sunioncardApprox() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sunioncardWithLimit() {
    }

    @Disabled("Keys of the base test hash to different slots (CROSSSLOT on Redis Cluster)")
    @Override
    public void sunionstore() {
    }

}
