/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.core.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link CipherCodec}.
 *
 * @author Mark Paluch
 */
class CipherCodecUnitTests {

    private final SecretKeySpec key = new SecretKeySpec("1234567890123456".getBytes(), "AES");

    private final IvParameterSpec iv = new IvParameterSpec("1234567890123456".getBytes());

    private final String transform = "AES/CBC/PKCS5Padding";

    CipherCodec.CipherSupplier encrypt = new CipherCodec.CipherSupplier() {

        @Override
        public Cipher get(CipherCodec.KeyDescriptor keyDescriptor) throws GeneralSecurityException {

            Cipher cipher = Cipher.getInstance(transform);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            return cipher;
        }

        @Override
        public CipherCodec.KeyDescriptor encryptionKey() {
            return CipherCodec.KeyDescriptor.create("foobar", 142);
        }

    };

    CipherCodec.CipherSupplier decrypt = (CipherCodec.KeyDescriptor keyDescriptor) -> {

        Cipher cipher = Cipher.getInstance(transform);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher;
    };

    @ParameterizedTest
    @MethodSource("cryptoTestValues")
    void shouldEncryptValue(CryptoTestArgs testArgs) {

        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);

        ByteBuffer encrypted = crypto.encodeValue(testArgs.content);
        assertThat(encrypted).isNotEqualTo(ByteBuffer.wrap(testArgs.content.getBytes()));

        assertThat(new String(encrypted.array())).startsWith("$foobar+142$");

        String decrypted = crypto.decodeValue(encrypted);
        assertThat(decrypted).isEqualTo(testArgs.content);
        assertThat(StringCodec.UTF8.encodeValue(testArgs.content)).isEqualTo(ByteBuffer.wrap(testArgs.content.getBytes()));
    }

    @ParameterizedTest
    @MethodSource("cryptoTestValues")
    void shouldEncryptValueToByteBuf(CryptoTestArgs testArgs) {

        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);

        ToByteBufEncoder<String, String> direct = (ToByteBufEncoder<String, String>) crypto;

        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        ByteBuf target = allocator.buffer();

        direct.encodeValue(testArgs.content, target);

        assertThat(target).isNotEqualTo(Unpooled.wrappedBuffer(testArgs.content.getBytes()));
        assertThat(target.toString(0, 20, StandardCharsets.US_ASCII)).startsWith("$foobar+142$");

        String result = crypto.decodeValue(target.nioBuffer());
        assertThat(result).isEqualTo(testArgs.content);
    }

    static List<CryptoTestArgs> cryptoTestValues() {

        StringBuilder hugeString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            hugeString.append(UUID.randomUUID().toString());
        }

        return Arrays.asList(new CryptoTestArgs("foobar"), new CryptoTestArgs(hugeString.toString()));
    }

    @Test
    void shouldDecryptValue() {

        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);

        ByteBuffer encrypted = ByteBuffer.wrap(
                new byte[] { 36, 43, 48, 36, -99, -39, 126, -106, -7, -88, 118, -74, 42, 98, 117, 81, 37, -124, 26, -88 });// crypto.encodeValue("foobar");

        String result = crypto.decodeValue(encrypted);
        assertThat(result).isEqualTo("foobar");
    }

    @Test
    void shouldRejectPlusAndDollarKeyNames() {

        assertThatThrownBy(() -> CipherCodec.KeyDescriptor.create("my+key")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CipherCodec.KeyDescriptor.create("my$key")).isInstanceOf(IllegalArgumentException.class);
    }

    static class CryptoTestArgs {

        private final int size;

        private final String content;

        public CryptoTestArgs(String content) {
            this.size = content.length();
            this.content = content;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [size=").append(size);
            sb.append(']');
            return sb.toString();
        }

    }

}
