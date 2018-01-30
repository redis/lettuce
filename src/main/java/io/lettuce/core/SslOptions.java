/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslProvider;

/**
 * Options to configure SSL options for the connections kept to Redis servers.
 *
 * @author Mark Paluch
 * @since 4.3
 */
public class SslOptions {

    public static final SslProvider DEFAULT_SSL_PROVIDER = SslProvider.JDK;

    private final SslProvider sslProvider;
    private final URL keystore;
    private final char[] keystorePassword;
    private final URL truststore;
    private final char[] truststorePassword;

    protected SslOptions(Builder builder) {
        this.sslProvider = builder.sslProvider;
        this.keystore = builder.keystore;
        this.keystorePassword = builder.keystorePassword;
        this.truststore = builder.truststore;
        this.truststorePassword = builder.truststorePassword;
    }

    protected SslOptions(SslOptions original) {
        this.sslProvider = original.getSslProvider();
        this.keystore = original.keystore;
        this.keystorePassword = original.keystorePassword;
        this.truststore = original.getTruststore();
        this.truststorePassword = original.getTruststorePassword();
    }

    /**
     * Create a copy of {@literal options}
     *
     * @param options the original
     * @return A new instance of {@link SslOptions} containing the values of {@literal options}
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
        private URL keystore;
        private char[] keystorePassword = new char[0];
        private URL truststore;
        private char[] truststorePassword = new char[0];

        private Builder() {
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
         * Use the OpenSSL provider for SSL connections. The OpenSSL provider requires the <a
         * href="http://netty.io/wiki/forked-tomcat-native.html">{@code netty-tcnative}</a> dependency with the OpenSSL JNI
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
         * Sets the Keystore file to load client certificates. The key store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Keystores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@literal null}.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(File keystore) {
            return keystore(keystore, new char[0]);
        }

        /**
         * Sets the Keystore file to load client certificates. The key store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Keystores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@literal null}.
         * @param keystorePassword the keystore password. May be empty to omit password and the keystore integrity check.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(File keystore, char[] keystorePassword) {

            LettuceAssert.notNull(keystore, "Keystore must not be null");
            LettuceAssert.isTrue(keystore.exists(), String.format("Keystore file %s does not exist", truststore));
            LettuceAssert.isTrue(keystore.isFile(), String.format("Keystore file %s is not a file", truststore));

            try {
                return keystore(keystore.toURI().toURL(), keystorePassword);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Sets the Keystore resource to load client certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Keystore are reloaded on each connection attempt that
         * allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@literal null}.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(URL keystore) {
            return keystore(keystore, new char[0]);
        }

        /**
         * Sets the Keystore resource to load client certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Keystores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param keystore the keystore file, must not be {@literal null}.
         * @param keystorePassword the keystore password. May be empty to omit password and the keystore integrity check.
         * @return {@code this}
         * @since 4.4
         */
        public Builder keystore(URL keystore, char[] keystorePassword) {

            LettuceAssert.notNull(keystore, "Keystore must not be null");

            this.keystore = keystore;

            if (keystorePassword != null) {
                this.keystorePassword = Arrays.copyOf(keystorePassword, keystorePassword.length);
            } else {
                this.keystorePassword = new char[0];
            }

            return this;
        }

        /**
         * Sets the Truststore file to load trusted certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Truststores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@literal null}.
         * @return {@code this}
         */
        public Builder truststore(File truststore) {
            return truststore(truststore, "");
        }

        /**
         * Sets the Truststore file to load trusted certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Truststores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@literal null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(File truststore, String truststorePassword) {

            LettuceAssert.notNull(truststore, "Truststore must not be null");
            LettuceAssert.isTrue(truststore.exists(), String.format("Truststore file %s does not exist", truststore));
            LettuceAssert.isTrue(truststore.isFile(), String.format("Truststore file %s is not a file", truststore));

            try {
                return truststore(truststore.toURI().toURL(), truststorePassword);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Truststores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@literal null}.
         * @return {@code this}
         */
        public Builder truststore(URL truststore) {
            return truststore(truststore, "");
        }

        /**
         * Sets the Truststore resource to load trusted certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Truststores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         *
         * @param truststore the truststore file, must not be {@literal null}.
         * @param truststorePassword the truststore password. May be empty to omit password and the truststore integrity check.
         * @return {@code this}
         */
        public Builder truststore(URL truststore, String truststorePassword) {

            LettuceAssert.notNull(truststore, "Truststore must not be null");

            this.truststore = truststore;

            if (LettuceStrings.isNotEmpty(truststorePassword)) {
                this.truststorePassword = truststorePassword.toCharArray();
            } else {
                this.truststorePassword = new char[0];
            }

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
     * @return the configured {@link SslProvider}.
     */
    public SslProvider getSslProvider() {
        return sslProvider;
    }

    /**
     * @return the keystore {@link URL}.
     */
    public URL getKeystore() {
        return keystore;
    }

    /**
     * @return the password for the keystore. May be empty.
     */
    public char[] getKeystorePassword() {
        return Arrays.copyOf(keystorePassword, keystorePassword.length);
    }

    /**
     * @return the truststore {@link URL}.
     */
    public URL getTruststore() {
        return truststore;
    }

    /**
     * @return the password for the truststore. May be empty.
     */
    public char[] getTruststorePassword() {
        return Arrays.copyOf(truststorePassword, truststorePassword.length);
    }
}
