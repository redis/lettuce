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
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.CompressionCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandArgs;

import rx.observers.TestSubscriber;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
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
    public void testJavaSerializerRx() throws Exception {
        StatefulRedisConnection<String, Object> redisConnection = client.connect(new SerializedObjectCodec());
        List<String> list = list("one", "two");

        TestSubscriber<String> subscriber = TestSubscriber.create();

        redisConnection.reactive().set(key, list, SetArgs.Builder.ex(1)).subscribe(subscriber);
        subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
        subscriber.assertCompleted();
        subscriber.assertValue("OK");

        redisConnection.close();
    }

    @Test
    public void testDeflateCompressedJavaSerializer() throws Exception {
        RedisCommands<String, Object> connection = client
                .connect(
                        CompressionCodec
                                .valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.DEFLATE))
                .sync();
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);

        connection.close();
    }

    @Test
    public void testGzipompressedJavaSerializer() throws Exception {
        RedisCommands<String, Object> connection = client
                .connect(CompressionCodec
                        .valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.GZIP))
                .sync();
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);

        connection.close();
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

        connection.close();
    }

    @Test
    public void testByteBufferCodec() throws Exception {

        RedisCommands<ByteBuffer, ByteBuffer> connection = client.connect(new ByteBufferCodec()).sync();
        String value = "üöäü+#";

        ByteBuffer wrap = ByteBuffer.wrap(value.getBytes());

        connection.set(wrap, wrap);

        List<ByteBuffer> keys = connection.keys(wrap);
        assertThat(keys).hasSize(1);
        ByteBuffer byteBuffer = keys.get(0);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);

        assertThat(bytes).isEqualTo(value.getBytes());

        connection.close();
    }

    @Test
    public void testExperimentalByteCodec() throws Exception {

        RedisCommands<byte[], byte[]> connection = client.connect(CommandArgs.ExperimentalByteArrayCodec.INSTANCE)
                .sync();
        String value = "üöäü+#";
        connection.set(key.getBytes(), value.getBytes());
        assertThat(connection.get(key.getBytes())).isEqualTo(value.getBytes());
        connection.set(key.getBytes(), null);
        assertThat(connection.get(key.getBytes())).isEqualTo(new byte[0]);

        List<byte[]> keys = connection.keys(key.getBytes());
        assertThat(keys).contains(key.getBytes());
        connection.close();
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

    public class ByteBufferCodec implements RedisCodec<ByteBuffer, ByteBuffer> {

        @Override
        public ByteBuffer decodeKey(ByteBuffer bytes) {

            ByteBuffer decoupled = ByteBuffer.allocate(bytes.remaining());
            decoupled.put(bytes);
            return (ByteBuffer) decoupled.flip();
        }

        @Override
        public ByteBuffer decodeValue(ByteBuffer bytes) {

            ByteBuffer decoupled = ByteBuffer.allocate(bytes.remaining());
            decoupled.put(bytes);
            return (ByteBuffer) decoupled.flip();
        }

        @Override
        public ByteBuffer encodeKey(ByteBuffer key) {
            return key.asReadOnlyBuffer();
        }

        @Override
        public ByteBuffer encodeValue(ByteBuffer value) {
            return value.asReadOnlyBuffer();
        }
    }
}
