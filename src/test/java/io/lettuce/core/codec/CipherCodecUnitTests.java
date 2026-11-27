package io.lettuce.core.codec;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Tag;
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
 * @author Armen Petrosyan
 */
@Tag(UNIT_TEST)
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

    @Test
    void shouldEncryptValueToByteBufUsingValueEncoding() {

        RedisCodec<String, String> crypto = CipherCodec.forValues(new PrefixCodec(), encrypt, decrypt);

        ToByteBufEncoder<String, String> direct = (ToByteBufEncoder<String, String>) crypto;

        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        ByteBuf target = allocator.buffer();
        ByteBuf keyTarget = allocator.buffer();

        try {
            direct.encodeValue("foobar", target);
            direct.encodeKey("foobar", keyTarget);

            assertThat(crypto.decodeValue(target.nioBuffer())).isEqualTo("V:foobar");
            assertThat(crypto.decodeValue(crypto.encodeValue("foobar"))).isEqualTo("V:foobar");
            assertThat(crypto.decodeKey(keyTarget.nioBuffer())).isEqualTo("K:foobar");
        } finally {
            target.release();
            keyTarget.release();
        }
    }

    @Test
    void shouldStoreInitializationVectorAlongsideCiphertext() {

        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, iv.getIV().length);

        ByteBuffer encrypted = crypto.encodeValue("foobar");
        CipherCodec.KeyDescriptor.from(encrypted);
        byte[] storedIv = new byte[iv.getIV().length];
        encrypted.get(storedIv);

        assertThat(storedIv).isEqualTo(iv.getIV());
    }

    @ParameterizedTest
    @MethodSource("initializationVectorTestValues")
    void shouldRoundTripWithFreshInitializationVectors(CryptoTestArgs testArgs) {

        CipherCodec.CipherSupplier gcmEncrypt = descriptor -> {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        };
        CipherCodec.CipherSupplier gcmDecrypt = descriptor -> {
            assertThat(descriptor.getName()).isEmpty();
            assertThat(descriptor.getVersion()).isZero();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, descriptor.getInitializationVector()));
            return cipher;
        };
        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, gcmEncrypt, gcmDecrypt, 12);

        ByteBuffer first = crypto.encodeValue(testArgs.content);
        ByteBuffer second = crypto.encodeValue(testArgs.content);
        assertThat(first).isNotEqualTo(second);
        assertThat(crypto.decodeValue(first.duplicate())).isEqualTo(testArgs.content);
        assertThat(crypto.decodeValue(second)).isEqualTo(testArgs.content);

        ByteBuf target = Unpooled.buffer();
        try {
            ((ToByteBufEncoder<String, String>) crypto).encodeValue(testArgs.content, target);
            ByteBuffer encoded = target.nioBuffer();
            assertThat(encoded).isNotEqualTo(first);
            assertThat(crypto.decodeValue(encoded)).isEqualTo(testArgs.content);
        } finally {
            target.release();
        }

        first.put(first.limit() - 1, (byte) (first.get(first.limit() - 1) ^ 1));
        assertThatThrownBy(() -> crypto.decodeValue(first)).isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(AEADBadTagException.class);
    }

    static Stream<CryptoTestArgs> initializationVectorTestValues() {
        return Stream.concat(Stream.of(new CryptoTestArgs("")), cryptoTestValues().stream());
    }

    @Test
    void shouldDecryptUsingStoredInitializationVectorAndKeyDescriptor() {

        CipherCodec.CipherSupplier storedIvDecrypt = descriptor -> {
            assertThat(descriptor.getName()).isEqualTo("foobar");
            assertThat(descriptor.getVersion()).isEqualTo(142);
            byte[] storedIv = descriptor.getInitializationVector();
            assertThat(storedIv).isEqualTo(iv.getIV());
            storedIv[0] ^= 1;
            assertThat(descriptor.getInitializationVector()).isEqualTo(iv.getIV());
            Cipher cipher = Cipher.getInstance(transform);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(descriptor.getInitializationVector()));
            return cipher;
        };
        RedisCodec<String, String> crypto = CipherCodec.forValues(new PrefixCodec(), encrypt, storedIvDecrypt, 16);

        ByteBuffer encoded = crypto.encodeValue("foobar");
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(encoded.remaining() + 3);
        directBuffer.position(3);
        directBuffer.put(encoded).flip();
        directBuffer.position(3);
        assertThat(crypto.decodeValue(directBuffer.slice())).isEqualTo("V:foobar");

        ByteBuf target = Unpooled.buffer();
        try {
            ((ToByteBufEncoder<String, String>) crypto).encodeValue("foobar", target);
            assertThat(crypto.decodeValue(target.nioBuffer())).isEqualTo("V:foobar");
        } finally {
            target.release();
        }
    }

    @Test
    void shouldRejectInvalidInitializationVectorConfiguration() {

        assertThatThrownBy(() -> CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, -1))
                .isInstanceOf(IllegalArgumentException.class);
        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, 12);
        assertThatThrownBy(() -> crypto.encodeValue("foobar")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configured length");
        ByteBuf target = Unpooled.buffer();
        try {
            assertThatThrownBy(() -> ((ToByteBufEncoder<String, String>) crypto).encodeValue("foobar", target))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("configured length");
        } finally {
            target.release();
        }
    }

    @Test
    void shouldRejectTruncatedInitializationVector() {

        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, 16);
        ByteBuffer truncated = ByteBuffer.wrap("$foobar+142$short".getBytes(StandardCharsets.US_ASCII));
        assertThatThrownBy(() -> crypto.decodeValue(truncated)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Truncated initialization vector");
        assertThat(CipherCodec.KeyDescriptor.unnamed().getInitializationVector()).isEmpty();
    }

    @Test
    void shouldRoundTripEmptyValueWithoutStoredInitializationVector() {

        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);
        assertThat(crypto.decodeValue(crypto.encodeValue(""))).isEmpty();
        ByteBuf target = Unpooled.buffer();
        try {
            ((ToByteBufEncoder<String, String>) crypto).encodeValue("", target);
            assertThat(crypto.decodeValue(target.nioBuffer())).isEmpty();
        } finally {
            target.release();
        }
    }

    @ParameterizedTest
    @MethodSource("initializationVectorTestValues")
    void shouldReadBothFormatsWhileSwitchingWriters(CryptoTestArgs testArgs) {

        CipherCodec.CipherSupplier storedIvEncrypt = new CipherCodec.CipherSupplier() {

            @Override
            public Cipher get(CipherCodec.KeyDescriptor descriptor) throws GeneralSecurityException {
                Cipher cipher = Cipher.getInstance(transform);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                return cipher;
            }

            @Override
            public CipherCodec.KeyDescriptor encryptionKey() {
                return CipherCodec.KeyDescriptor.create("foobar", 143);
            }

        };
        CipherCodec.CipherSupplier mixedDecrypt = descriptor -> {
            assertThat(descriptor.getName()).isEqualTo("foobar");
            assertThat(descriptor.getVersion()).isIn(142, 143);
            Cipher cipher = Cipher.getInstance(transform);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    descriptor.getVersion() == 142 ? iv : new IvParameterSpec(descriptor.getInitializationVector()));
            return cipher;
        };
        RedisCodec<String, String> legacy = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);
        RedisCodec<String, String> reader = CipherCodec.forValues(StringCodec.UTF8, encrypt, mixedDecrypt,
                descriptor -> descriptor.getVersion() == 142 ? 0 : 16);
        RedisCodec<String, String> writer = CipherCodec.forValues(StringCodec.UTF8, storedIvEncrypt, mixedDecrypt,
                descriptor -> descriptor.getVersion() == 142 ? 0 : 16);

        for (RedisCodec<String, String> codec : Arrays.asList(legacy, reader, writer)) {
            ByteBuffer encoded = codec.encodeValue(testArgs.content);
            assertThat(reader.decodeValue(encoded.duplicate())).isEqualTo(testArgs.content);
            assertThat(writer.decodeValue(encoded.duplicate())).isEqualTo(testArgs.content);
            if (codec != writer) {
                assertThat(legacy.decodeValue(encoded.duplicate())).isEqualTo(testArgs.content);
            }
            ByteBuf target = Unpooled.buffer();
            try {
                ((ToByteBufEncoder<String, String>) codec).encodeValue(testArgs.content, target);
                assertThat(reader.decodeValue(target.nioBuffer())).isEqualTo(testArgs.content);
                assertThat(writer.decodeValue(target.nioBuffer())).isEqualTo(testArgs.content);
                if (codec != writer) {
                    assertThat(legacy.decodeValue(target.nioBuffer())).isEqualTo(testArgs.content);
                }
            } finally {
                target.release();
            }
        }
    }

    @Test
    void shouldRejectInvalidInitializationVectorLengthSelector() {

        assertThatThrownBy(() -> CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, null))
                .isInstanceOf(IllegalArgumentException.class);
        RedisCodec<String, String> crypto = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt, descriptor -> -1);
        assertThatThrownBy(() -> crypto.encodeValue("foobar")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
        ByteBuf target = Unpooled.buffer();
        try {
            assertThatThrownBy(() -> ((ToByteBufEncoder<String, String>) crypto).encodeValue("foobar", target))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must not be negative");
        } finally {
            target.release();
        }
        RedisCodec<String, String> legacy = CipherCodec.forValues(StringCodec.UTF8, encrypt, decrypt);
        assertThatThrownBy(() -> crypto.decodeValue(legacy.encodeValue("foobar"))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
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

    /**
     * Codec using distinct key and value encodings to tell both encoders apart.
     */
    static class PrefixCodec implements RedisCodec<String, String>, ToByteBufEncoder<String, String> {

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return StringCodec.UTF8.decodeKey(bytes);
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return StringCodec.UTF8.decodeValue(bytes);
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StringCodec.UTF8.encodeKey("K:" + key);
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return StringCodec.UTF8.encodeValue("V:" + value);
        }

        @Override
        public void encodeKey(String key, ByteBuf target) {
            StringCodec.UTF8.encodeKey("K:" + key, target);
        }

        @Override
        public void encodeValue(String value, ByteBuf target) {
            StringCodec.UTF8.encodeValue("V:" + value, target);
        }

        @Override
        public int estimateSize(Object keyOrValue) {
            return StringCodec.UTF8.estimateSize(keyOrValue) + 2;
        }

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
