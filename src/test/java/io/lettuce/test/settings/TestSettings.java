/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.test.settings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

import io.lettuce.test.env.Endpoints;

/**
 * This class provides settings used while testing. You can override these using system properties.
 * <p>
 * Alternatively, tests can run against externally provisioned databases (e.g. Redis Enterprise) by setting the
 * {@code TEST_ENV_PROVIDER=re} environment variable. In that mode, host, port, credentials and TLS mode are resolved from the
 * endpoint configuration file referenced by {@code REDIS_ENDPOINTS_CONFIG_PATH} (see {@link Endpoints}), using the database
 * named by {@code RE_DB_NAME} (defaults to {@literal standalone}). Explicit system properties always take precedence over the
 * provider.
 *
 * @author Mark Paluch
 * @author Tugdual Grall
 */
public class TestSettings {

    private TestSettings() {
    }

    /**
     *
     * @return hostname of your redis instance. Defaults to {@literal localhost}. Can be overriden with
     *         {@code -Dhost=YourHostName}
     */
    public static String host() {
        String host = System.getProperty("host");
        if (host != null) {
            return host;
        }
        return TestEnvProvider.isActive() ? TestEnvProvider.host() : "localhost";
    }

    /**
     *
     * @return unix domain socket name of your redis instance. Defaults to {@literal work/socket-6479}. Can be overriden with
     *         {@code -Ddomainsocket=YourSocket}
     */
    public static String socket() {
        return System.getProperty("domainsocket", "work/socket-6482");
    }

    /**
     *
     * @return unix domain socket name of your redis sentinel instance. Defaults to {@literal work/socket-26379}. Can be
     *         overriden with {@code -Dsentineldomainsocket=YourSocket}
     */
    public static String sentinelSocket() {
        return System.getProperty("sentineldomainsocket", "work/socket-26379");
    }

