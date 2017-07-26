package com.lambdaworks.redis;

/**
 * Options to configure a SOCKS 4/5 proxy.
 *
 * @author Yiding He
 */
public class SocksProxyOptions {

    public static final int SOCKS_VERSION_4 = 4;

    public static final int SOCKS_VERSION_5 = 5;

    public static final int DEFAULT_SOCKS_VERSION = SOCKS_VERSION_5;

    private final String host;

    private final int port;

    private final String username;

    private final String password;

    private final int socksVersion;

    protected SocksProxyOptions(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.socksVersion = builder.socksVersion;
    }

    protected SocksProxyOptions(SocksProxyOptions original) {
        this.host = original.getHost();
        this.port = original.getPort();
        this.username = original.getUsername();
        this.password = original.getPassword();
        this.socksVersion = original.getSocksVersion();
    }

    /**
     * Returns a new {@link Builder} to construct {@link SocksProxyOptions}.
     *
     * @param host host name or ip address of proxy server.
     * @param port port of proxy server.
     * @return a new {@link Builder} to construct {@link SocksProxyOptions}.
     */
    public static Builder builder(String host, int port) {
        return new Builder(host, port);
    }

    public static class Builder {

        private String host;

        private int port;

        private String username;

        private String password;

        private int socksVersion = DEFAULT_SOCKS_VERSION;

        private Builder(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * Set version number of socks proxy.
         *
         * @param socksVersion version number of socks proxy.
         * @return {@code this}
         */
        public Builder socksVersion(int socksVersion) {
            if (socksVersion != SOCKS_VERSION_4 && socksVersion != SOCKS_VERSION_5) {
                throw new IllegalArgumentException("socks version should be 4 or 5.");
            }
            this.socksVersion = socksVersion;
            return this;
        }

        /**
         * Set password of socks proxy.
         *
         * @param password password of socks proxy.
         * @return {@code this}
         */
        public Builder password(String password) {

            this.password = password;
            return this;
        }

        /**
         * Set username of socks proxy.
         *
         * @param username username of socks proxy.
         * @return {@code this}
         */
        public Builder username(String username) {

            this.username = username;
            return this;
        }

        /**
         * Create a new instance of {@link SocksProxyOptions}
         *
         * @return new instance of {@link SocksProxyOptions}
         */
        public SocksProxyOptions build() {
            return new SocksProxyOptions(this);
        }
    }

    /**
     * Get host name or ip address of proxy server.
     *
     * @return host name or ip address of proxy server.
     */
    public String getHost() {
        return host;
    }

    /**
     * Get port of proxy server.
     *
     * @return port of proxy server.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get username of socks proxy.
     *
     * @return username of socks proxy.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get password of socks proxy. Socks proxy version 4 don't use a password.
     *
     * @return password of socks proxy.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get socks version of proxy server.
     *
     * @return socks version of proxy server.
     */
    public int getSocksVersion() {
        return socksVersion;
    }
}
