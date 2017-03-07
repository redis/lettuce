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

import io.lettuce.TestClientResources;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import io.lettuce.core.AbstractTest;
import io.lettuce.core.FastShutdown;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSettings;
import io.lettuce.core.internal.LettuceLists;

/**
 * @author Mark Paluch
 */
public class AbstractClusterTest extends AbstractTest {

    public static final String host = TestSettings.hostAddr();

    public static final int SLOT_A = SlotHash.getSlot("a".getBytes());
    public static final int SLOT_B = SlotHash.getSlot("b".getBytes());


    // default test cluster 2 masters + 2 slaves
    public static final int port1 = 7379;
    public static final int port2 = port1 + 1;
    public static final int port3 = port1 + 2;
    public static final int port4 = port1 + 3;

    // master+slave or master+master
    public static final int port5 = port1 + 4;
    public static final int port6 = port1 + 5;

    // auth cluster
    public static final int port7 = port1 + 6;
    public static final String KEY_A = "a";
    public static final String KEY_B = "b";
    public static final String KEY_D = "d";

    protected static RedisClusterClient clusterClient;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClusterClient() throws Exception {
        clusterClient = RedisClusterClient.create(
                TestClientResources.get(), LettuceLists.unmodifiableList(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClusterClient() {
        FastShutdown.shutdown(clusterClient);
    }

    public static int[] createSlots(int from, int to) {
        int[] result = new int[to - from];
        int counter = 0;
        for (int i = from; i < to; i++) {
            result[counter++] = i;

        }
        return result;
    }

}
