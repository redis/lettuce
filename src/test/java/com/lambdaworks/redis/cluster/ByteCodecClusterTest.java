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
