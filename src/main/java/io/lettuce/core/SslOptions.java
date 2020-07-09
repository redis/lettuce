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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

/**
 * Options to configure SSL options for the connections kept to Redis servers.
 *
 * @author Mark Paluch
 * @author Amin Mohtashami
 * @author Felipe Ruiz
 * @since 4.3
 */
public class SslOptions {

    public static final SslProvider DEFAULT_SSL_PROVIDER = SslProvider.JDK;

    private final String keyStoreType;

    private final SslProvider sslProvider;

    private final URL keystore;

    private final char[] keystorePassword;

    private final URL truststore;

    private final char[] truststorePassword;

    private final String[] protocols;

    private final String[] cipherSuites;

    private final Consumer<SslContextBuilder> sslContextBuilderCustomizer;

    private final Supplier<SSLParameters> sslParametersSupplier;

    private final KeystoreAction keymanager;

    private final KeystoreAction trustmanager;

    private final Duration handshakeTimeout;

    protected SslOptions(Builder builder) {
        this.keyStoreType = builder.keyStoreType;
        this.sslProvider = builder.sslProvider;
        this.handshakeTimeout = builder.sslHandshakeTimeout;
        this.keystore = builder.keystore;
        this.keystorePassword = builder.keystorePassword;
        this.truststore = builder.truststore;
        this.truststorePassword = builder.truststorePassword;

        this.protocols = builder.protocols;
        this.cipherSuites = builder.cipherSuites;

        this.sslContextBuilderCustomizer = builder.sslContextBuilderCustomizer;
        this.sslParametersSupplier = builder.sslParametersSupplier;
        this.keymanager = builder.keymanager;
        this.trustmanager = builder.trustmanager;
    }

    protected SslOptions(SslOptions original) {
        this.keyStoreType = original.keyStoreType;
        this.sslProvider = original.getSslProvider();
        this.handshakeTimeout = original.handshakeTimeout;
        this.keystore = original.keystore;
        this.keystorePassword = original.keystorePassword;
        this.truststore = original.getTruststore();
        this.truststorePassword = original.getTruststorePassword();

        this.protocols = original.protocols;
        this.cipherSuites = original.cipherSuites;

        this.sslContextBuilderCustomizer = original.sslContextBuilderCustomizer;
        this.sslParametersSupplier = original.sslParametersSupplier;
        this.keymanager = original.keymanager;
        this.trustmanager = original.trustmanager;
    }

    /**
     * Create a copy of {@literal options}.
     *
     * @param options the original.
     * @return A new instance of {@link SslOptions} containing the values of {@literal options}.
     */
    public static SslOptions copyOf(SslOptions options) {
        return new SslOptions(options);
    }

    /**
     * Returns a new {@link SslOptions.Builder} to construct {@link SslOptions}.
     *
     * @return a new {@link SslOptions.Builder} to construct {@link SslOptions}.
     */
    public static SslOptions.Builder builder() {
        return new SslOptions.Builder();
    }

