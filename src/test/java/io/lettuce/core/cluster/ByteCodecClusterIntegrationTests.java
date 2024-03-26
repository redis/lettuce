package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.TestSupport;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class ByteCodecClusterIntegrationTests extends TestSupport {

    @Test
    @Inject
    void testByteCodec(RedisClusterClient clusterClient) {

        StatefulRedisClusterConnection<byte[], byte[]> connection = clusterClient.connect(new ByteArrayCodec());

        connection.sync().set(key.getBytes(), value.getBytes());
        assertThat(connection.sync().get(key.getBytes())).isEqualTo(value.getBytes());
    }
}