    /**
     *
     * @return resolved address of {@link #host()}
     * @throws IllegalStateException when hostname cannot be resolved
     */
    public static String hostAddr() {
        try {
            InetAddress[] allByName = InetAddress.getAllByName(host());
            for (InetAddress inetAddress : allByName) {
                if (inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
            return InetAddress.getByName(host()).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     *
     * @return default username of your redis instance.
     */
    public static String username() {
        if (TestEnvProvider.isActive() && TestEnvProvider.username() != null) {
            return TestEnvProvider.username();
        }
        return "default";
    }

    /**
     *
     * @return password of your redis instance. Defaults to {@literal passwd}. Can be overridden with
     *         {@code -Dpassword=YourPassword}
     */
    public static CharSequence password() {
        String password = System.getProperty("password");
        if (password != null) {
            return password;
        }
        return TestEnvProvider.isActive() ? TestEnvProvider.password() : "foobared";
    }

    /**
     *
     * @return password of a second user your redis instance. Defaults to {@literal lettuceTest}. Can be overridden with
     *         {@code -Dacl.username=SampleUsername}
     */
    public static String aclUsername() {
        return System.getProperty("acl.username", "lettuceTest");
    }

    /**
     *
     * @return password of a second user of your redis instance. Defaults to {@literal lettuceTestPasswd}. Can be overridden
     *         with {@code -Dacl.password=SamplePassword}
     */
    public static CharSequence aclPassword() {
        return System.getProperty("acl.password", "lettuceTestPasswd");
    }

    /**
     *
     * @return port of your redis instance. Defaults to {@literal 6479}. Can be overriden with {@code -Dport=1234}
     */
    public static int port() {
        String port = System.getProperty("port");
        if (port != null) {
            return Integer.parseInt(port);
        }
        return TestEnvProvider.isActive() ? TestEnvProvider.port() : 6479;
    }

    /**
     *
     * @return sslport of your redis instance. Defaults to {@literal 6443}. Can be overriden with {@code -Dsslport=1234}
     */
    public static int sslPort() {
        return Integer.parseInt(System.getProperty("sslport", "6443"));
    }

    /**
     *
     * @return {@link #port()} with added {@literal 500}
     */
    public static int nonexistentPort() {
        return port() + 500;
    }

    /**
     *
     * @param offset
     * @return {@link #port()} with added {@literal offset}
     */
    public static int port(int offset) {
        return port() + offset;
    }

    /**
     *
     * @param offset
     * @return {@link #sslPort()} with added {@literal offset}
     */
    public static int sslPort(int offset) {
        int port = sslPort() + offset;
        if (port == 7443) {
            throw new IllegalStateException("Please use a different port than 7443. Thank you.");
        }
        return port;
    }

    /**
     *
     * @return base port of the test proxy used for simulating network issues. Defaults to {@literal 9479}. Can be overridden
     *         with {@code -Dproxy.port=1234}
     */
    public static int proxyPort() {
        return Integer.parseInt(System.getProperty("proxy.port", "9479"));
    }

    /**
     *
     * @param offset
     * @return {@link #proxyPort()} with added {@literal offset}
     */
    public static int proxyPort(int offset) {
        return proxyPort() + offset;
    }

    /**
     *
     * @return admin port of the toxiproxy service. Defaults to {@literal 8474}. Can be overridden with
     *         {@code -Dproxy.admin.port=8474}
     */
    public static int proxyAdminPort() {
        return Integer.parseInt(System.getProperty("proxy.admin.port", "8474"));
    }

    /**
     *
     * @return port of the mTLS standalone Redis instance (redis-standalone-5-client-cert). Defaults to {@literal 6445}.
     */
    public static int mtlsStandalonePort() {
        return sslPort(2); // 6445
    }

    /**
     *
     * @return port of the mTLS cluster (ssl-test-cluster). Defaults to {@literal 7443}.
     */
    public static int mtlsClusterPort() {
        return 7443;
    }

    /**
     *
     * @return hostname of the modules-enabled (JSON, Search, TimeSeries, Bloom) standalone Redis instance. Defaults to
     *         {@literal 127.0.0.1}. Can be overridden with {@code -Dmodule.host=YourHostName}
     */
    public static String moduleHost() {
        String host = System.getProperty("module.host");
        if (host != null) {
            return host;
        }
        return TestEnvProvider.isActive() ? TestEnvProvider.host() : "127.0.0.1";
    }

    /**
     *
     * @return port of the modules-enabled (JSON, Search, TimeSeries, Bloom) standalone Redis instance. Defaults to
     *         {@literal 16379}. Can be overridden with {@code -Dmodule.port=1234}
     */
    public static int modulePort() {
        String port = System.getProperty("module.port");
        if (port != null) {
            return Integer.parseInt(port);
        }
        return TestEnvProvider.isActive() ? TestEnvProvider.port() : 16379;
    }

    /**
     *
     * @return {@code true} if connections to the default test database require TLS. Always {@code false} unless an external
     *         test environment provider is active and its endpoint is configured for TLS.
     */
    public static boolean tls() {
        return TestEnvProvider.isActive() && TestEnvProvider.tls();
    }

    /**
     *
     * @return {@code true} if an external test environment provider ({@code TEST_ENV_PROVIDER=re}) is active.
     */
    public static boolean isProviderActive() {
        return TestEnvProvider.isActive();
    }

    /**
     * Resolves test endpoint coordinates from an externally provisioned test environment (Redis Enterprise) when
     * {@code TEST_ENV_PROVIDER=re} is set. The endpoint is looked up in {@link Endpoints#DEFAULT} (loaded from
     * {@code REDIS_ENDPOINTS_CONFIG_PATH}) under the name given by {@code RE_DB_NAME} (default {@literal standalone}). Fails
     * fast when the provider is active but the endpoint cannot be resolved.
     */
    private static class TestEnvProvider {

        private static final String PROVIDER_RE = "re";

        private static final String PROVIDER = System.getenv("TEST_ENV_PROVIDER");

        private static volatile Endpoints.Endpoint resolvedEndpoint;

        static boolean isActive() {
            return PROVIDER_RE.equalsIgnoreCase(PROVIDER);
        }

        static String host() {
            Endpoints.Endpoint endpoint = endpoint();
            List<Endpoints.RawEndpoint> rawEndpoints = endpoint.getRawEndpoints();
            if (rawEndpoints != null && !rawEndpoints.isEmpty() && rawEndpoints.get(0).getDnsName() != null) {
                return rawEndpoints.get(0).getDnsName();
            }
            return URI.create(firstUri(endpoint)).getHost();
        }

        static int port() {
            Endpoints.Endpoint endpoint = endpoint();
            List<Endpoints.RawEndpoint> rawEndpoints = endpoint.getRawEndpoints();
            if (rawEndpoints != null && !rawEndpoints.isEmpty() && rawEndpoints.get(0).getPort() != 0) {
                return rawEndpoints.get(0).getPort();
            }
            return URI.create(firstUri(endpoint)).getPort();
        }

        static String username() {
            return endpoint().getUsername();
        }

        static String password() {
            return endpoint().getPassword();
        }

        static boolean tls() {
            return endpoint().isTls();
        }

        private static String firstUri(Endpoints.Endpoint endpoint) {
            List<String> uris = endpoint.getEndpoints();
            if (uris == null || uris.isEmpty()) {
                throw new IllegalStateException(
                        "Endpoint '" + dbName() + "' defines neither raw_endpoints nor endpoints entries");
            }
            return uris.get(0);
        }

        private static String dbName() {
            String dbName = System.getenv("RE_DB_NAME");
            return dbName == null || dbName.isEmpty() ? "standalone" : dbName;
        }

        private static Endpoints.Endpoint endpoint() {
            Endpoints.Endpoint endpoint = resolvedEndpoint;
            if (endpoint == null) {
                endpoint = Endpoints.DEFAULT.getEndpoint(dbName());
                if (endpoint == null) {
                    throw new IllegalStateException("TEST_ENV_PROVIDER=" + PROVIDER + " is set but the endpoint '" + dbName()
                            + "' cannot be resolved. Verify that REDIS_ENDPOINTS_CONFIG_PATH points to an endpoint "
                            + "configuration file containing a database named '" + dbName() + "'");
                }
                resolvedEndpoint = endpoint;
            }
            return endpoint;
        }

    }

}
