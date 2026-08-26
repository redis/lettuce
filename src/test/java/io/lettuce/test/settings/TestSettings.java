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
 * Endpoint coordinates are resolved from the {@link Endpoints} configuration: the file referenced by the
 * {@code REDIS_ENDPOINTS_CONFIG_PATH} environment variable, or the bundled {@code endpoints.json} test resource describing the
 * local Docker environment when the variable is not set. The database name is selected by the {@code RE_DB_NAME} environment
 * variable (defaults to {@literal standalone}). Explicit system properties always take precedence over the endpoint
 * configuration. When {@code TEST_ENV_PROVIDER=re} is set (externally provisioned Redis Enterprise databases), resolution fails
 * fast instead of falling back to the local defaults.
 *
 * @author Mark Paluch
 * @author Tugdual Grall
 */
public class TestSettings {

    private static final String DEFAULT_DB_NAME = "standalone";

    private static final String MODULES_DB_NAME = "standalone-modules";

    private static final String CLUSTER_DB_NAME = "cluster";

    private TestSettings() {
    }

    /**
     *
     * @return hostname of your redis instance. Resolved from the endpoint configuration, defaults to {@literal localhost}. Can
     *         be overriden with {@code -Dhost=YourHostName}
     */
    public static String host() {
        String host = System.getProperty("host");
        if (host != null) {
            return host;
        }
        Endpoints.Endpoint endpoint = endpoint();
        if (endpoint != null) {
            return EndpointResolver.host(endpoint);
        }
        failFastIfExternallyProvisioned(dbName());
        return "localhost";
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
     * @return default username of your redis instance. Resolved from the endpoint configuration, defaults to
     *         {@literal default}. Can be overridden with {@code -Dusername=SampleUsername}
     */
    public static String username() {
        String username = System.getProperty("username");
        if (username != null) {
            return username;
        }
        Endpoints.Endpoint endpoint = endpoint();
        if (endpoint != null && endpoint.getUsername() != null && !endpoint.getUsername().isEmpty()) {
            return endpoint.getUsername();
        }
        return "default";
    }

    /**
     *
     * @return password of your redis instance. Resolved from the endpoint configuration, defaults to {@literal foobared}. Can
     *         be overridden with {@code -Dpassword=YourPassword}
     */
    public static CharSequence password() {
        String password = System.getProperty("password");
        if (password != null) {
            return password;
        }
        Endpoints.Endpoint endpoint = endpoint();
        if (endpoint != null && endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
            return endpoint.getPassword();
        }
        return "foobared";
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
     * @return port of your redis instance. Resolved from the endpoint configuration, defaults to {@literal 6479}. Can be
     *         overriden with {@code -Dport=1234}
     */
    public static int port() {
        String port = System.getProperty("port");
        if (port != null) {
            return Integer.parseInt(port);
        }
        Endpoints.Endpoint endpoint = endpoint();
        if (endpoint != null) {
            return EndpointResolver.port(endpoint);
        }
        failFastIfExternallyProvisioned(dbName());
        return 6479;
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
     * @return {@link #localBasePort()} with added {@literal 500}
     */
    public static int nonexistentPort() {
        return localBasePort() + 500;
    }

    /**
     * Port offsets address the local Docker test topology only and are never derived from an endpoint configuration.
     *
     * @param offset
     * @return the local base port (6479, can be overridden with {@code -Dport=1234}) with added {@literal offset}
     */
    public static int port(int offset) {
        return localBasePort() + offset;
    }

    private static int localBasePort() {
        return Integer.parseInt(System.getProperty("port", "6479"));
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
     * @return hostname of the modules-enabled (JSON, Search, TimeSeries, Bloom) standalone Redis instance. Resolved from the
     *         {@literal standalone-modules} endpoint (falling back to the default endpoint), defaults to {@literal 127.0.0.1}.
     *         Can be overridden with {@code -Dmodule.host=YourHostName}
     */
    public static String moduleHost() {
        String host = System.getProperty("module.host");
        if (host != null) {
            return host;
        }
        Endpoints.Endpoint endpoint = moduleEndpoint();
        if (endpoint != null) {
            return EndpointResolver.host(endpoint);
        }
        failFastIfExternallyProvisioned(MODULES_DB_NAME);
        return "127.0.0.1";
    }

    /**
     *
     * @return port of the modules-enabled (JSON, Search, TimeSeries, Bloom) standalone Redis instance. Resolved from the
     *         {@literal standalone-modules} endpoint (falling back to the default endpoint), defaults to {@literal 16379}. Can
     *         be overridden with {@code -Dmodule.port=1234}
     */
    public static int modulePort() {
        String port = System.getProperty("module.port");
        if (port != null) {
            return Integer.parseInt(port);
        }
        Endpoints.Endpoint endpoint = moduleEndpoint();
        if (endpoint != null) {
            return EndpointResolver.port(endpoint);
        }
        failFastIfExternallyProvisioned(MODULES_DB_NAME);
        return 16379;
    }

    /**
     *
     * @return {@code true} if connections to the default test database require TLS. Resolved from the endpoint configuration,
     *         defaults to {@code false}. Can be overridden with {@code -Dtls=true}
     */
    public static boolean tls() {
        String tls = System.getProperty("tls");
        if (tls != null) {
            return Boolean.parseBoolean(tls);
        }
        Endpoints.Endpoint endpoint = endpoint();
        return endpoint != null && endpoint.isTls();
    }

    /**
     *
     * @return the endpoint of the default test database ({@code RE_DB_NAME}, defaults to {@literal standalone}) or {@code null}
     *         if the endpoint configuration does not define it.
     */
    public static Endpoints.Endpoint endpoint() {
        return EndpointResolver.endpoint(dbName());
    }

    /**
     *
     * @return the endpoint of the modules-enabled test database ({@literal standalone-modules}, falling back to the default
     *         endpoint) or {@code null} if the endpoint configuration defines neither.
     */
    public static Endpoints.Endpoint moduleEndpoint() {
        Endpoints.Endpoint endpoint = EndpointResolver.endpoint(MODULES_DB_NAME);
        return endpoint != null ? endpoint : endpoint();
    }

    /**
     *
     * @return the endpoint of the Redis Cluster test database ({@literal cluster}) or {@code null} if the endpoint
     *         configuration does not define it. Fails fast when an external test environment provider is active.
     */
    public static Endpoints.Endpoint clusterEndpoint() {
        Endpoints.Endpoint endpoint = EndpointResolver.endpoint(CLUSTER_DB_NAME);
        if (endpoint == null) {
            failFastIfExternallyProvisioned(CLUSTER_DB_NAME);
        }
        return endpoint;
    }

    private static String dbName() {
        String dbName = System.getenv("RE_DB_NAME");
        return dbName == null || dbName.isEmpty() ? DEFAULT_DB_NAME : dbName;
    }

    private static void failFastIfExternallyProvisioned(String name) {
        String provider = System.getenv("TEST_ENV_PROVIDER");
        if ("re".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("TEST_ENV_PROVIDER=" + provider + " is set but the endpoint '" + name
                    + "' cannot be resolved. Verify that REDIS_ENDPOINTS_CONFIG_PATH points to an endpoint "
                    + "configuration file containing a database named '" + name + "'");
        }
    }

    /**
     * Resolves host and port coordinates from an {@link Endpoints.Endpoint}, preferring raw endpoint metadata over the URI
     * list.
     */
    private static class EndpointResolver {

        static Endpoints.Endpoint endpoint(String name) {
            return Endpoints.DEFAULT.getEndpoint(name);
        }

        static String host(Endpoints.Endpoint endpoint) {
            List<Endpoints.RawEndpoint> rawEndpoints = endpoint.getRawEndpoints();
            if (rawEndpoints != null && !rawEndpoints.isEmpty()) {
                Endpoints.RawEndpoint rawEndpoint = rawEndpoints.get(0);
                if (rawEndpoint.getDnsName() != null && !rawEndpoint.getDnsName().isEmpty()) {
                    return rawEndpoint.getDnsName();
                }
                if (rawEndpoint.getAddr() != null && !rawEndpoint.getAddr().isEmpty()) {
                    return rawEndpoint.getAddr().get(0);
                }
            }
            return URI.create(firstUri(endpoint)).getHost();
        }

        static int port(Endpoints.Endpoint endpoint) {
            List<Endpoints.RawEndpoint> rawEndpoints = endpoint.getRawEndpoints();
            if (rawEndpoints != null && !rawEndpoints.isEmpty() && rawEndpoints.get(0).getPort() != 0) {
                return rawEndpoints.get(0).getPort();
            }
            return URI.create(firstUri(endpoint)).getPort();
        }

        private static String firstUri(Endpoints.Endpoint endpoint) {
            List<String> uris = endpoint.getEndpoints();
            if (uris == null || uris.isEmpty()) {
                throw new IllegalStateException("Endpoint defines neither raw_endpoints nor endpoints entries");
            }
            return uris.get(0);
        }

    }

}
