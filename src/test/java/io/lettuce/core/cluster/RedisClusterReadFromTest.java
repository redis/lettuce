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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.*;

import io.lettuce.TestClientResources;
import io.lettuce.core.FastShutdown;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

@SuppressWarnings("unchecked")
public class RedisClusterReadFromTest extends AbstractClusterTest {

    protected RedisAdvancedClusterCommands<String, String> sync;
    protected StatefulRedisClusterConnection<String, String> connection;

    @BeforeClass
    public static void setupClient() throws Exception {

        setupClusterClient();
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                Collections.singletonList(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClient() {

        shutdownClusterClient();
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() {

        clusterRule.getClusterClient().reloadPartitions();
        connection = clusterClient.connect();
        sync = connection.sync();
    }

    @After
    public void after() {
        sync.getStatefulConnection().close();
    }

    @Test
    public void defaultTest() {
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.MASTER);
    }

    @Test
    public void readWriteMaster() {

        connection.setReadFrom(ReadFrom.MASTER);

        sync.set(key, value);
        assertThat(sync.get(key)).isEqualTo(value);
    }

    @Test
    public void readWriteMasterPreferred() {

        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);

        sync.set(key, value);
        assertThat(sync.get(key)).isEqualTo(value);
    }

    @Test
    public void readWriteSlave() {

        connection.setReadFrom(ReadFrom.SLAVE);

        sync.set(key, "value1");

        connection.getConnection(host, port2).sync().waitForReplication(1, 1000);
        assertThat(sync.get(key)).isEqualTo("value1");
    }

    @Test
    public void readWriteSlavePreferred() {

        connection.setReadFrom(ReadFrom.SLAVE_PREFERRED);

        sync.set(key, "value1");

        connection.getConnection(host, port2).sync().waitForReplication(1, 1000);
        assertThat(sync.get(key)).isEqualTo("value1");
    }

    @Test
    public void readWriteNearest() {

        connection.setReadFrom(ReadFrom.NEAREST);

        sync.set(key, "value1");

        connection.getConnection(host, port2).sync().waitForReplication(1, 1000);
        assertThat(sync.get(key)).isEqualTo("value1");
    }
}
