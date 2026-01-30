/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * 
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.failover.health;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.support.http.HttpClient;
import io.lettuce.core.support.http.HttpClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Health check strategy that uses the Redis Enterprise REST API database availability endpoint.
 * <p>
 * This strategy uses the {@code /v1/bdbs/<database_id>/availability} REST API to verify whether the database is available to
 * perform read and write operations and can respond to queries from client applications.
 * </p>
 *
 * <h2>Health Check Process</h2>
 * <ol>
 * <li>Resolves the BDB (database) ID by matching the endpoint host against known database endpoints</li>
 * <li>Caches the BDB ID for subsequent health checks to avoid repeated lookups</li>
 * <li>Calls the availability API to check if the database is healthy</li>
 * </ol>
 *
 * <h2>Lag-Aware Availability Checks</h2>
 * <p>
 * When {@link Config#isExtendedCheckEnabled()} is {@code true}, performs a lag-aware availability check using the
 * {@code extend_check=lag} query parameter.
 * </p>
 * <p>
 * The lag tolerance threshold can be configured via {@link Config#getAvailabilityLagTolerance()} (default: 100ms).
 * </p>
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 * @see <a href="https://redis.io/docs/latest/operate/rs/monitoring/db-availability/">Redis Enterprise Database Availability
 *      API</a>
 */
public class LagAwareStrategy implements HealthCheckStrategy {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(LagAwareStrategy.class);

    private final LagAwareStrategy.Config config;

    private final RedisRestClient redisRestClient;

    private final HttpClient httpClient;

    private Long cachedBdbId;

    public LagAwareStrategy(LagAwareStrategy.Config config) {
        this(config, null);
    }

    public LagAwareStrategy(LagAwareStrategy.Config config, HttpClient httpClient) {
        this.config = config;

        if (httpClient != null) {
            this.httpClient = httpClient;
        } else {
            this.httpClient = HttpClientResources.get();
        }

        this.redisRestClient = createRedisRestClient(config, this.httpClient);
    }

    @Override
    public int getInterval() {
        return config.interval;
    }

    @Override
    public int getTimeout() {
        return config.timeout;
    }

    @Override
    public int getNumProbes() {
        return config.getNumProbes();
    }

    @Override
    public ProbingPolicy getPolicy() {
        return config.getPolicy();
    }

    @Override
    public int getDelayInBetweenProbes() {
        return config.getDelayInBetweenProbes();
    }

    @Override
    public HealthStatus doHealthCheck(RedisURI endpoint) {
        try {
            Long bdbId = cachedBdbId;
            if (bdbId == null) {
                // Try to find BDB that matches the database host
                String dbHost = endpoint.getHost();
                List<RedisRestClient.BdbInfo> bdbs = redisRestClient.getBdbs();
                RedisRestClient.BdbInfo matchingBdb = bdbs.stream().filter(bdb -> bdb.matches(dbHost)).findFirst().orElse(null);

                if (matchingBdb == null) {
                    String msg = String.format("No BDB found matching host '%s' for health check", dbHost);
                    log.warn(msg);
                    throw new RedisException(msg);
                } else {
                    bdbId = matchingBdb.getUid();
                    log.debug("Found matching BDB '{}' for host '{}'", bdbId, dbHost);
                    cachedBdbId = bdbId;
                }
            }
            if (this.config.isExtendedCheckEnabled()) {
                // Use extended check with lag validation
                if (redisRestClient.checkBdbAvailability(bdbId, true, this.config.getAvailabilityLagTolerance().toMillis())) {
                    return HealthStatus.HEALTHY;
                }
            } else {
                // Use standard datapath validation only
                if (redisRestClient.checkBdbAvailability(bdbId, false)) {
                    return HealthStatus.HEALTHY;
                }
            }
        } catch (Exception e) {
            log.error("Error while checking database availability", e);
            cachedBdbId = null;
            throw new RedisException("Error while checking availability", e);
        }
        return HealthStatus.UNHEALTHY;
    }

    @Override
    public void close() {
        if (redisRestClient != null) {
            redisRestClient.close();
        }
    }

    RedisRestClient createRedisRestClient(LagAwareStrategy.Config config, HttpClient httpClient) {
        return new RedisRestClient(config.getRestEndpoint(), config.getCredentialsSupplier(), config.getTimeout(),
                config.getSslOptions(), this.httpClient);
    }

    HttpClient getHttpClient() {
        return httpClient;
    }

    public static class Config extends HealthCheckStrategy.Config {

        public static final boolean EXTENDED_CHECK_DEFAULT = true;

        public static final Duration AVAILABILITY_LAG_TOLERANCE_DEFAULT = Duration.ofMillis(5000);

        private final URI restEndpoint;

        private final Supplier<RedisCredentials> credentialsSupplier;

        // SSL configuration for HTTPS connections to Redis Enterprise REST API
        private final SslOptions sslOptions;

        // Maximum acceptable lag in milliseconds (default: 5000);
        private final Duration availability_lag_tolerance;

        // Enable extended lag checking (default: true - performs lag validation in addition to standard
        // datapath validation )
        private final boolean extendedCheckEnabled;

        public Config(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
            this(builder(restEndpoint, credentialsSupplier).availabilityLagTolerance(AVAILABILITY_LAG_TOLERANCE_DEFAULT)
                    .extendedCheckEnabled(EXTENDED_CHECK_DEFAULT));
        }

        private Config(Builder builder) {
            super(builder);

            this.restEndpoint = builder.restEndpoint;
            this.credentialsSupplier = builder.credentialsSupplier;
            this.sslOptions = builder.sslOptions;
            this.availability_lag_tolerance = builder.availabilityLagTolerance;
            this.extendedCheckEnabled = builder.extendedCheckEnabled;
        }

        public URI getRestEndpoint() {
            return restEndpoint;
        }

        public Supplier<RedisCredentials> getCredentialsSupplier() {
            return credentialsSupplier;
        }

        public SslOptions getSslOptions() {
            return sslOptions;
        }

        public Duration getAvailabilityLagTolerance() {
            return availability_lag_tolerance;
        }

        public boolean isExtendedCheckEnabled() {
            return extendedCheckEnabled;
        }

        /**
         * Create a new builder for LagAwareStrategy.Config.
         *
         * @param restEndpoint the Redis Enterprise REST API endpoint
         * @param credentialsSupplier the credentials supplier
         * @return a new ConfigBuilder instance
         */
        public static Builder builder(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return builder().restEndpoint(restEndpoint).credentialsSupplier(credentialsSupplier);
        }

        /**
         * Use {@link Config#builder(URI, Supplier)} instead.
         *
         * @return a new Builder instance
         */
        public static Builder builder() {
            return new LagAwareStrategy.Config.Builder();
        }

        /**
         * Create a new Config instance with default values.
         * <p>
         * Extended checks like lag validation is enabled by default. With a default lag tolerance of 100ms. To perform only
         * standard datapath validation, use {@link #databaseAvailability(URI, Supplier)}. To configure a custom lag tolerance,
         * use {@link #lagAwareWithTolerance(URI, Supplier, Duration)}
         * </p>
         */
        public static Config create(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return builder(restEndpoint, credentialsSupplier).build();
        }

        /**
         * Perform standard datapath validation only.
         * <p>
         * Extended checks like lag validation is disabled by default. To enable extended checks, use
         * {@link #lagAware(URI, Supplier)} or {@link #lagAwareWithTolerance(URI, Supplier, Duration)}
         * </p>
         */
        public static Config databaseAvailability(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return builder(restEndpoint, credentialsSupplier).extendedCheckEnabled(false).build();
        }

        /**
         * Perform standard datapath validation and lag validation using the default lag tolerance.
         * <p>
         * To configure a custom lag tolerance, use {@link #lagAwareWithTolerance(URI, Supplier, Duration)}
         * </p>
         */
        public static Config lagAware(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier) {
            return builder(restEndpoint, credentialsSupplier).extendedCheckEnabled(true).build();
        }

        /**
         * Perform standard datapath validation and lag validation using the specified lag tolerance.
         */
        public static Config lagAwareWithTolerance(URI restEndpoint, Supplier<RedisCredentials> credentialsSupplier,
                Duration availabilityLagTolerance) {
            return builder(restEndpoint, credentialsSupplier).extendedCheckEnabled(true)
                    .availabilityLagTolerance(availabilityLagTolerance).build();
        }

        /**
         * Builder for LagAwareStrategy.Config.
         */
        public static class Builder extends HealthCheckStrategy.Config.Builder<Builder, Config> {

            private URI restEndpoint;

            private Supplier<RedisCredentials> credentialsSupplier;

            // SSL configuration for HTTPS connections
            private SslOptions sslOptions;

            // Maximum acceptable lag in milliseconds (default: 100);
            private Duration availabilityLagTolerance = AVAILABILITY_LAG_TOLERANCE_DEFAULT;

            // Enable extended lag checking
            private boolean extendedCheckEnabled = EXTENDED_CHECK_DEFAULT;

            private Builder() {
            }

            public Builder restEndpoint(URI restEndpoint) {
                LettuceAssert.notNull(restEndpoint, "Redis Enterprise REST API endpoint must not be null");

                this.restEndpoint = restEndpoint;
                return this;
            }

            public Builder credentialsSupplier(Supplier<RedisCredentials> credentialsSupplier) {
                LettuceAssert.notNull(credentialsSupplier, "Credentials supplier must not be null");

                this.credentialsSupplier = credentialsSupplier;
                return this;
            }

            /**
             * Set SSL options for HTTPS connections to Redis Enterprise REST API. This allows configuration of custom
             * truststore, keystore, and SSL parameters for secure connections.
             *
             * @param sslOptions the SSL configuration options
             * @return this builder
             */
            public Builder sslOptions(SslOptions sslOptions) {
                LettuceAssert.notNull(sslOptions, "SSL options must not be null");

                this.sslOptions = sslOptions;
                return this;
            }

            /**
             * Set the maximum acceptable lag in milliseconds.
             *
             * @param availabilityLagTolerance the lag tolerance in milliseconds (default: 100)
             * @return this builder
             */
            public Builder availabilityLagTolerance(Duration availabilityLagTolerance) {
                LettuceAssert.notNull(availabilityLagTolerance, "Availability lag tolerance must not be null");
                LettuceAssert.isTrue(availabilityLagTolerance.toMillis() > 0,
                        "Availability lag tolerance must be greater than zero");

                this.availabilityLagTolerance = availabilityLagTolerance;
                return this;
            }

            /**
             * Enable extended lag checking. When enabled, performs lag validation in addition to standard datapath validation.
             * When disabled performs only standard datapath validation - all slots are available.
             * 
             * @param extendedCheckEnabled true to enable extended lag checking (default: false)
             * @return this builder
             */
            public Builder extendedCheckEnabled(boolean extendedCheckEnabled) {
                this.extendedCheckEnabled = extendedCheckEnabled;
                return this;
            }

            /**
             * Build the Config instance.
             * 
             * @return a new Config instance
             */
            @Override
            public Config build() {
                LettuceAssert.notNull(restEndpoint, "Redis Enterprise REST API endpoint must not be null");

                return new Config(this);
            }

        }

    }

}
