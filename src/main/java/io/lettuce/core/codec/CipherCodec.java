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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

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
 * Each value is prefixed with the key message that is enclosed with dollar ({@code $}) signs and using the plus sign
 * ({@code +}) to denote the key version. Decryption decodes the key name and requests a {@link Cipher} from
 * {@link CipherSupplier} to decrypt values with an appropriate key/{@link Cipher}.
 * <p/>
 * This {@link RedisCodec codec} does not provide re-wrapping or key rotation features.
 *
 * @author Mark Paluch
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
        return (RedisCodec) new CipherCodecWrapper((RedisCodec) delegate, encrypt, decrypt);
    }

    @SuppressWarnings("unchecked")
    private static class CipherCodecWrapper implements RedisCodec<Object, Object>, ToByteBufEncoder<Object, Object> {

        private RedisCodec<Object, Object> delegate;

        private CipherSupplier encrypt;

        private CipherSupplier decrypt;

        CipherCodecWrapper(RedisCodec<Object, Object> delegate, CipherSupplier encrypt, CipherSupplier decrypt) {

            this.delegate = delegate;
            this.encrypt = encrypt;
            this.decrypt = decrypt;
        }

        @Override
        public Object decodeKey(ByteBuffer bytes) {
            return delegate.decodeKey(bytes);
        }

        @Override
        public Object decodeValue(ByteBuffer bytes) {

            KeyDescriptor keyDescriptor = KeyDescriptor.from(bytes);

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
                ((ToByteBufEncoder) delegate).encodeKey(value, serialized);
            } else {
                ByteBuffer byteBuffer = delegate.encodeValue(value);
                serialized = target.alloc().buffer(byteBuffer.remaining());
                serialized.writeBytes(byteBuffer);
            }

            try {
                KeyDescriptor keyDescriptor = this.encrypt.encryptionKey();
                Cipher cipher = this.encrypt.get(keyDescriptor);

                keyDescriptor.writeTo(target);
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

                ByteBuffer intermediate = ByteBuffer
                        .allocate(cipher.getOutputSize(serialized.remaining() + 3 + keyDescriptor.name.length + 10));

                keyDescriptor.writeTo(intermediate);
                intermediate.put(doWithCipher(cipher, serialized));
                intermediate.flip();

                return intermediate;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
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

        private KeyDescriptor(byte[] name, int version) {

            for (byte b : name) {
                if (b == '+' || b == '$') {
                    throw new IllegalArgumentException(
                            String.format("Key name %s must not contain plus (+) or dollar ($) characters", new String(name)));
                }
            }
            this.name = name;
            this.version = version;
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
