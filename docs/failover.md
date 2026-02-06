# Automatic Failover and Failback with Lettuce

!!! warning "Experimental Feature"
    This feature is experimental and may change in future versions.

Lettuce supports automatic failover and failback for your Redis deployments through the `MultiDbClient`. This is useful when:

1. You have more than one Redis deployment (e.g., two independent Redis servers or multiple Redis databases replicated across active-active clusters).
2. You want your application to connect to and use one deployment at a time.
3. You want your application to fail over to the next available deployment if the current deployment becomes unavailable.
4. You want your application to fail back to the original deployment when it becomes available again.

Lettuce will fail over to a subsequent Redis deployment after the circuit breaker detects failures exceeding the configured threshold. In the background, Lettuce executes health checks to determine when a Redis deployment is available again. When this occurs, Lettuce will fail back to the original deployment.

## Basic Usage

To configure Lettuce for failover, you specify a weighted list of Redis databases. Lettuce will connect to the Redis database with the highest weight. If the highest-weighted database becomes unavailable, Lettuce will attempt to connect to the database with the next highest weight.

Suppose you run two Redis deployments: `redis-east` and `redis-west`. You want your application to first connect to `redis-east`. If `redis-east` becomes unavailable, you want your application to connect to `redis-west`.

```java
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.DatabaseConfig;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.MultiDbOptions;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;


// Configure databases with weights (higher weight = higher priority)
// redis-east
RedisURI eastUri = RedisURI.builder()
        .withHost("redis-east.example.com")
        .withPort(6379)
        .withPassword("secret".toCharArray())
        .build();

DatabaseConfig wast = DatabaseConfig.builder(eastUri)
        .weight(1.0f)  // Primary database
        .build();

// redis-west
RedisURI westUri = RedisURI.builder()
                .withHost("redis-west.example.com")
                .withPort(6379)
                .withPassword("secret".toCharArray())
                .build();
DatabaseConfig west = DatabaseConfig.builder(westUri)
        .weight(0.5f)  // Secondary database
        .build();

// Create the multi-database client
MultiDbClient client = MultiDbClient.create(Arrays.asList(east, west));

// Connect and use like a regular Redis connection
StatefulRedisMultiDbConnection<String, String> connection = client.connect();

// Execute commands asynchronously - they go to the highest-weighted healthy database
connection.async().set("key", "value");
String value = connection.async().get("key").get();

// Clean up
connection.close();
client.shutdown();
```

## Configuration Options

### DatabaseConfig

Each database is configured using `DatabaseConfig.Builder`:

| Setting | Method | Default | Description |
|---------|--------|---------|-------------|
| Weight | `weight(float)` | `1.0` | Priority weight for database selection. Higher weight = higher priority. |
| Circuit Breaker Config | `circuitBreakerConfig(CircuitBreakerConfig)` | Default config | Circuit breaker settings for failure detection. |
| Health Check Strategy | `healthCheckStrategySupplier(HealthCheckStrategySupplier)` | `PingStrategy.DEFAULT` | Strategy for health checks. Use `HealthCheckStrategySupplier.NO_HEALTH_CHECK` to disable. |
| Client Options | `clientOptions(ClientOptions)` | Default options | Per-database client options. |

### MultiDbOptions

Global options for the multi-database client:

| Setting | Method | Default | Description |
|---------|--------|---------|-------------|
| Failback Supported | `failbackSupported(boolean)` | `true` | Enable automatic failback to higher-priority databases. |
| Failback Check Interval | `failbackCheckInterval(Duration)` | `120 seconds` | Interval for checking if failed databases have recovered. |
| Grace Period | `gracePeriod(Duration)` | `60 seconds` | Time to wait before allowing failback to a recovered database. |
| Delay Between Failover Attempts | `delayInBetweenFailoverAttempts(Duration)` | `12 seconds` | Delay between failover attempts when no healthy database is available. |
| Initialization Policy | `initializationPolicy(InitializationPolicy)` | `MAJORITY_AVAILABLE` | Policy for connection initialization. |

Example with custom options:

