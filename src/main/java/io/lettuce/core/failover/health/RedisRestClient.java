package io.lettuce.core.failover.health;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.SslOptions;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.json.JsonArray;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.support.http.HttpClient;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * REST client for Redis Enterprise and Redis Cloud REST API. Provides methods to query database information and check database
 * availability.
 */
class RedisRestClient {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(RedisRestClient.class);

    private static final int DEFAULT_TIMEOUT_MS = 1000;

    public static final String V_1_BDBS_S_AVAILABILITY = "/v1/bdbs/%s/availability";

    public static final String V_1_BDBS = "/v1/bdbs";

    public static final String AVAILABILITY_EXTEND_CHECK = "extend_check";

    public static final String AVAILABILITY_LAG_TOLERANCE_MS = "availability_lag_tolerance_ms";

    private final URI endpoint;

    private final Supplier<RedisCredentials> credentialsSupplier;

    private final HttpClient.ConnectionConfig connectionConfig;

    private final HttpClient httpClient;

    private final AtomicReference<CompletableFuture<HttpClient.HttpConnection>> connectionFutureRef = new AtomicReference<>();

    RedisRestClient(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier, int timeoutMs, SslOptions sslOptions,
            HttpClient httpClient) {
        LettuceAssert.notNull(restEndpoint, "Redis Enterprise REST API restEndpoint must not be null");
        LettuceAssert.notNull(httpClient, "HttpClient must not be null");

        this.endpoint = restEndpoint;
        this.credentialsSupplier = credentialsSupplier;
        this.httpClient = httpClient;

        // Create connection configuration
        HttpClient.ConnectionConfig.Builder configBuilder = HttpClient.ConnectionConfig.builder().connectionTimeout(timeoutMs)
                .readTimeout(timeoutMs);

        if (sslOptions != null) {
            configBuilder.sslOptions(sslOptions);
        }

        this.connectionConfig = configBuilder.build();
    }

