package com.lambdaworks.redis.cluster;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RedisClusterNodeTest {
    @Test
    public void testEquality() throws Exception {
        RedisClusterNode node = new RedisClusterNode();

        assertTrue(node.equals(new RedisClusterNode()));
    }

    @Test
    public void testToString() throws Exception {
        RedisClusterNode node = new RedisClusterNode();

        assertThat(node.toString(), containsString(RedisClusterNode.class.getSimpleName()));
    }
}
