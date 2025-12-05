package io.lettuce.core.failover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.test.settings.TestSettings;

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

    public static final RedisURI URI1 = RedisURI.create(TestSettings.host(), TestSettings.port());

    public static final RedisURI URI2 = RedisURI.create(TestSettings.host(), TestSettings.port(1));

    public static final RedisURI URI3 = RedisURI.create(TestSettings.host(), TestSettings.port(5));

    public static final DatabaseConfig DB1 = new DatabaseConfig(URI1, 1.0f);

    public static final DatabaseConfig DB2 = new DatabaseConfig(URI2, 0.5f);

    public static final DatabaseConfig DB3 = new DatabaseConfig(URI3, 0.25f);

    public static final List<DatabaseConfig> DBs = getDatabaseConfigs();

    private static List<DatabaseConfig> getDatabaseConfigs() {
        return Arrays.asList(DB1, DB2, DB3);
    }

    public static List<DatabaseConfig> getDatabaseConfigs(RedisURI... URIs) {
        float weight = 1.0f;
        List<DatabaseConfig> endpoints = new ArrayList<>();
        for (RedisURI uri : URIs) {
            endpoints.add(new DatabaseConfig(uri, weight));
            weight /= 2;
        }
        return endpoints;
    }

    public static List<DatabaseConfig> getDatabaseConfigs(CircuitBreaker.CircuitBreakerConfig circuitBreakerConfig,
            RedisURI... URIs) {
        float weight = 1.0f;
        List<DatabaseConfig> endpoints = new ArrayList<>();
        for (RedisURI uri : URIs) {
            endpoints.add(new DatabaseConfig(uri, weight, null, circuitBreakerConfig));
            weight /= 2;
        }
        return endpoints;
    }

}
