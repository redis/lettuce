package com.lambdaworks.redis.cluster.models.partitions;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import com.lambdaworks.redis.RedisURI;

public class RedisClusterNodeTest {
    @Test
    public void testEquality() throws Exception {
        RedisClusterNode node = new RedisClusterNode();

        assertThat(node).isEqualTo(new RedisClusterNode());
        assertThat(node.hashCode()).isEqualTo(new RedisClusterNode().hashCode());

        node.setUri(new RedisURI());
        assertThat(node.hashCode()).isNotEqualTo(new RedisClusterNode());

    }

    @Test
    public void testToString() throws Exception {
        RedisClusterNode node = new RedisClusterNode();

        assertThat(node.toString()).contains(RedisClusterNode.class.getSimpleName());
    }
}
