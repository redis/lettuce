/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.cluster.commands;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.StreamCommandIntegrationTests;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisStreamCommands} using Redis Cluster.
 *
 * @author Mark Paluch
 */
class StreamClusterCommandIntegrationTests extends StreamCommandIntegrationTests {

    @Inject
    StreamClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

    @Disabled("MULTI not available on Redis Cluster")
    @Override
    public void xreadTransactional() {
        super.xreadTransactional();
    }

    @Disabled("Required node reconfiguration with stream-node-max-entries")
    @Override
    public void xaddMinidLimit() {
        super.xaddMinidLimit();
    }

}
