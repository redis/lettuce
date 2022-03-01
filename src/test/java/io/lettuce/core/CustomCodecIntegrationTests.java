/*
 * Copyright 2011-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.CipherCodec;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
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
        StatefulRedisConnection<String, Object> connection = client
                .connect(
                        CompressionCodec.valueCompressor(new SerializedObjectCodec(),
                                CompressionCodec.CompressionType.DEFLATE));
        RedisCommands<String, Object> sync = connection
                .sync();
        List<String> list = list("one", "two");
        sync.set(key, list);
        assertThat(sync.get(key)).isEqualTo(list);

        connection.close();
    }

    @Test
    void testGzipompressedJavaSerializer() {
        StatefulRedisConnection<String, Object> connection = client
                .connect(CompressionCodec.valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.GZIP));
        RedisCommands<String, Object> sync = connection.sync();
        List<String> list = list("one", "two");
        sync.set(key, list);
        assertThat(sync.get(key)).isEqualTo(list);

        connection.close();
    }

    @Test
    void testEncryptedCodec() {

        StatefulRedisConnection<String, String> connection = client
                .connect(CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt));
        RedisCommands<String, String> sync = connection
                .sync();

        sync.set(key, "foobar");
        assertThat(sync.get(key)).isEqualTo("foobar");

        connection.close();
    }

    @Test
    void testByteCodec() {
        StatefulRedisConnection<byte[], byte[]> connection = client.connect(new ByteArrayCodec());
        RedisCommands<byte[], byte[]> sync = connection.sync();
        String value = "üöäü+#";
        sync.set(key.getBytes(), value.getBytes());
        assertThat(sync.get(key.getBytes())).isEqualTo(value.getBytes());
        sync.set(key.getBytes(), null);
        assertThat(sync.get(key.getBytes())).isEqualTo(new byte[0]);

        List<byte[]> keys = sync.keys(key.getBytes());
        assertThat(keys).contains(key.getBytes());

        connection.close();
    }

    @Test
    void testByteBufferCodec() {

        StatefulRedisConnection<ByteBuffer, ByteBuffer> connection = client.connect(new ByteBufferCodec());
        RedisCommands<ByteBuffer, ByteBuffer> sync = connection.sync();
        String value = "üöäü+#";

        ByteBuffer wrap = ByteBuffer.wrap(value.getBytes());

        sync.set(wrap, wrap);

        List<ByteBuffer> keys = sync.keys(wrap);
        assertThat(keys).hasSize(1);
        ByteBuffer byteBuffer = keys.get(0);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);

        assertThat(bytes).isEqualTo(value.getBytes());

        connection.close();
    }

    @Test
    void testComposedCodec() {

        RedisCodec<String, Object> composed = RedisCodec.of(StringCodec.ASCII, new SerializedObjectCodec());
        StatefulRedisConnection<String, Object> connection = client.connect(composed);
        RedisCommands<String, Object> sync = connection.sync();

        sync.set(key, new Person());

        List<String> keys = sync.keys(key);
        assertThat(keys).hasSize(1);

        assertThat(sync.get(key)).isInstanceOf(Person.class);

        connection.close();
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
