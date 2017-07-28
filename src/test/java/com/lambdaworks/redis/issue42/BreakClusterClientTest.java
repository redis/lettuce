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
package com.lambdaworks.redis.issue42;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;

import java.util.concurrent.TimeUnit;

import org.junit.*;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.TestClientResources;
import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.cluster.ClusterRule;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;

@SlowTests
@Ignore("Run me manually")
public class BreakClusterClientTest extends BreakClientBase {
    public static final String host = TestSettings.hostAddr();
    public static final int port1 = 7379;
    public static final int port2 = 7380;
    public static final int port3 = 7381;
    public static final int port4 = 7382;

    private static RedisClusterClient clusterClient;
    private RedisClusterCommands<String, String> clusterConnection;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClient() {
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, port1).withTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build());
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void setUp() throws Exception {
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterRule.isStable();
            }
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

        clusterConnection = clusterClient.connectCluster(this.slowCodec);

    }

    @After
    public void tearDown() throws Exception {
        clusterConnection.close();
    }

    @Test
    public void testStandAlone() throws Exception {
        testSingle(clusterConnection);
    }

    @Test
    public void testLooping() throws Exception {
        testLoop(clusterConnection);
    }

}
