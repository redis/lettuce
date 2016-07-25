package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
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
}
