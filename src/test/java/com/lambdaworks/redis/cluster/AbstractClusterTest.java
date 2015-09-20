package com.lambdaworks.redis.cluster;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.resource.ClientResources;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.google.common.collect.ImmutableList;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AbstractClusterTest {

    public static final String host = TestSettings.hostAddr();
    public static final int port1 = 7379;
    public static final int port2 = 7380;
    public static final int port3 = 7381;
    public static final int port4 = 7382;

    protected static RedisClusterClient clusterClient;

    public static final int SLOT_A = SlotHash.getSlot("a".getBytes());
    public static final int SLOT_B = SlotHash.getSlot("b".getBytes());

    public static final String KEY_A = "a";
    public static final String KEY_B = "b";

    protected String key = "key";
    protected String value = "value";

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    protected static ClientResources resources = TestClientResources.create();

    @BeforeClass
    public static void setupClusterClient() throws Exception {
        clusterClient = RedisClusterClient.create(resources, ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClusterClient() {
        FastShutdown.shutdown(clusterClient);
    }

}