```java
MultiDbOptions options = MultiDbOptions.builder()
        .failbackSupported(true)
        .failbackCheckInterval(Duration.ofSeconds(30))
        .gracePeriod(Duration.ofSeconds(10))
        .delayInBetweenFailoverAttempts(Duration.ofSeconds(5))
        .build();

MultiDbClient client = MultiDbClient.create(Arrays.asList(db1, db2), options);
```

### Circuit Breaker Configuration

The circuit breaker detects failures and triggers failover:

| Setting | Method | Default | Description |
|---------|--------|---------|-------------|
| Failure Rate Threshold | `failureRateThreshold(float)` | `10.0` | Percentage of failures to trigger circuit breaker. |
| Minimum Number of Failures | `minimumNumberOfFailures(int)` | `1000` | Minimum failures before circuit breaker can open. |
| Metrics Window Size | `metricsWindowSize(int)` | `2 seconds` | Time window for collecting metrics. |
| Tracked Exceptions | `trackedExceptions(Set)` | Connection/Timeout exceptions | Exceptions that count as failures. |

```java
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;

CircuitBreakerConfig cbConfig = CircuitBreakerConfig.builder()
        .failureRateThreshold(50.0f)
        .minimumNumberOfFailures(100)
        .metricsWindowSize(5)
        .build();

DatabaseConfig db = DatabaseConfig.builder(redisUri)
        .circuitBreakerConfig(cbConfig)
        .build();
```

## Health Check Configuration and Customization

The MultiDbClient includes a comprehensive health check system that continuously monitors the availability of Redis databases to enable automatic failover and failback.

The health check system serves several critical purposes in the failover architecture:

- **Proactive Monitoring** - Continuously monitors passive databases that aren't currently receiving traffic
- **Failback Detection** - Determines when a previously failed database has recovered and is ready to accept traffic
- **Circuit Breaker Integration** - Works with the circuit breaker pattern to manage database state transitions
- **Customizable Strategies** - Supports pluggable health check implementations for different deployment scenarios

The health check system operates independently of your application traffic, running background checks at configurable intervals to assess database health without impacting performance.

### PingStrategy (Default)

Uses the Redis `PING` command to verify connectivity:

```java
import io.lettuce.core.failover.health.PingStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategy;

// Use default PingStrategy
DatabaseConfig db = DatabaseConfig.builder(redisUri)
        .healthCheckStrategySupplier(PingStrategy.DEFAULT)
        .build();

// Or with custom configuration
HealthCheckStrategy.Config healthConfig = HealthCheckStrategy.Config.builder()
        .interval(5000)           // Check every 5 seconds
        .timeout(1000)            // 1 second timeout
        .numProbes(3)             // 3 probes per check
        .delayInBetweenProbes(500) // 500ms between probes
        .build();

DatabaseConfig db = DatabaseConfig.builder(redisUri)
        .healthCheckStrategySupplier((uri, factory) ->
                new PingStrategy(factory, healthConfig))
        .build();
```

### LagAwareStrategy (Redis Enterprise)

For Redis Enterprise deployments, use `LagAwareStrategy` which leverages the REST API to check database availability and replication lag.

**Required dependencies** (optional in Lettuce):
```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-codec-http</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

```java
import io.lettuce.core.failover.health.LagAwareStrategy;

LagAwareStrategy.Config lagConfig = LagAwareStrategy.Config.builder()
        .restApiUri(URI.create("https://cluster.redis.local:9443"))
        .credentials(() -> RedisCredentials.just("admin", "password"))
        .extendedCheckEnabled(true)  // Enable lag-aware checks
        .availabilityLagTolerance(Duration.ofMillis(100))
        .build();

DatabaseConfig db = DatabaseConfig.builder(redisUri)
        .healthCheckStrategySupplier((uri, factory) -> new LagAwareStrategy(lagConfig))
        .build();
```

### Custom Health Check Strategies

Implement the `HealthCheckStrategy` interface for custom health validation logic, integration with external monitoring systems, or latency-based health checks.

```java
public class CustomHealthCheck extends AbstractHealthCheckStrategy {

    public CustomHealthCheck(HealthCheckStrategy.Config config) {
        super(config);
    }

    @Override
    public HealthStatus doHealthCheck(RedisURI endpoint) {
        // Return HealthStatus.HEALTHY, UNHEALTHY, or UNKNOWN
        return checkExternalMonitoringSystem(endpoint)
                ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
    }
}