    /**
     * Create a new {@link SslOptions} using default settings.
     *
     * @return a new instance of default cluster client client options.
     */
    public static SslOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link SslOptions}.
     */
    public static class Builder {

        private SslProvider sslProvider = DEFAULT_SSL_PROVIDER;

        private String keyStoreType;

        private URL keystore;

        private char[] keystorePassword = new char[0];

        private URL truststore;

        private char[] truststorePassword = new char[0];

        private String[] protocols = null;

        private String[] cipherSuites = null;

        private Consumer<SslContextBuilder> sslContextBuilderCustomizer = contextBuilder -> {

        };

        private Supplier<SSLParameters> sslParametersSupplier = SSLParameters::new;

        private KeystoreAction keymanager = KeystoreAction.NO_OP;

        private KeystoreAction trustmanager = KeystoreAction.NO_OP;

        private Duration sslHandshakeTimeout = Duration.ofSeconds(10);

        private Builder() {
        }

        /**
         * Sets the cipher suites to use.
         *
         * @param cipherSuites cipher suites to use.
         * @return {@code this}
         * @since 5.3
         */
        public Builder cipherSuites(String... cipherSuites) {

            LettuceAssert.notNull(cipherSuites, "Cipher suites must not be null");

            this.cipherSuites = cipherSuites;
            return this;
        }

        /**
         * Use the JDK SSL provider for SSL connections.
         *
         * @return {@code this}
         */
        public Builder jdkSslProvider() {
            return sslProvider(SslProvider.JDK);
        }

        /**
         * Use the OpenSSL provider for SSL connections. The OpenSSL provider requires the
         * <a href="http://netty.io/wiki/forked-tomcat-native.html">{@code netty-tcnative}</a> dependency with the OpenSSL JNI
         * binary.
         *
         * @return {@code this}
         * @throws IllegalStateException if OpenSSL is not available
         */
        public Builder openSslProvider() {
            return sslProvider(SslProvider.OPENSSL);
        }

        private Builder sslProvider(SslProvider sslProvider) {

            if (sslProvider == SslProvider.OPENSSL) {
                if (!OpenSsl.isAvailable()) {
                    throw new IllegalStateException("OpenSSL SSL Provider is not available");
                }
            }

            this.sslProvider = sslProvider;

            return this;
        }

        /**
         * Sets a timeout for the SSL handshake.
         *
         * @param timeout {@link Duration}.
         * @return {@code this}
         * @since 5.3.2
         */
        public Builder handshakeTimeout(Duration timeout) {

            LettuceAssert.notNull(timeout, "SSL Handshake Timeout must not be null");

            this.sslHandshakeTimeout = timeout;
            return this;
        }

        /**
         * Sets the KeyStore type. Defaults to {@link KeyStore#getDefaultType()} if not set.
         *
         * @param keyStoreType the keystore type to use, must not be {@code null}.
         * @return {@code this}
         * @since 5.3
         */
        public Builder keyStoreType(String keyStoreType) {

            LettuceAssert.notNull(keyStoreType, "KeyStoreType must not be null");
            this.keyStoreType = keyStoreType;
            return this;
        }

        /**
         * Sets the Keystore file to load client certificates. The key store file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@code null}.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(File keystore) {
            return keystore(keystore, new char[0]);
        }

        /**
         * Sets the Keystore file to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@code null}.
         * @param keystorePassword the keystore password. May be empty to omit password and the keystore integrity check.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(File keystore, char[] keystorePassword) {

            LettuceAssert.notNull(keystore, "Keystore must not be null");
            LettuceAssert.isTrue(keystore.exists(), () -> String.format("Keystore file %s does not exist", truststore));
            LettuceAssert.isTrue(keystore.isFile(), () -> String.format("Keystore %s is not a file", truststore));

            return keystore(Resource.from(keystore), keystorePassword);
        }

        /**
         * Sets the Keystore resource to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore URL, must not be {@code null}.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(URL keystore) {
            return keystore(keystore, null);
        }

        /**
         * Sets the Keystore resource to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@code null}.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(URL keystore, char[] keystorePassword) {

            LettuceAssert.notNull(keystore, "Keystore must not be null");

            this.keystore = keystore;

            return keystore(Resource.from(keystore), keystorePassword);
        }

        /**
         * Sets the key file and its certificate to use for client authentication. The key is reloaded on each connection
         * attempt that allows to replace certificates during runtime.
         *
         * @param keyCertChainFile an X.509 certificate chain file in PEM format.
         * @param keyFile a PKCS#8 private key file in PEM format.
         * @param keyPassword the password of the {@code keyFile}, or {@code null} if it's not password-protected.
         * @return {@code this}
         * @since 5.3
         */
        public Builder keyManager(File keyCertChainFile, File keyFile, char[] keyPassword) {

            LettuceAssert.notNull(keyCertChainFile, "Key certificate file must not be null");
            LettuceAssert.notNull(keyFile, "Key file must not be null");
            LettuceAssert.isTrue(keyCertChainFile.exists(),
                    () -> String.format("Key certificate file %s does not exist", keyCertChainFile));
            LettuceAssert.isTrue(keyCertChainFile.isFile(),
                    () -> String.format("Key certificate %s is not a file", keyCertChainFile));
            LettuceAssert.isTrue(keyFile.exists(), () -> String.format("Key file %s does not exist", keyFile));
            LettuceAssert.isTrue(keyFile.isFile(), () -> String.format("Key %s is not a file", keyFile));

            return keyManager(Resource.from(keyCertChainFile), Resource.from(keyFile), keyPassword);
        }

        /**
         * Sets the key and its certificate to use for client authentication. The key is reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param keyCertChain an {@link Resource} for a X.509 certificate chain in PEM format.
         * @param key an {@link Resource} for a PKCS#8 private key in PEM format.
         * @param keyPassword the password of the {@code keyFile}, or {@code null} if it's not password-protected.
         * @return {@code this}
         * @since 5.3
         * @see Resource
         */
        public Builder keyManager(Resource keyCertChain, Resource key, char[] keyPassword) {

            LettuceAssert.notNull(keyCertChain, "KeyChain InputStreamProvider must not be null");
            LettuceAssert.notNull(key, "Key InputStreamProvider must not be null");

            char[] passwordToUse = getPassword(keyPassword);
            this.keymanager = (builder, keyStoreType) -> {

                try (InputStream keyCertChainIs = keyCertChain.get(); InputStream keyIs = key.get()) {
                    builder.keyManager(keyCertChainIs, keyIs,
                            passwordToUse == null || passwordToUse.length == 0 ? null : new String(passwordToUse));
                }
            };

            return this;
        }

        /**
         * Sets the {@link KeyManagerFactory}.
         *
         * @param keyManagerFactory the {@link KeyManagerFactory} to use.
         * @return {@code this}
         * @since 5.3
         */
        public Builder keyManager(KeyManagerFactory keyManagerFactory) {

            LettuceAssert.notNull(keyManagerFactory, "KeyManagerFactory must not be null");

            this.keymanager = (builder, keyStoreType) -> builder.keyManager(keyManagerFactory);

            return this;
        }

        /**
         * Sets the Java Keystore resource to load client certificates. The keystore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The keystore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param resource the provider that opens a {@link InputStream} to the keystore file, must not be {@code null}.
         * @param keystorePassword the keystore password. May be empty to omit password and the keystore integrity check.
         * @return {@code this}
         * @since 5.3
         */
        public Builder keystore(Resource resource, char[] keystorePassword) {

            LettuceAssert.notNull(resource, "Keystore InputStreamProvider must not be null");

            char[] keystorePasswordToUse = getPassword(keystorePassword);
            this.keystorePassword = keystorePasswordToUse;
            this.keymanager = (builder, keyStoreType) -> {

                try (InputStream is = resource.get()) {
                    builder.keyManager(createKeyManagerFactory(is, keystorePasswordToUse, keyStoreType));
                }
            };

            return this;
        }

        /**
         * Sets the protocol used for the connection established to Redis Server, such as {@code TLSv1.2, TLSv1.1, TLSv1}.
         *
         * @param protocols list of desired protocols to use.
         * @return {@code this}
         * @since 5.3
         */
        public Builder protocols(String... protocols) {

            LettuceAssert.notNull(protocols, "Protocols  must not be null");

            this.protocols = protocols;
            return this;
        }

        /**
         * Sets the Truststore file to load trusted certificates. The truststore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @return {@code this}
         */
        public Builder truststore(File truststore) {
            return truststore(truststore, null);
        }

        /**
         * Sets the Truststore file to load trusted certificates. The truststore file must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(File truststore, String truststorePassword) {

            LettuceAssert.notNull(truststore, "Truststore must not be null");
            LettuceAssert.isTrue(truststore.exists(), () -> String.format("Truststore file %s does not exist", truststore));
            LettuceAssert.isTrue(truststore.isFile(), () -> String.format("Truststore file %s is not a file", truststore));

            return truststore(Resource.from(truststore), getPassword(truststorePassword));
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The truststore resource must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @return {@code this}
         */
        public Builder truststore(URL truststore) {
            return truststore(truststore, null);
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The truststore resource must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@code null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(URL truststore, String truststorePassword) {

            LettuceAssert.notNull(truststore, "Truststore must not be null");

            this.truststore = truststore;

            return truststore(Resource.from(truststore), getPassword(truststorePassword));
        }

        /**
         * Sets the certificate file to load trusted certificates. The file must provide X.509 certificates in PEM format.
         * Certificates are reloaded on each connection attempt that allows to replace certificates during runtime.
         *
         * @param certCollection the X.509 certificate collection in PEM format.
         * @return {@code this}
         * @since 5.3
         */
        public Builder trustManager(File certCollection) {

            LettuceAssert.notNull(certCollection, "Certificate collection must not be null");
            LettuceAssert.isTrue(certCollection.exists(),
                    () -> String.format("Certificate collection file %s does not exist", certCollection));
            LettuceAssert.isTrue(certCollection.isFile(),
                    () -> String.format("Certificate collection %s is not a file", certCollection));

            return trustManager(Resource.from(certCollection));
        }

        /**
         * Sets the certificate resource to load trusted certificates. The file must provide X.509 certificates in PEM format.
         * Certificates are reloaded on each connection attempt that allows to replace certificates during runtime.
         *
         * @param certCollection the X.509 certificate collection in PEM format.
         * @return {@code this}
         * @since 5.3
         */
        public Builder trustManager(Resource certCollection) {

            LettuceAssert.notNull(certCollection, "Truststore must not be null");

            this.trustmanager = (builder, keyStoreType) -> {

                try (InputStream is = certCollection.get()) {
                    builder.trustManager(is);
                }
            };

            return this;
        }

        /**
         * Sets the {@link TrustManagerFactory}.
         *
         * @param trustManagerFactory the {@link TrustManagerFactory} to use.
         * @return {@code this}
         * @since 5.3
         */
        public Builder trustManager(TrustManagerFactory trustManagerFactory) {

            LettuceAssert.notNull(trustManagerFactory, "TrustManagerFactory must not be null");

            this.trustmanager = (builder, keyStoreType) -> {
                builder.trustManager(trustManagerFactory);
            };

            return this;
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The truststore resource must be supported by
         * {@link java.security.KeyStore} which is {@link KeyStore#getDefaultType()} by default. The truststore is reloaded on
         * each connection attempt that allows to replace certificates during runtime.
         *
         * @param resource the provider that opens a {@link InputStream} to the keystore file, must not be {@code null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(Resource resource, char[] truststorePassword) {

            LettuceAssert.notNull(resource, "Truststore InputStreamProvider must not be null");

            char[] passwordToUse = getPassword(truststorePassword);
            this.truststorePassword = passwordToUse;
            this.trustmanager = (builder, keyStoreType) -> {

                try (InputStream is = resource.get()) {
                    builder.trustManager(createTrustManagerFactory(is, passwordToUse, keyStoreType));
                }
            };

            return this;
        }

        /**
         * Applies a {@link SslContextBuilder} customizer by calling {@link java.util.function.Consumer#accept(Object)}
         *
         * @param contextBuilderCustomizer builder callback to customize the {@link SslContextBuilder}.
         * @return {@code this}
         * @since 5.3
         */
        public Builder sslContext(Consumer<SslContextBuilder> contextBuilderCustomizer) {

            LettuceAssert.notNull(contextBuilderCustomizer, "SslContextBuilder customizer must not be null");

            this.sslContextBuilderCustomizer = contextBuilderCustomizer;
            return this;
        }

        /**
         * Configures a {@link Supplier} to create {@link SSLParameters}.
         *
         * @param sslParametersSupplier {@link Supplier} for {@link SSLParameters}.
         * @return {@code this}
         * @since 5.3
         */
        public Builder sslParameters(Supplier<SSLParameters> sslParametersSupplier) {

            LettuceAssert.notNull(sslParametersSupplier, "SSLParameters supplier must not be null");

            this.sslParametersSupplier = sslParametersSupplier;
            return this;
        }

        /**
         * Create a new instance of {@link SslOptions}
         *
         * @return new instance of {@link SslOptions}
         */
        public SslOptions build() {
            return new SslOptions(this);
        }

    }

    /**
     * Creates a new {@link SslContextBuilder} object that is pre-configured with values from this {@link SslOptions} object.
     *
     * @return a new {@link SslContextBuilder}.
     * @throws IOException thrown when loading the keystore or the truststore fails.
     * @throws GeneralSecurityException thrown when loading the keystore or the truststore fails.
     * @since 5.3
     */
    public SslContextBuilder createSslContextBuilder() throws IOException, GeneralSecurityException {

        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().sslProvider(this.sslProvider)
                .keyStoreType(keyStoreType);

        if (protocols != null && protocols.length > 0) {
            sslContextBuilder.protocols(protocols);
        }

        if (cipherSuites != null && cipherSuites.length > 0) {
            sslContextBuilder.ciphers(Arrays.asList(cipherSuites));
        }

        keymanager.accept(sslContextBuilder, this.keyStoreType);
        trustmanager.accept(sslContextBuilder, this.keyStoreType);
        sslContextBuilderCustomizer.accept(sslContextBuilder);

        return sslContextBuilder;
    }

    /**
     * Creates a {@link SSLParameters} object that is pre-configured with values from this {@link SslOptions} object.
     *
     * @return a new a {@link SSLParameters} object.
     * @since 5.3
     */
    public SSLParameters createSSLParameters() {

        SSLParameters sslParams = sslParametersSupplier.get();

        if (protocols != null && protocols.length > 0) {
            sslParams.setProtocols(protocols);
        }

        if (cipherSuites != null && cipherSuites.length > 0) {
            sslParams.setCipherSuites(cipherSuites);
        }

        return sslParams;
    }

    /**
     * Returns a builder to create new {@link SslOptions} whose settings are replicated from the current {@link SslOptions}.
     *
     * @return a {@link SslOptions.Builder} to create new {@link SslOptions} whose settings are replicated from the current
     *         {@link SslOptions}.
     * @since 5.3
     */
    public SslOptions.Builder mutate() {

        Builder builder = builder();
        builder.keyStoreType = this.keyStoreType;
        builder.sslProvider = this.getSslProvider();
        builder.keystore = this.keystore;
        builder.keystorePassword = this.keystorePassword;
        builder.truststore = this.getTruststore();
        builder.truststorePassword = this.getTruststorePassword();

        builder.protocols = this.protocols;
        builder.cipherSuites = this.cipherSuites;

        builder.sslContextBuilderCustomizer = this.sslContextBuilderCustomizer;
        builder.sslParametersSupplier = this.sslParametersSupplier;
        builder.keymanager = this.keymanager;
        builder.trustmanager = this.trustmanager;
        builder.sslHandshakeTimeout = this.handshakeTimeout;

        return builder;
    }

    /**
     * @return the configured {@link SslProvider}.
     */
    @Deprecated
    public SslProvider getSslProvider() {
        return sslProvider;
    }

    /**
     * @return the keystore {@link URL}.
     * @deprecated since 5.3, {@link javax.net.ssl.KeyManager} is configured via {@link #createSslContextBuilder()}.
     */
    @Deprecated
    public URL getKeystore() {
        return keystore;
    }

    /**
     * @return the set of protocols.
     */
    public String[] getProtocols() {
        return protocols;
    }

    /**
     * @return the set of cipher suites.
     */
    public String[] getCipherSuites() {
        return cipherSuites;
    }

    /**
     * @return the SSL handshake timeout
     * @since 5.3.2
     */
    public Duration getHandshakeTimeout() {
        return handshakeTimeout;
    }

    /**
     * @return the password for the keystore. May be empty.
     * @deprecated since 5.3, {@link javax.net.ssl.KeyManager} is configured via {@link #createSslContextBuilder()}.
     */
    @Deprecated
    public char[] getKeystorePassword() {
        return Arrays.copyOf(keystorePassword, keystorePassword.length);
    }

    /**
     * @return the truststore {@link URL}.
     * @deprecated since 5.3, {@link javax.net.ssl.TrustManager} is configured via {@link #createSslContextBuilder()}.
     */
    @Deprecated
    public URL getTruststore() {
        return truststore;
    }

    /**
     *
     * @return the password for the truststore. May be empty.
     * @deprecated since 5.3, {@link javax.net.ssl.TrustManager} is configured via {@link #createSslContextBuilder()}.
     */
    @Deprecated
    public char[] getTruststorePassword() {
        return Arrays.copyOf(truststorePassword, truststorePassword.length);
    }

    private static KeyManagerFactory createKeyManagerFactory(InputStream inputStream, char[] storePassword, String keyStoreType)
            throws GeneralSecurityException, IOException {

        KeyStore keyStore = getKeyStore(inputStream, storePassword, keyStoreType);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, storePassword == null ? new char[0] : storePassword);

        return keyManagerFactory;
    }

    private static KeyStore getKeyStore(InputStream inputStream, char[] storePassword, String keyStoreType)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        KeyStore keyStore = KeyStore
                .getInstance(LettuceStrings.isEmpty(keyStoreType) ? KeyStore.getDefaultType() : keyStoreType);

        try {
            keyStore.load(inputStream, storePassword);
        } finally {
            inputStream.close();
        }
        return keyStore;
    }

    private static TrustManagerFactory createTrustManagerFactory(InputStream inputStream, char[] storePassword,
            String keystoreType) throws GeneralSecurityException, IOException {

        KeyStore trustStore = getKeyStore(inputStream, storePassword, keystoreType);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        return trustManagerFactory;
    }

    private static char[] getPassword(String truststorePassword) {
        return LettuceStrings.isNotEmpty(truststorePassword) ? truststorePassword.toCharArray() : null;
    }

    private static char[] getPassword(char[] chars) {
        return chars != null ? Arrays.copyOf(chars, chars.length) : null;
    }

    @FunctionalInterface
    interface KeystoreAction {

        static KeystoreAction NO_OP = (builder, keyStoreType) -> {
        };

        void accept(SslContextBuilder sslContextBuilder, String keyStoreType) throws IOException, GeneralSecurityException;

    }

    /**
     * Supplier for a {@link InputStream} representing a resource. The resulting {@link InputStream} must be closed by the
     * calling code.
     *
     * @since 5.3
     */
    @FunctionalInterface
    public interface Resource {

        /**
         * Create a {@link Resource} that obtains a {@link InputStream} from a {@link URL}.
         *
         * @param url the URL to obtain the {@link InputStream} from.
         * @return a {@link Resource} that opens a connection to the URL and obtains the {@link InputStream} for it.
         */
        static Resource from(URL url) {

            LettuceAssert.notNull(url, "URL must not be null");

            return () -> url.openConnection().getInputStream();
        }

        /**
         * Create a {@link Resource} that obtains a {@link InputStream} from a {@link File}.
         *
         * @param file the File to obtain the {@link InputStream} from.
         * @return a {@link Resource} that obtains the {@link FileInputStream} for the given {@link File}.
         */
        static Resource from(File file) {

            LettuceAssert.notNull(file, "File must not be null");

            return () -> new FileInputStream(file);
        }

        /**
         * Obtains the {@link InputStream}.
         *
         * @return the {@link InputStream}.
         * @throws IOException
         */
        InputStream get() throws IOException;

    }

}
