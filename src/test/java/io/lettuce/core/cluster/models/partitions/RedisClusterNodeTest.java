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
package io.lettuce.core.cluster.models.partitions;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import io.lettuce.core.RedisURI;

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