    public List<BdbInfo> getBdbs() throws RedisRestException {
        HttpClient.Request request = HttpClient.Request.get(V_1_BDBS).queryParam("fields", "uid,endpoints")
                .headers(createAuthHeaders()).build();

        HttpClient.Response response;
        try {
            response = executeWithRetryAsync(request).join();
        } catch (Exception e) {
            Throwable cause = Exceptions.unwrap(e);
            throw new RedisRestException("Failed to get BDBs: " + cause.getMessage(), cause);
        }

        // Validate response status
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            return readBdbs(response.getResponseBody(StandardCharsets.UTF_8));
        } else {
            throw new RedisRestException("Failed to get BDBs", response.getStatusCode(),
                    response.getResponseBody(StandardCharsets.UTF_8));
        }
    }

    public boolean checkBdbAvailability(Long uid, boolean lagAware) {
        return checkBdbAvailability(uid, lagAware, null);
    }

    public boolean checkBdbAvailability(Long uid, boolean extendedCheckEnabled, Long availabilityLagToleranceMs) {
        String availabilityPath = String.format(V_1_BDBS_S_AVAILABILITY, uid);

        HttpClient.Request.RequestBuilder requestBuilder = HttpClient.Request.get(availabilityPath)
                .headers(createAuthHeaders());

        if (extendedCheckEnabled) {
            // Use extended check with lag validation
            requestBuilder.queryParam(AVAILABILITY_EXTEND_CHECK, "lag");
            if (availabilityLagToleranceMs != null) {
                requestBuilder.queryParam(AVAILABILITY_LAG_TOLERANCE_MS, availabilityLagToleranceMs.toString());
            }
        }

        HttpClient.Request request = requestBuilder.build();

        try {
            HttpClient.Response response = executeWithRetryAsync(request).join();
            return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
        } catch (Exception e) {
            Throwable cause = Exceptions.unwrap(e);
            log.warn("Availability check for {} failed: {}", uid, cause.getMessage());
            return false;
        }
    }

    /**
     * Creates HTTP headers with Basic Authentication if credentials are available.
     *
     * @return a map of HTTP headers, or an empty map if no credentials are configured.
     */
    private Map<String, String> createAuthHeaders() {
        if (credentialsSupplier == null) {
            return Collections.emptyMap();
        }

        RedisCredentials credentials = credentialsSupplier.get();
        if (credentials == null || credentials.getUsername() == null || credentials.getPassword() == null) {
            return Collections.emptyMap();
        }

        String username = credentials.getUsername();
        char[] password = credentials.getPassword();

        // Create Basic Auth header: "Basic base64(username:password)"
        String auth = username + ":" + new String(password);
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    /**
     * Executes an HTTP request with automatic retry on connection failure or retryable HTTP status codes. If the first attempt
     * fails with an IOException (connection failure) or returns a retryable status code (503, 502, 504), the connection is
     * closed and the request is retried once.
     *
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws IOException if both the initial attempt and retry fail with IOException
     */
    private CompletableFuture<HttpClient.Response> executeWithRetryAsync(HttpClient.Request request) {
        return executeRequestAsync(request).handle((response, ex) -> {
            if (ex == null && !isRetryableStatus(response.getStatusCode())) {
                return CompletableFuture.completedFuture(response);
            }
            // Log and retry
            if (ex != null) {
                log.debug("Request failed, retrying after reconnection: {}", ex.getMessage());
            } else {
                log.debug("Received retryable status code {}, retrying after reconnection", response.getStatusCode());
            }
            return closeConnectionAsync().thenCompose(v -> executeRequestAsync(request));
        }).thenCompose(Function.identity());
    }

    /**
     * Executes an HTTP request asynchronously and returns the response.
     *
     * @param request the HTTP request to execute
     * @return a CompletableFuture that completes with the HTTP response
     */
    private CompletableFuture<HttpClient.Response> executeRequestAsync(HttpClient.Request request) {
        return getConnectionAsync().thenCompose(conn -> conn.executeAsync(request));
    }

    /**
     * Determines if an HTTP status code is retryable. Retryable status codes include:
     * <ul>
     * <li>408 Request Timeout - Server didn't receive full request; retry may succeed</li>
     * <li>429 Too Many Requests - Rate limiting; retry after backoff</li>
     * <li>500 Internal Server Error - Server transient failure</li>
     * <li>502 Bad Gateway - Upstream server error</li>
     * <li>503 Service Unavailable - Server temporarily unavailable</li>
     * <li>504 Gateway Timeout - Upstream server timeout</li>
     * </ul>
     *
     * @param statusCode the HTTP status code
     * @return true if the status code is retryable, false otherwise
     */
    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503
                || statusCode == 504;
    }

    /**
     * Gets an HTTP connection asynchronously using the Placeholder Future Pattern. This method is thread-safe and ensures that
     * only one connection attempt is made at a time. The caller is responsible for checking if the connection is active.
     *
     * @return a CompletableFuture that completes with an HttpConnection
     */
    private CompletableFuture<HttpClient.HttpConnection> getConnectionAsync() {
        while (true) {
            CompletableFuture<HttpClient.HttpConnection> existing = connectionFutureRef.get();

            if (existing == null) {
                // No connection - create placeholder first, then CAS, then connect
                CompletableFuture<HttpClient.HttpConnection> placeholder = new CompletableFuture<>();

                if (connectionFutureRef.compareAndSet(null, placeholder)) {
                    // Won the CAS - only this thread calls connectAsync
                    httpClient.connectAsync(endpoint, connectionConfig).whenComplete((conn, ex) -> {
                        if (ex != null) {
                            // Connection failed - clear the placeholder and propagate error
                            connectionFutureRef.compareAndSet(placeholder, null);
                            log.error("Failed to establish HTTP connection to {}", endpoint, ex);
                            placeholder.completeExceptionally(ex);
                        } else {
                            log.debug("Established HTTP connection to {}", endpoint);
                            placeholder.complete(conn);
                        }
                    });
                    return placeholder;
                }
                // CAS failed - another thread won, loop back to get their placeholder
                continue;
            }

            // Return existing connection - caller checks isActive()
            return existing;
        }
    }

    /**
     * Closes the current HTTP connection asynchronously if it exists.
     *
     * @return a CompletableFuture that completes when the connection is closed
     */
    private CompletableFuture<Void> closeConnectionAsync() {
        CompletableFuture<HttpClient.HttpConnection> existing = connectionFutureRef.getAndSet(null);
        if (existing == null) {
            return CompletableFuture.completedFuture(null);
        }

        return existing.handle((conn, ex) -> {
            if (conn != null) {
                return conn.closeAsync().exceptionally(closeEx -> {
                    log.warn("Error closing HTTP connection", closeEx);
                    return null;
                });
            }
            return CompletableFuture.<Void> completedFuture(null);
        }).thenCompose(Function.identity());
    }

    /**
     * Closes the HTTP connection. Should be called when this RedisRestClient instance is no longer needed.
     */
    public void close() {
        closeConnectionAsync().join();
    }

    /**
     * Closes the HTTP connection asynchronously. Should be called when this RedisRestClient instance is no longer needed.
     *
     * @return a CompletableFuture that completes when the connection is closed
     */
    public CompletableFuture<Void> closeAsync() {
        return closeConnectionAsync();
    }

    /**
     * Parses the response body and extracts BDB information including endpoints.
     * 
     * @param responseBody the JSON response containing BDBs with endpoints
     * @return list of BDB information objects
     */
    private List<BdbInfo> readBdbs(String responseBody) {
        JsonParser jsonParser = ClientOptions.DEFAULT_JSON_PARSER.get();
        JsonArray bdbs = jsonParser.createJsonValue(responseBody).asJsonArray();
        List<BdbInfo> bdbInfoList = new ArrayList<>();

        for (JsonValue bdbElement : bdbs.asList()) {
            if (!bdbElement.isJsonObject()) {
                continue;
            }

            JsonObject bdb = bdbElement.asJsonObject();
            if (bdb.get("uid") == null) {
                continue;
            }

            Number bdbIdValue = bdb.get("uid").asNumber();
            Long bdbId = bdbIdValue != null ? bdbIdValue.longValue() : null;

            List<EndpointInfo> endpoints = new ArrayList<>();

            JsonValue endpointsJson = bdb.get("endpoints");
            if (endpointsJson != null && endpointsJson.isJsonArray()) {
                JsonArray endpointsArray = endpointsJson.asJsonArray();

                for (JsonValue endpointElement : endpointsArray.asList()) {
                    if (!endpointElement.isJsonObject()) {
                        continue;
                    }

                    JsonObject endpoint = endpointElement.asJsonObject();

                    // Extract addr array
                    List<String> addrList = new ArrayList<>();
                    JsonValue jsonAddr = endpoint.get("addr");
                    if (jsonAddr != null && jsonAddr.isJsonArray()) {
                        JsonArray addresses = jsonAddr.asJsonArray();
                        for (JsonValue addrElement : addresses.asList()) {
                            if (addrElement.isString()) {
                                addrList.add(addrElement.asString());
                            }
                        }
                    }

                    // Extract other fields
                    String dnsName = endpoint.get("dns_name") != null ? endpoint.get("dns_name").asString() : null;
                    Integer port = endpoint.get("port") != null ? endpoint.get("port").asNumber().intValue() : null;
                    String endpointUid = endpoint.get("uid") != null ? endpoint.get("uid").asString() : null;

                    endpoints.add(new EndpointInfo(addrList, dnsName, port, endpointUid));
                }
            }

            bdbInfoList.add(new BdbInfo(bdbId, endpoints));
        }

        return bdbInfoList;
    }

    /**
     * Information about a Redis Enterprise BDB (database) including its endpoints.
     */
    static class BdbInfo {

        private final Long uid;

        private final List<EndpointInfo> endpoints;

        BdbInfo(Long uid, List<EndpointInfo> endpoints) {
            this.uid = uid;
            this.endpoints = endpoints;
        }

        Long getUid() {
            return uid;
        }

        List<EndpointInfo> getEndpoints() {
            return endpoints;
        }

        /**
         * Check if this BDB matches the given database host by comparing endpoints.
         *
         * @param host the database host to match
         * @return true if this BDB has an endpoint matching the host
         */
        boolean matches(String host) {
            for (EndpointInfo endpoint : this.endpoints) {
                // First check dns_name
                if (host.equals(endpoint.getDnsName())) {
                    return true;
                }

                // Then check addr array for IP addresses
                if (endpoint.getAddr() != null) {
                    for (String addr : endpoint.getAddr()) {
                        if (host.equals(addr)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "BdbInfo{" + "uid='" + uid + '\'' + ", endpoints=" + endpoints + '}';
        }

    }

    /**
     * Information about a Redis Enterprise BDB endpoint.
     */
    static class EndpointInfo {

        private final List<String> addr;

        private final String dnsName;

        private final Integer port;

        private final String uid;

        EndpointInfo(List<String> addr, String dnsName, Integer port, String uid) {
            this.addr = addr;
            this.dnsName = dnsName;
            this.port = port;
            this.uid = uid;
        }

        List<String> getAddr() {
            return addr;
        }

        String getDnsName() {
            return dnsName;
        }

        Integer getPort() {
            return port;
        }

        String getUid() {
            return uid;
        }

        @Override
        public String toString() {
            return "EndpointInfo{" + "addr=" + addr + ", dnsName='" + dnsName + '\'' + ", port=" + port + ", uid='" + uid + '\''
                    + '}';
        }

    }

}
