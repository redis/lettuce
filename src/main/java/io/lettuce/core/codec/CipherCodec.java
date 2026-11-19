package io.lettuce.core.codec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.function.ToIntFunction;

import javax.crypto.Cipher;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.buffer.ByteBuf;

/**
 * A crypto {@link RedisCodec} that that allows transparent encryption/decryption of values. This codec uses {@link Cipher}
 * instances provided by {@link CipherSupplier} to process encryption and decryption.
 * <p/>
 * This codec supports various encryption keys by encoding the key name and used key version in the value that is stored in
 * Redis. The message format for encryption is:
 *
 * <pre class="code">
 *     $&lt;key name&gt;+&lt;key version&gt;$&lt;cipher text&gt;
 * </pre>
 *
 * When storing initialization vectors, the message format is:
 *
 * <pre class="code">
 *     $&lt;key name&gt;+&lt;key version&gt;$&lt;initialization vector&gt;&lt;cipher text&gt;
 * </pre>
 *
 * The IV length is configured on the codec and is not encoded in the message. To read both formats with one codec, use
 * {@link #forValues(RedisCodec, CipherSupplier, CipherSupplier, ToIntFunction)} to select zero for legacy key versions and the
 * stored IV length for new key versions. Readers and writers must agree on the format for each key name and version.
 * <p>
 * Each value is prefixed with the key message that is enclosed with dollar ({@code $}) signs and using the plus sign
 * ({@code +}) to denote the key version. Decryption decodes the key name and requests a {@link Cipher} from
 * {@link CipherSupplier} to decrypt values with an appropriate key/{@link Cipher}.
 * <p/>
 * This {@link RedisCodec codec} does not provide re-wrapping or key rotation features.
 *
 * @author Mark Paluch
 * @author Armen Petrosyan
 * @since 5.2
 * @see CipherSupplier
 * @see KeyDescriptor
 */
public abstract class CipherCodec {

    private CipherCodec() {
    }

