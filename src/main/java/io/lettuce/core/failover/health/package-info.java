/**
 * Health check infrastructure for automatic failover in multi-database Redis connections.
 *
 * <h2>Health Check Strategies</h2>
 * <p>
 * This package provides a pluggable health check system for monitoring Redis database endpoints. Custom health check strategies
 * can be implemented by extending {@link io.lettuce.core.failover.health.HealthCheckStrategy}.
 * </p>
 *
 * <h3>Built-in Strategies</h3>
 * <ul>
 * <li>{@link io.lettuce.core.failover.health.PingStrategy} - Uses Redis PING command</li>
 * </ul>
 *
 * <h3>Example: Custom HTTP Health Check</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     public class HttpHealthCheckStrategy extends AbstractHealthCheckStrategy {
 *
 *         private final String healthCheckUrl;
 *
 *         public HttpHealthCheckStrategy(Config config, String healthCheckUrl) {
 *             super(config);
 *             this.healthCheckUrl = healthCheckUrl;
 *         }
 *
 *         &#64;Override
 *         public HealthStatus doHealthCheck(RedisURI endpoint) {
 *             try {
 *                 HttpURLConnection conn = (HttpURLConnection) new URL(healthCheckUrl).openConnection();
 *                 int code = conn.getResponseCode();
 *                 conn.disconnect();
 *                 return (code >= 200 && code < 300) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
 *             } catch (Exception e) {
 *                 return HealthStatus.UNHEALTHY;
 *             }
 *         }
 * 
 *     }
 *
 *     // Usage:
 *     HealthCheckStrategySupplier supplier = (uri, factory) -> new HttpHealthCheckStrategy(HealthCheckStrategy.Config.create(),
 *             "http://health-endpoint/check");
 *     DatabaseConfig config = DatabaseConfig.builder(redisURI).healthCheckStrategySupplier(supplier).build();
 * }
 * </pre>
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
package io.lettuce.core.failover.health;