// Use with healthCheckStrategySupplier()
DatabaseConfig db = DatabaseConfig.builder(redisUri)
        .healthCheckStrategySupplier((uri, factory) ->
                new CustomHealthCheck(HealthCheckStrategy.Config.create()))
        .build();
```

### Disabling Health Checks

To disable health checks for a specific database:

```java
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;

DatabaseConfig db = DatabaseConfig.builder(redisUri)
        .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
        .build();
```

## Failback

When a failover is triggered, Lettuce will attempt to connect to the next Redis server based on the weights of server configurations you provide at setup.

For example, recall the `redis-east` and `redis-west` deployments from the basic usage example above. Lettuce will attempt to connect to `redis-east` first. If `redis-east` becomes unavailable, then Lettuce will attempt to use `redis-west`.

Now suppose that `redis-east` eventually comes back online. You will likely want to switch your application back to `redis-east`.

### Automatic Failback

Lettuce automatically monitors the health of all configured databases, including those that are currently inactive due to previous failures. The automatic failback process works as follows:

- **Continuous Monitoring** - Health checks run continuously for all databases, regardless of their current active status
- **Recovery Detection** - When a previously failed database recovers, Lettuce detects this through health checks
- **Weight-Based Failback** - If automatic failback is enabled and a recovered database has a higher weight than the currently active database, Lettuce will automatically switch to the recovered database
- **Grace Period Respect** - Failback only occurs after the configured grace period has elapsed since the database was marked as healthy

Configure failback behavior:

```java
MultiDbOptions options = MultiDbOptions.builder()
        .failbackSupported(true)              // Enable automatic failback
        .failbackCheckInterval(Duration.ofSeconds(30))  // How often to check
        .gracePeriod(Duration.ofSeconds(60))  // Wait before failback
        .build();
```

### Disabling Failback

To disable automatic failback:

```java
MultiDbOptions options = MultiDbOptions.builder()
        .failbackSupported(false)
        .build();
```

### Manual Failback

You can manually switch to a specific database using the connection API:

```java
StatefulRedisMultiDbConnection<String, String> connection = client.connect();

// Get current endpoint
RedisURI current = connection.getCurrentEndpoint();

// Get all available endpoints
Collection<RedisURI> endpoints = connection.getEndpoints();

// Check if a specific endpoint is healthy
boolean healthy = connection.isHealthy(targetUri);

// Force switch to a specific endpoint
connection.switchTo(targetUri);
```

## Dynamic Database Management

You can add or remove databases at runtime:

```java
StatefulRedisMultiDbConnection<String, String> connection = client.connect();

// Add a new database
RedisURI newDb = RedisURI.create("redis://new-server:6379");
DatabaseConfig newConfig = DatabaseConfig.builder(newDb)
        .weight(0.8f)
        .build();
connection.addDatabase(newConfig);

// Remove a database
connection.removeDatabase(existingUri);
```

## Events

Listen for database switch events to monitor failover and failback:

### Database Switch Events

```java
client.getResources().eventBus().get()
        .filter(event -> event instanceof DatabaseSwitchEvent)
        .cast(DatabaseSwitchEvent.class)
        .subscribe(event -> log.info("Switch: {} -> {} ({})",
                event.getFromDb(), event.getToDb(), event.getReason()));
```

Switch reasons:
- `HEALTH_CHECK` - Database failed health check
- `CIRCUIT_BREAKER` - Circuit breaker opened due to failures
- `FAILBACK` - Automatic failback to higher-priority database
- `FORCED` - Manual switch via `switchTo()`

### All Databases Unhealthy Event

Fired when all databases are unhealthy and no failover target is available:

```java
client.getResources().eventBus().get()
        .filter(event -> event instanceof AllDatabasesUnhealthyEvent)
        .cast(AllDatabasesUnhealthyEvent.class)
        .subscribe(event -> log.warn("All databases unhealthy! Attempts: {}, DBs: {}",
                event.getFailedAttempts(), event.getUnhealthyDatabases()));
```

## Troubleshooting

### Enable Debug Logging

Add to your logging configuration:

```properties
logging.level.io.lettuce.core.failover=DEBUG
```

