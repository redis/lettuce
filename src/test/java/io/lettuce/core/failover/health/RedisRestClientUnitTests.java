/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * 
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.failover.health;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.support.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link RedisRestClient}.
 */
@ExtendWith(MockitoExtension.class)
@Tag(UNIT_TEST)
class RedisRestClientUnitTests {

    private static final String SAMPLE_BDBS_RESPONSE = loadResource("sample-bdbs-response.json");

    private static final URI REST_ENDPOINT = URI.create("https://localhost:9443");

    private static String loadResource(String resourceName) {
        try (InputStream is = RedisRestClientUnitTests.class.getResourceAsStream(resourceName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourceName, e);
        }
    }

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClient.HttpConnection httpConnection;

    @Mock
    private HttpClient.Response response;

    @Mock
    private Supplier<RedisCredentials> credentialsSupplier;

    private RedisRestClient redisRestClient;

    @BeforeEach
    void setUp() {
        when(httpClient.connectAsync(any(URI.class), any(HttpClient.ConnectionConfig.class)))
                .thenReturn(CompletableFuture.completedFuture(httpConnection));
        redisRestClient = new RedisRestClient(REST_ENDPOINT, credentialsSupplier, 1000, null, httpClient);
    }

    @Test
    void getBdbsParsesBdbsCorrectly() throws Exception {
        // Setup mock response
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody(StandardCharsets.UTF_8)).thenReturn(SAMPLE_BDBS_RESPONSE);

        // Execute
        List<RedisRestClient.BdbInfo> bdbs = redisRestClient.getBdbs();

        // Verify 3 BDBs are returned with correct UIDs
        assertNotNull(bdbs);
        assertEquals(3, bdbs.size());
        assertTrue(bdbs.stream().anyMatch(b -> b.getUid() == 1));
        assertTrue(bdbs.stream().anyMatch(b -> b.getUid() == 2));
        assertTrue(bdbs.stream().anyMatch(b -> b.getUid() == 3));

        // Verify BDB with uid "1" has correct endpoint details
        RedisRestClient.BdbInfo bdb1 = bdbs.stream().filter(b -> b.getUid() == 1).findFirst().orElse(null);
        assertNotNull(bdb1);
        assertEquals(1, bdb1.getEndpoints().size());

