# Automatic Failover and Failback with Lettuce

!!! WARNING
    **Experimental Feature** <p>
    This feature is experimental and may change in future versions.

Lettuce supports automatic failover and failback for your Redis deployments through the `MultiDbClient`. This is useful when:

1. You have more than one Redis deployment (e.g., two independent Redis servers or multiple Redis databases replicated across active-active clusters).
2. You want your application to connect to and use one deployment at a time.
3. You want your application to fail over to the next available deployment if the current deployment becomes unavailable.
4. You want your application to fail back to the original deployment when it becomes available again.

Lettuce will fail over to a subsequent Redis deployment after the circuit breaker detects failures exceeding the configured threshold. In the background, Lettuce executes health checks to determine when a Redis deployment is available again. When this occurs, Lettuce will fail back to the original deployment.

## Basic Usage

To configure Lettuce for failover, you specify a weighted list of Redis databases. Lettuce will connect to the Redis database with the highest weight. If the highest-weighted database becomes unavailable, Lettuce will attempt to connect to the database with the next highest weight.

Weights can be configured at setup time and changed dynamically at runtime (since 7.5.0), allowing you to adjust priorities without restarting your application.

Suppose you run two Redis deployments: `redis-east` and `redis-west`. You want your application to first connect to `redis-east`. If `redis-east` becomes unavailable, you want your application to connect to `redis-west`.

```java
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.api.MultiDbOptions;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;


// Configure databases with weights (higher weight = higher priority)
// redis-east
RedisURI eastUri = RedisURI.builder()
        .withHost("redis-east.example.com")
        .withPort(6379)
        .withPassword("secret".toCharArray())
        .build();

DatabaseConfig east = DatabaseConfig.builder(eastUri)
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
| Weight | `weight(float)` | `1.0` | Priority weight for database selection. Higher weight = higher priority. Can be changed at runtime (since 7.5.0). |
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
import io.lettuce.core.failover.api.CircuitBreakerConfig;

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
- **Dynamic Weight Changes** - Since 7.5.0, changing a database's weight at runtime can trigger automatic failback if the weight becomes higher than the currently active database

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

## Dynamic Weight and Priority Changes (Since 7.5.0)

Lettuce allows you to change database weights dynamically at runtime without restarting your application or recreating connections. This is useful for:

- **Controlled database switching** - Change which database should be active without restarting the application
- **Maintenance preparation** - Lower the weight of a database before planned maintenance to prevent automatic failback to it after the maintenance window
- **Database migration** - Switch the active database from one deployment to another (e.g., from old infrastructure to new infrastructure)
- **Priority inversion** - Temporarily make a secondary database the primary (e.g., during regional issues or performance problems)

!!! WARNING
    **Weight is Priority, Not Load Distribution** <p>
    The weight determines which **single database** is active at any time. It does NOT distribute load across multiple databases. Only one database handles traffic at a time - the healthy database with the highest weight.

### Changing Weights at Runtime

You can get and set weights for any database through the connection API:

```java
StatefulRedisMultiDbConnection<String, String> connection = client.connect();

// Get current active database and its weight
RedisDatabase currentDb = connection.getCurrentDatabase();
float currentWeight = currentDb.getWeight();
System.out.println("Current active database weight: " + currentWeight);

// Get weight for a specific database by endpoint
RedisURI targetUri = RedisURI.create("redis://redis-west.example.com:6379");
RedisDatabase targetDb = connection.getDatabase(targetUri);
float targetWeight = targetDb.getWeight();
System.out.println("Target database weight: " + targetWeight);

// Change which database should be active by setting a higher weight
// This will cause automatic failback to targetDb if failback is enabled
targetDb.setWeight(currentWeight + 1.0f);
```

### Runtime Behavior with Dynamic Weights

When you change database weights at runtime, the following behavior applies:

#### Automatic Failback Triggered by Weight Changes

If automatic failback is enabled (`failbackSupported(true)`), changing weights can trigger automatic failback:

- **Higher Priority Database Available** - If you increase a database's weight above the currently active database, and that database is healthy, Lettuce will automatically fail back to it during the next failback check
- **Failback Check Interval** - Weight-based failback respects the configured `failbackCheckInterval` (default: 120 seconds)
- **Grace Period** - The database must have passed its grace period before failback occurs
- **Health Requirements** - The target database must be healthy (health check status is `HEALTHY` and circuit breaker is `CLOSED`)

Example scenario:

```java
// Setup: Two databases with failback enabled
DatabaseConfig east = DatabaseConfig.builder(eastUri)
        .weight(1.0f)
        .build();

DatabaseConfig west = DatabaseConfig.builder(westUri)
        .weight(0.5f)
        .build();

MultiDbOptions options = MultiDbOptions.builder()
        .failbackSupported(true)
        .failbackCheckInterval(Duration.ofSeconds(30))
        .gracePeriod(Duration.ofSeconds(10))
        .build();

MultiDbClient client = MultiDbClient.create(Arrays.asList(east, west), options);
StatefulRedisMultiDbConnection<String, String> connection = client.connect();

// Initially connected to redis-east (weight 1.0)
System.out.println("Current: " + connection.getCurrentEndpoint()); // redis-east

// Manually switch to redis-west
connection.switchTo(westUri);
System.out.println("After manual switch: " + connection.getCurrentEndpoint()); // redis-west

// Increase redis-west's weight to be higher than redis-east
connection.getDatabase(westUri).setWeight(1.5f);

// Lettuce will NOT failback because redis-west (1.5) is already active
// and has the highest weight

// Now increase redis-east's weight to be even higher
connection.getDatabase(eastUri).setWeight(2.0f);

// After the next failback check (within 30 seconds), Lettuce will automatically
// fail back to redis-east because it has a higher weight (2.0 > 1.5)
// and is healthy
Thread.sleep(30000);
System.out.println("After weight change: " + connection.getCurrentEndpoint()); // redis-east
```

#### Weight Changes Without Automatic Failback

If automatic failback is disabled (`failbackSupported(false)`), weight change itself does not trigger automatic database switches:

```java
MultiDbOptions options = MultiDbOptions.builder()
        .failbackSupported(false)
        .build();

MultiDbClient client = MultiDbClient.create(Arrays.asList(east, west), options);
StatefulRedisMultiDbConnection<String, String> connection = client.connect();

// Change weights
connection.getDatabase(westUri).setWeight(5.0f);

// No automatic switch occurs - you must manually switch if desired
connection.switchTo(westUri);
```

#### Weight Changes and Failover

Weight changes affect which database Lettuce selects during failover:

- When the current database fails, Lettuce selects the healthy database with the **highest current weight**
- Weight changes are immediately reflected in failover decisions
- This allows you to control failover targets dynamically

### Best Practices for Dynamic Weight Changes

1. **Understand Failback Timing** - Weight changes trigger failback only during the next `failbackCheckInterval` check (default: 120 seconds)
2. **Consider Grace Periods** - Recently recovered databases must pass the grace period before they can become active
3. **Weight Validation** - Weights must be greater than 0; attempting to set invalid weights throws `IllegalArgumentException`
4. **Thread Safety** - Weight changes are thread-safe and can be performed from any thread
5. **Failover vs Failback** - Weight changes affect both failover target selection (immediate) and failback decisions (periodic)

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

## Need help or have questions?
For assistance with this automatic failover and failback feature,
[start a discussion](https://github.com/redis/lettuce/discussions/new?category=q-a).