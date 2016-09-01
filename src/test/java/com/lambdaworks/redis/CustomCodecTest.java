// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.Test;

import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.CompressionCodec;
import com.lambdaworks.redis.codec.RedisCodec;

public class CustomCodecTest extends AbstractRedisClientTest {

    @Test
    public void testJavaSerializer() throws Exception {
        StatefulRedisConnection<String, Object> redisConnection = client.connect(new SerializedObjectCodec());
        RedisCommands<String, Object> sync = redisConnection.sync();
        List<String> list = list("one", "two");
        sync.set(key, list);

        assertThat(sync.get(key)).isEqualTo(list);
        assertThat(sync.set(key, list)).isEqualTo("OK");
        assertThat(sync.set(key, list, SetArgs.Builder.ex(1))).isEqualTo("OK");

        redisConnection.close();
    }

    @Test
    public void testJavaSerializerReactive() throws Exception {
        StatefulRedisConnection<String, Object> redisConnection = client.connect(new SerializedObjectCodec());
        List<String> list = list("one", "two");

        TestSubscriber<String> subscriber = TestSubscriber.create();

        redisConnection.reactive().set(key, list, SetArgs.Builder.ex(1)).subscribe(subscriber);
        subscriber.awaitAndAssertNextValues("OK").assertComplete().assertNoError();

        redisConnection.close();
    }

    @Test
    public void testDeflateCompressedJavaSerializer() throws Exception {
        RedisCommands<String, Object> connection = client.connect(
                CompressionCodec.valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.DEFLATE)).sync();
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);

        connection.getStatefulConnection().close();
    }

    @Test
    public void testGzipompressedJavaSerializer() throws Exception {
        RedisCommands<String, Object> connection = client.connect(
                CompressionCodec.valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.GZIP)).sync();
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);

        connection.getStatefulConnection().close();
    }

    @Test
    public void testByteCodec() throws Exception {
        RedisCommands<byte[], byte[]> connection = client.connect(new ByteArrayCodec()).sync();
        String value = "üöäü+#";
        connection.set(key.getBytes(), value.getBytes());
        assertThat(connection.get(key.getBytes())).isEqualTo(value.getBytes());
        connection.set(key.getBytes(), null);
        assertThat(connection.get(key.getBytes())).isEqualTo(new byte[0]);

        List<byte[]> keys = connection.keys(key.getBytes());
        assertThat(keys).contains(key.getBytes());

        connection.getStatefulConnection().close();
    }

    @Test
    public void testExperimentalByteCodec() throws Exception {
        RedisCommands<byte[], byte[]> connection = client.connect(CommandArgs.ExperimentalByteArrayCodec.INSTANCE).sync();
        String value = "üöäü+#";
        connection.set(key.getBytes(), value.getBytes());
        assertThat(connection.get(key.getBytes())).isEqualTo(value.getBytes());
        connection.set(key.getBytes(), null);
        assertThat(connection.get(key.getBytes())).isEqualTo(new byte[0]);

        List<byte[]> keys = connection.keys(key.getBytes());
        assertThat(keys).contains(key.getBytes());
        connection.getStatefulConnection().close();
    }

    public class SerializedObjectCodec implements RedisCodec<String, Object> {
        private Charset charset = Charset.forName("UTF-8");

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return charset.decode(bytes).toString();
        }

        @Override
        public Object decodeValue(ByteBuffer bytes) {
            try {
                byte[] array = new byte[bytes.remaining()];
                bytes.get(array);
                ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(array));
                return is.readObject();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return charset.encode(key);
        }

        @Override
        public ByteBuffer encodeValue(Object value) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(bytes);
                os.writeObject(value);
                return ByteBuffer.wrap(bytes.toByteArray());
            } catch (IOException e) {
                return null;
            }
        }
    }
}
