/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import reactor.test.StepVerifier;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.*;
import io.lettuce.test.LettuceExtension;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class CustomCodecIntegrationTests extends TestSupport {

    private final SecretKeySpec secretKey = new SecretKeySpec("1234567890123456".getBytes(), "AES");

    private final IvParameterSpec iv = new IvParameterSpec("1234567890123456".getBytes());

    // Creates a CryptoCipher instance with the transformation and properties.
    private final String transform = "AES/CBC/PKCS5Padding";

    CipherCodec.CipherSupplier encrypt = (CipherCodec.KeyDescriptor keyDescriptor) -> {

        Cipher cipher = Cipher.getInstance(transform);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher;
    };

    CipherCodec.CipherSupplier decrypt = (CipherCodec.KeyDescriptor keyDescriptor) -> {

        Cipher cipher = Cipher.getInstance(transform);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return cipher;
    };

    private final RedisClient client;

    @Inject
    CustomCodecIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @Test
    void testJavaSerializer() {
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
    void testJavaSerializerReactive() {

        StatefulRedisConnection<String, Object> redisConnection = client.connect(new SerializedObjectCodec());
        List<String> list = list("one", "two");

        StepVerifier.create(redisConnection.reactive().set(key, list, SetArgs.Builder.ex(1))).expectNext("OK").verifyComplete();
        redisConnection.close();
    }

    @Test
    void testDeflateCompressedJavaSerializer() {
        RedisCommands<String, Object> connection = client
                .connect(
                        CompressionCodec.valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.DEFLATE))
                .sync();
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);

        connection.getStatefulConnection().close();
    }

    @Test
    void testGzipompressedJavaSerializer() {
        RedisCommands<String, Object> connection = client
                .connect(CompressionCodec.valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.GZIP))
                .sync();
        List<String> list = list("one", "two");
        connection.set(key, list);
        assertThat(connection.get(key)).isEqualTo(list);

        connection.getStatefulConnection().close();
    }

    @Test
    void testEncryptedCodec() {

        RedisCommands<String, String> connection = client.connect(CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt))
                .sync();

        connection.set(key, "foobar");
        assertThat(connection.get(key)).isEqualTo("foobar");

        connection.getStatefulConnection().close();
    }

    @Test
    void testByteCodec() {
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
    void testByteBufferCodec() {

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

        connection.getStatefulConnection().close();
    }

    @Test
    void testComposedCodec() {

        RedisCodec<String, Object> composed = RedisCodec.of(StringCodec.ASCII, new SerializedObjectCodec());
        RedisCommands<String, Object> connection = client.connect(composed).sync();

        connection.set(key, new Person());

        List<String> keys = connection.keys(key);
        assertThat(keys).hasSize(1);

        assertThat(connection.get(key)).isInstanceOf(Person.class);

        connection.getStatefulConnection().close();
    }

    class SerializedObjectCodec implements RedisCodec<String, Object> {

        private Charset charset = StandardCharsets.UTF_8;

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

    static class Person implements Serializable {

    }

}
