package io.lettuce.core.failover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.test.settings.TestSettings;

import static io.lettuce.core.failover.health.HealthCheckStrategySupplier.NO_HEALTH_CHECK;

/**
 * @author Ali Takavci
 * @since 7.1
 */
public class MultiDbTestSupport extends TestSupport {

    protected final MultiDbClient multiDbClient;

    protected final RedisClient directClient1;

    protected final RedisClient directClient2;

    protected final RedisClient directClient3;

    protected RedisURI uri1;

    protected RedisURI uri2;

    protected RedisURI uri3;

    public MultiDbTestSupport(MultiDbClient multiDbClient) {
        this.multiDbClient = multiDbClient;
        Iterator<RedisURI> endpoints = multiDbClient.getRedisURIs().iterator();
        this.uri1 = endpoints.next();
        this.uri2 = endpoints.next();
        this.uri3 = endpoints.next();

        this.directClient1 = RedisClient.create(uri1);
        this.directClient2 = RedisClient.create(uri2);
        this.directClient3 = RedisClient.create(uri3);
    }

    @BeforeEach
    void setUpMultiDb() {
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();
        directClient3.connect().sync().flushall();
    }

    @AfterEach
    public void tearDownMultiDb() {
        directClient1.shutdown();
        directClient2.shutdown();
        directClient3.shutdown();
    }

    public static final RedisURI URI1 = RedisURI.create(TestSettings.host(), TestSettings.port(10));

    public static final RedisURI URI2 = RedisURI.create(TestSettings.host(), TestSettings.port(11));

    public static final RedisURI URI3 = RedisURI.create(TestSettings.host(), TestSettings.port(12));

    /*
     * DBs configured with Disable health checks for testing CB detected failures without interference from health checks
     */
    public static final DatabaseConfig DB1 = DatabaseConfig.builder(URI1).weight(1.0f)
            .healthCheckStrategySupplier(NO_HEALTH_CHECK).build();

    public static final DatabaseConfig DB2 = DatabaseConfig.builder(URI2).weight(0.5f)
            .healthCheckStrategySupplier(NO_HEALTH_CHECK).build();

    public static final DatabaseConfig DB3 = DatabaseConfig.builder(URI3).weight(0.25f)
            .healthCheckStrategySupplier(NO_HEALTH_CHECK).build();

    public static final List<DatabaseConfig> DBs = getDatabaseConfigs();

    private static List<DatabaseConfig> getDatabaseConfigs() {
        return Arrays.asList(DB1, DB2, DB3);
    }

    public static List<DatabaseConfig> getDatabaseConfigs(RedisURI... URIs) {
        float weight = 1.0f;
        List<DatabaseConfig> endpoints = new ArrayList<>();
        for (RedisURI uri : URIs) {
            endpoints.add(DatabaseConfig.builder(uri).weight(weight).build());
            weight /= 2;
        }
        return endpoints;
    }

    public static List<DatabaseConfig> getDatabaseConfigs(ClientOptions clientOptions,
            CircuitBreaker.CircuitBreakerConfig circuitBreakerConfig, RedisURI... URIs) {
        float weight = 1.0f;
        List<DatabaseConfig> endpoints = new ArrayList<>();
        for (RedisURI uri : URIs) {
            endpoints.add(DatabaseConfig.builder(uri).weight(weight).circuitBreakerConfig(circuitBreakerConfig)
                    .clientOptions(clientOptions).build());
            weight /= 2;
        }
        return endpoints;
    }

}
