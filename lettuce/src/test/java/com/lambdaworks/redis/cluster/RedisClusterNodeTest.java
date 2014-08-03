package com.lambdaworks.redis.cluster;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class RedisClusterNodeTest {
    @Test
    public void testEquality() throws Exception {
        RedisClusterNode node = new RedisClusterNode();

        assertThat(node).isEqualTo(new RedisClusterNode());
    }

    @Test
    public void testToString() throws Exception {
        RedisClusterNode node = new RedisClusterNode();

        assertThat(node.toString()).contains(RedisClusterNode.class.getSimpleName());
    }
}
