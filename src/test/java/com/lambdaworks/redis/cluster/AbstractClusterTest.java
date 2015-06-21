package com.lambdaworks.redis.cluster;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AbstractClusterTest extends AbstractTest {

    public static final String host = TestSettings.hostAddr();

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

    protected static RedisClusterClient clusterClient;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClusterClient() throws Exception {
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClusterClient() {
        clusterClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
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