        RedisRestClient.EndpointInfo endpoint = bdb1.getEndpoints().get(0);
        assertEquals("redis-11035.test.example.com", endpoint.getDnsName());
        assertEquals(11035, endpoint.getPort());
        assertEquals("1:1", endpoint.getUid());
        assertTrue(endpoint.getAddr().contains("10.0.0.1"));
    }

    @Test
    void bdbInfoMatchesByDnsName() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody(StandardCharsets.UTF_8)).thenReturn(SAMPLE_BDBS_RESPONSE);

        List<RedisRestClient.BdbInfo> bdbs = redisRestClient.getBdbs();

        // Test matches() by DNS name
        RedisRestClient.BdbInfo matchingBdb = bdbs.stream().filter(bdb -> bdb.matches("redis-12001.test.example.com"))
                .findFirst().orElse(null);
        assertNotNull(matchingBdb);
        assertEquals(3L, matchingBdb.getUid());
    }

    @Test
    void bdbInfoMatchesByIpAddress() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody(StandardCharsets.UTF_8)).thenReturn(SAMPLE_BDBS_RESPONSE);

        List<RedisRestClient.BdbInfo> bdbs = redisRestClient.getBdbs();

        // Test matches() by IP address
        RedisRestClient.BdbInfo matchingBdb = bdbs.stream().filter(bdb -> bdb.matches("10.0.0.3")).findFirst().orElse(null);
        assertNotNull(matchingBdb);
        assertEquals(2L, matchingBdb.getUid());
    }

    @Test
    void getBdbsThrowsExceptionOnNonSuccessStatus() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(401);
        when(response.getResponseBody(StandardCharsets.UTF_8)).thenReturn("Unauthorized");

        assertThrows(RedisRestException.class, () -> redisRestClient.getBdbs());
    }

    @Test
    void getBdbsThrowsExceptionOnIOException() throws Exception {
        CompletableFuture<HttpClient.Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IOException("Connection failed"));
        when(httpConnection.executeAsync(any(HttpClient.Request.class))).thenReturn(failedFuture);

        assertThrows(RedisRestException.class, () -> redisRestClient.getBdbs());
    }

    @Test
    void checkBdbAvailabilityReturnsTrueOnSuccess() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);

        boolean result = redisRestClient.checkBdbAvailability(1L, false, null);

        assertTrue(result);
    }

    @Test
    void checkBdbAvailabilityReturnsFalseOnNonSuccessStatus() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(503);

        boolean result = redisRestClient.checkBdbAvailability(1L, false, null);

        assertFalse(result);
    }

    @Test
    void checkBdbAvailabilityReturnsFalseOnIOException() throws Exception {
        CompletableFuture<HttpClient.Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IOException("Connection failed"));
        when(httpConnection.executeAsync(any(HttpClient.Request.class))).thenReturn(failedFuture);

        boolean result = redisRestClient.checkBdbAvailability(1L, false, null);

        assertFalse(result);
    }

    @Test
    void checkBdbAvailabilityWithoutExtendedCheck() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);

        redisRestClient.checkBdbAvailability(42L, false, null);

        ArgumentCaptor<HttpClient.Request> requestCaptor = ArgumentCaptor.forClass(HttpClient.Request.class);
        verify(httpConnection).executeAsync(requestCaptor.capture());

        HttpClient.Request capturedRequest = requestCaptor.getValue();
        assertEquals("/v1/bdbs/42/availability", capturedRequest.getPath());
        assertTrue(capturedRequest.getQueryParams().isEmpty());
    }

    @Test
    void checkBdbAvailabilityWithExtendedCheckWithoutLagTolerance() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);

        redisRestClient.checkBdbAvailability(42L, true, null);

        ArgumentCaptor<HttpClient.Request> requestCaptor = ArgumentCaptor.forClass(HttpClient.Request.class);
        verify(httpConnection).executeAsync(requestCaptor.capture());

        HttpClient.Request capturedRequest = requestCaptor.getValue();
        assertEquals("/v1/bdbs/42/availability", capturedRequest.getPath());
        assertEquals(1, capturedRequest.getQueryParams().size());
        assertEquals("lag", capturedRequest.getQueryParams().get("extend_check"));
    }

    @Test
    void checkBdbAvailabilityWithExtendedCheckAndLagTolerance() throws Exception {
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(response.getStatusCode()).thenReturn(200);

        redisRestClient.checkBdbAvailability(42L, true, 5000L);

        ArgumentCaptor<HttpClient.Request> requestCaptor = ArgumentCaptor.forClass(HttpClient.Request.class);
        verify(httpConnection).executeAsync(requestCaptor.capture());

        HttpClient.Request capturedRequest = requestCaptor.getValue();
        assertEquals("/v1/bdbs/42/availability", capturedRequest.getPath());
        assertEquals(2, capturedRequest.getQueryParams().size());
        assertEquals("lag", capturedRequest.getQueryParams().get("extend_check"));
        assertEquals("5000", capturedRequest.getQueryParams().get("availability_lag_tolerance_ms"));
    }

    @Test
    void httpConnectionIsReestablishedOnUnexpectedClose() throws Exception {
        // Setup for requests
        when(httpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(httpConnection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(response.getStatusCode()).thenReturn(200);

        // First request - connection will be established (httpConnection is null initially)
        redisRestClient.checkBdbAvailability(1L, false, null);

        // Verify connect was called once for the first request
        verify(httpClient, times(1)).connectAsync(any(URI.class), any(HttpClient.ConnectionConfig.class));

        // Simulate connection becoming inactive - executeAsync() returns failed future with IOException
        // (this mimics what NettyHttpConnection.executeAsync() does when channel.isActive() returns false)
        CompletableFuture<HttpClient.Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IOException("Connection is not active"));
        when(httpConnection.executeAsync(any(HttpClient.Request.class))).thenReturn(failedFuture);

        // Setup reconnected connection to succeed
        HttpClient.HttpConnection reconnectedHttpConnection = mock(HttpClient.HttpConnection.class);
        when(httpClient.connectAsync(any(URI.class), any(HttpClient.ConnectionConfig.class)))
                .thenReturn(CompletableFuture.completedFuture(reconnectedHttpConnection));
        when(reconnectedHttpConnection.executeAsync(any(HttpClient.Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Second request - should trigger reconnection after executeAsync() fails
        redisRestClient.checkBdbAvailability(1L, false, null);

        // Verify connect was called twice (first request + reestablishment after failure)
        verify(httpClient, times(2)).connectAsync(any(URI.class), any(HttpClient.ConnectionConfig.class));
    }

}
