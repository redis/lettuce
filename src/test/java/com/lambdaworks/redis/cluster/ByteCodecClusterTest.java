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
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;

/**
 * @author Mark Paluch
 */
public class ByteCodecClusterTest extends AbstractClusterTest {

    @Test
    public void testByteCodec() throws Exception {

        StatefulRedisClusterConnection<byte[], byte[]> connection = clusterClient.connect(new ByteArrayCodec());

        connection.sync().set(key.getBytes(), value.getBytes());
        assertThat(connection.sync().get(key.getBytes())).isEqualTo(value.getBytes());
    }

    @Test
    public void deprecatedTestByteCodec() throws Exception {

        RedisAdvancedClusterCommands<byte[], byte[]> commands = clusterClient.connectCluster(new ByteArrayCodec());

        commands.set(key.getBytes(), value.getBytes());
        assertThat(commands.get(key.getBytes())).isEqualTo(value.getBytes());
    }

    @Test
    public void deprecatedTestAsyncByteCodec() throws Exception {

        RedisAdvancedClusterAsyncCommands<byte[], byte[]> commands = clusterClient.connectClusterAsync(new ByteArrayCodec());

        commands.set(key.getBytes(), value.getBytes()).get();
        assertThat(commands.get(key.getBytes()).get()).isEqualTo(value.getBytes());
    }
}
