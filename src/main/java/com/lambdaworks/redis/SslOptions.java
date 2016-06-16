package com.lambdaworks.redis;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.lambdaworks.redis.internal.LettuceAssert;

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
    private final URL truststore;

    protected SslOptions(Builder builder) {
        this.sslProvider = builder.sslProvider;
        this.truststore = builder.truststore;
    }

    protected SslOptions(SslOptions original) {
        this.sslProvider = original.getSslProvider();
        this.truststore = original.getTruststore();
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
        private URL truststore;

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
         * Sets the Truststore file to load trusted certificates. The trust store file must be supported by
         * {@link java.security.KeyStore} which is {@code jks} by default. Truststores are reloaded on each connection attempt
         * that allows to replace certificates during runtime.
         * 
         * @param truststore the truststore file, must not be {@literal null}.
         * @return {@code this}
         */
        public Builder truststore(File truststore) {

            LettuceAssert.notNull(truststore, "Truststore must not be null");
            LettuceAssert.isTrue(truststore.exists(), String.format("Truststore file %s does not exist", truststore));
            LettuceAssert.isTrue(truststore.isFile(), String.format("Truststore file %s is not a file", truststore));

            try {
                this.truststore = truststore.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }

            return this;
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

            LettuceAssert.notNull(truststore, "Truststore must not be null");
            this.truststore = truststore;

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

    public SslProvider getSslProvider() {
        return sslProvider;
    }

    public URL getTruststore() {
        return truststore;
    }
}