    /**
     * A {@link RedisCodec} that compresses values from a delegating {@link RedisCodec}.
     *
     * @param delegate codec used for key-value encoding/decoding, must not be {@code null}.
     * @param encrypt the {@link CipherSupplier} of encryption {@link Cipher} to use.
     * @param decrypt the {@link CipherSupplier} of decryption {@link Cipher} to use.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return Cipher codec.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> RedisCodec<K, V> forValues(RedisCodec<K, V> delegate, CipherSupplier encrypt, CipherSupplier decrypt) {
        LettuceAssert.notNull(delegate, "RedisCodec must not be null");
        LettuceAssert.notNull(encrypt, "Encryption Supplier must not be null");
        LettuceAssert.notNull(decrypt, "Decryption Supplier must not be null");
        return (RedisCodec) new CipherCodecWrapper((RedisCodec) delegate, encrypt, decrypt, descriptor -> 0);
    }

    /**
     * Create a codec that stores a fixed-length initialization vector between the key descriptor and ciphertext.
     * <p>
     * The encryption supplier must initialize each cipher with a fresh IV of the configured length. The decryption supplier can
     * obtain the stored IV from {@link KeyDescriptor#getInitializationVector()}. Both readers and writers must use the same IV
     * length. To read values with and without stored IVs, use
     * {@link #forValues(RedisCodec, CipherSupplier, CipherSupplier, ToIntFunction)} instead.
     *
     * @param <K> key type.
     * @param <V> value type.
     * @param delegate codec used for key-value encoding/decoding, must not be {@code null}.
     * @param encrypt supplier of initialized encryption ciphers, must not be {@code null}.
     * @param decrypt supplier of initialized decryption ciphers, must not be {@code null}.
     * @param initializationVectorLength number of IV bytes stored with each value, must be positive.
     * @return cipher codec that stores initialization vectors with encrypted values.
     * @throws IllegalArgumentException if a supplier or delegate is {@code null}, or the IV length is not positive.
     * @since 8.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> RedisCodec<K, V> forValues(RedisCodec<K, V> delegate, CipherSupplier encrypt, CipherSupplier decrypt,
            int initializationVectorLength) {

        LettuceAssert.notNull(delegate, "RedisCodec must not be null");
        LettuceAssert.notNull(encrypt, "Encryption Supplier must not be null");
        LettuceAssert.notNull(decrypt, "Decryption Supplier must not be null");
        LettuceAssert.isTrue(initializationVectorLength > 0, "Initialization vector length must be positive");
        return (RedisCodec) new CipherCodecWrapper((RedisCodec) delegate, encrypt, decrypt,
                descriptor -> initializationVectorLength);
    }

    /**
     * Create a codec that selects the stored IV length by key descriptor for each value.
     * <p>
     * The selector receives the key name and version before an IV is attached to the descriptor. Return zero for values without
     * a stored IV, or a positive length for values with a stored IV. A negative result is rejected when encoding or decoding.
     * The selector must be thread-safe and return a consistent length for each key name and version.
     * <p>
     * To migrate existing data, first deploy readers that accept both formats while retaining the legacy encryption key
     * version. Once all readers accept both, select a new encryption key version with a positive IV length. Keep the legacy
     * decryption key and original cipher parameters available until legacy values are replaced or expire. Reusing a key version
     * for a different layout makes those values ambiguous and can cause decryption failures or incorrect plaintext.
     *
     * @param <K> key type.
     * @param <V> value type.
     * @param delegate codec used for key-value encoding/decoding, must not be {@code null}.
     * @param encrypt supplier of initialized encryption ciphers, must not be {@code null}.
     * @param decrypt supplier of initialized decryption ciphers, must not be {@code null}.
     * @param initializationVectorLength selector of the number of IV bytes stored with each value, must not be {@code null}.
     * @return cipher codec that can read both legacy values and values with stored IVs.
     * @throws IllegalArgumentException if a supplier, delegate, or selector is {@code null}.
     * @since 8.0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <K, V> RedisCodec<K, V> forValues(RedisCodec<K, V> delegate, CipherSupplier encrypt, CipherSupplier decrypt,
            ToIntFunction<KeyDescriptor> initializationVectorLength) {

        LettuceAssert.notNull(delegate, "RedisCodec must not be null");
        LettuceAssert.notNull(encrypt, "Encryption Supplier must not be null");
        LettuceAssert.notNull(decrypt, "Decryption Supplier must not be null");
        LettuceAssert.notNull(initializationVectorLength, "Initialization vector length selector must not be null");
        return (RedisCodec) new CipherCodecWrapper((RedisCodec) delegate, encrypt, decrypt, initializationVectorLength);
    }

    @SuppressWarnings("unchecked")
    private static class CipherCodecWrapper implements RedisCodec<Object, Object>, ToByteBufEncoder<Object, Object> {

        private RedisCodec<Object, Object> delegate;

        private CipherSupplier encrypt;

        private CipherSupplier decrypt;

        private final ToIntFunction<KeyDescriptor> initializationVectorLength;

        CipherCodecWrapper(RedisCodec<Object, Object> delegate, CipherSupplier encrypt, CipherSupplier decrypt,
                ToIntFunction<KeyDescriptor> initializationVectorLength) {

            this.delegate = delegate;
            this.encrypt = encrypt;
            this.decrypt = decrypt;
            this.initializationVectorLength = initializationVectorLength;
        }

        @Override
        public Object decodeKey(ByteBuffer bytes) {
            return delegate.decodeKey(bytes);
        }

        @Override
        public Object decodeValue(ByteBuffer bytes) {

            KeyDescriptor keyDescriptor = KeyDescriptor.from(bytes);
            int initializationVectorLength = getInitializationVectorLength(keyDescriptor);
            if (initializationVectorLength > 0) {
                LettuceAssert.isTrue(bytes.remaining() >= initializationVectorLength, "Truncated initialization vector");
                byte[] initializationVector = new byte[initializationVectorLength];
                bytes.get(initializationVector);
                keyDescriptor = new KeyDescriptor(keyDescriptor.name, keyDescriptor.version, initializationVector);
            }

            try {
                return delegate.decodeValue(doWithCipher(this.decrypt.get(keyDescriptor), bytes));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void encodeKey(Object key, ByteBuf target) {

            if (delegate instanceof ToByteBufEncoder) {
                ((ToByteBufEncoder) delegate).encodeKey(key, target);
                return;
            }

            target.writeBytes(delegate.encodeKey(key));
        }

        @Override
        public void encodeValue(Object value, ByteBuf target) {

            ByteBuf serialized;
            if (delegate instanceof ToByteBufEncoder) {
                serialized = target.alloc().buffer(estimateSize(value));
                ((ToByteBufEncoder) delegate).encodeValue(value, serialized);
            } else {
                ByteBuffer byteBuffer = delegate.encodeValue(value);
                serialized = target.alloc().buffer(byteBuffer.remaining());
                serialized.writeBytes(byteBuffer);
            }

            try {
                KeyDescriptor keyDescriptor = this.encrypt.encryptionKey();
                Cipher cipher = this.encrypt.get(keyDescriptor);

                byte[] initializationVector = getInitializationVector(keyDescriptor, cipher);
                keyDescriptor.writeTo(target);
                target.writeBytes(initializationVector);
                doWithCipher(cipher, serialized, target);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            } finally {
                serialized.release();
            }
        }

        @Override
        public int estimateSize(Object keyOrValue) {

            if (delegate instanceof ToByteBufEncoder) {
                return ((ToByteBufEncoder) delegate).estimateSize(keyOrValue);
            }

            return /* blocksize */16 + /* avg key descriptor size */8;
        }

        @Override
        public ByteBuffer encodeKey(Object key) {
            return delegate.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(Object value) {
            try {

                ByteBuffer serialized = delegate.encodeValue(value);
                KeyDescriptor keyDescriptor = this.encrypt.encryptionKey();
                Cipher cipher = this.encrypt.get(keyDescriptor);

                byte[] initializationVector = getInitializationVector(keyDescriptor, cipher);
                ByteBuffer intermediate = ByteBuffer
                        .allocate(cipher.getOutputSize(serialized.remaining() + 3 + keyDescriptor.name.length + 10)
                                + initializationVector.length);

                keyDescriptor.writeTo(intermediate);
                intermediate.put(initializationVector);
                intermediate.put(doWithCipher(cipher, serialized));
                intermediate.flip();

                return intermediate;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }

        private int getInitializationVectorLength(KeyDescriptor keyDescriptor) {

            int length = initializationVectorLength.applyAsInt(keyDescriptor);
            LettuceAssert.isTrue(length >= 0, "Initialization vector length must not be negative");
            return length;
        }

        private byte[] getInitializationVector(KeyDescriptor keyDescriptor, Cipher cipher) {

            int initializationVectorLength = getInitializationVectorLength(keyDescriptor);
            if (initializationVectorLength == 0) {
                return new byte[0];
            }

            byte[] initializationVector = cipher.getIV();
            LettuceAssert.isTrue(initializationVector != null && initializationVector.length == initializationVectorLength,
                    "Cipher initialization vector must match the configured length");
            return initializationVector;
        }

        private void doWithCipher(Cipher cipher, ByteBuf serialized, ByteBuf target) throws GeneralSecurityException {

            ByteBuffer intermediate = ByteBuffer.allocate(cipher.getOutputSize(serialized.readableBytes()));

            ByteBuffer buffer = serialized.nioBuffer();

            cipher.update(buffer, intermediate);
            cipher.doFinal(buffer, intermediate);

            intermediate.flip();
            target.writeBytes(intermediate);

            serialized.readerIndex(serialized.writerIndex());
        }

        private ByteBuffer doWithCipher(Cipher cipher, ByteBuffer source) throws GeneralSecurityException {

            byte[] encrypted = new byte[source.remaining()];
            source.get(encrypted);

            byte[] update = cipher.update(encrypted);
            if (update == null) {
                update = new byte[0];
            }
            byte[] finalBytes = cipher.doFinal();

            ByteBuffer buffer = ByteBuffer.allocate(update.length + finalBytes.length);
            buffer.put(update).put(finalBytes).flip();

            return buffer;
        }

    }

    /**
     * Represents a supplier of {@link Cipher}. Requires to return a new {@link Cipher} instance as ciphers are one-time use
     * only.
     */
    @FunctionalInterface
    public interface CipherSupplier {

        /**
         * Creates a new {@link Cipher}.
         *
         * @return a new {@link Cipher}.
         * @throws GeneralSecurityException
         * @param keyDescriptor the key to use for the returned {@link Cipher}.
         */
        Cipher get(KeyDescriptor keyDescriptor) throws GeneralSecurityException;

        /**
         * Returns the latest {@link KeyDescriptor} to use for encryption.
         *
         * @return the {@link KeyDescriptor} to use for encryption.
         */
        default KeyDescriptor encryptionKey() {
            return KeyDescriptor.unnamed();
        }

    }

    /**
     * Descriptor to determine which crypto key to use. Allows versioning and usage of named keys. Key names must not contain
     * dollar {@code $} or plus {@code +} characters as these characters are used within the message format to encode key name
     * and key version.
     */
    public static class KeyDescriptor {

        private static final KeyDescriptor UNNAMED = new KeyDescriptor("".getBytes(StandardCharsets.US_ASCII), 0);

        private final byte[] name;

        private final int version;

        private final byte[] initializationVector;

        private KeyDescriptor(byte[] name, int version) {
            this(name, version, new byte[0]);
        }

        private KeyDescriptor(byte[] name, int version, byte[] initializationVector) {

            for (byte b : name) {
                if (b == '+' || b == '$') {
                    throw new IllegalArgumentException(
                            String.format("Key name %s must not contain plus (+) or dollar ($) characters", new String(name)));
                }
            }
            this.name = name;
            this.version = version;
            this.initializationVector = initializationVector;
        }

        /**
         * Returns the default {@link KeyDescriptor} that has no specified name.
         *
         * @return the default {@link KeyDescriptor}.
         */
        public static KeyDescriptor unnamed() {
            return UNNAMED;
        }

        /**
         * Create a named {@link KeyDescriptor} without version. Version defaults to zero.
         *
         * @param name the key name. Must not contain plus or dollar character.
         * @return the {@link KeyDescriptor} for {@code name}.
         */
        public static KeyDescriptor create(String name) {
            return create(name, 0);
        }

        /**
         * Create a named and versioned {@link KeyDescriptor}.
         *
         * @param name the key name. Must not contain plus or dollar character.
         * @param version the key version.
         * @return the {@link KeyDescriptor} for {@code name}.
         */
        public static KeyDescriptor create(String name, int version) {
            return create(name, version, Charset.defaultCharset());
        }

        /**
         * Create a named and versioned {@link KeyDescriptor} using {@link Charset} to encode {@code name} to its binary
         * representation.
         *
         * @param name the key name. Must not contain plus or dollar character.
         * @param version the key version.
         * @param charset must not be {@code null}.
         * @return the {@link KeyDescriptor} for {@code name}.
         */
        public static KeyDescriptor create(String name, int version, Charset charset) {

            LettuceAssert.notNull(name, "Name must not be null");
            LettuceAssert.notNull(charset, "Charset must not be null");

            return new KeyDescriptor(name.getBytes(charset), version);
        }

        static KeyDescriptor from(ByteBuffer bytes) {

            int end = -1;
            int version = -1;

            if (bytes.get() != '$') {
                throw new IllegalArgumentException("Cannot extract KeyDescriptor. Malformed message header.");
            }

            int startPosition = bytes.position();

            for (int i = 0; i < bytes.remaining(); i++) {
                if (bytes.get(bytes.position() + i) == '$') {
                    end = (bytes.position() - startPosition) + i;
                    break;
                }

                if (bytes.get(bytes.position() + i) == '+') {
                    version = (bytes.position() - startPosition) + i;
                }
            }

            if (end == -1 || version == -1) {
                throw new IllegalArgumentException("Cannot extract KeyDescriptor");
            }

            byte[] name = new byte[version];
            bytes.get(name);
            bytes.get();

            byte[] versionBytes = new byte[end - version - 1];
            bytes.get(versionBytes);
            bytes.get(); // skip last char

            return new KeyDescriptor(name, Integer.parseInt(new String(versionBytes)));
        }

        /**
         * Get the initialization vector stored with the value being decrypted.
         *
         * @return a copy of the IV, or an empty array when the codec does not store IVs or the descriptor is used for
         *         encryption.
         * @since 8.0
         */
        public byte[] getInitializationVector() {
            return initializationVector.clone();
        }

        public int getVersion() {
            return version;
        }

        /**
         * Returns the key {@code name} by decoding name bytes using the {@link Charset#defaultCharset() default charset}.
         *
         * @return the key name.
         */
        public String getName() {
            return getName(Charset.defaultCharset());
        }

        /**
         * Returns the key {@code name} by decoding name bytes using the given {@link Charset}.
         *
         * @param charset the {@link Charset} to use to decode the key name, must not be {@code null}.
         * @return the key name.
         */
        public String getName(Charset charset) {

            LettuceAssert.notNull(charset, "Charset must not be null");
            return new String(name, charset);
        }

        void writeTo(ByteBuf target) {
            target.writeByte('$').writeBytes(this.name).writeByte('+').writeBytes(Integer.toString(this.version).getBytes())
                    .writeByte('$');
        }

        void writeTo(ByteBuffer target) {
            target.put((byte) '$').put(this.name).put((byte) '+').put(Integer.toString(this.version).getBytes())
                    .put((byte) '$');
        }

    }

}
