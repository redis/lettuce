package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;

/**
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SpringJavaConfigTest {

@Configuration
static class LettuceConfig {

    @Bean(destroyMethod = "shutdown")
    ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean(destroyMethod = "shutdown")
    RedisClient redisClient(ClientResources clientResources) {

        return RedisClient.create(clientResources, RedisURI.create(TestSettings.host(), TestSettings.port()));
    }

    @Bean(destroyMethod = "shutdown")
    RedisClusterClient redisClusterClient(ClientResources clientResources) {

        RedisURI redisURI = RedisURI.create(TestSettings.host(), 7379);

        return RedisClusterClient.create(clientResources, redisURI);
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, String> connection(RedisClient redisClient) {
        return redisClient.connect();
    }

    @Bean(destroyMethod = "close")
    StatefulRedisClusterConnection<String, String> clusterConnection(RedisClusterClient clusterClient) {
        return clusterClient.connect();
    }
}

    @Autowired
    StatefulRedisConnection<String, String> connection;

    @Autowired
    StatefulRedisClusterConnection<String, String> clusterConnection;

    @Test
    public void testStandalone() {
        assertThat(connection.sync().ping()).isEqualTo("PONG");
    }

    @Test
    public void testCluster() {
        assertThat(clusterConnection.sync().ping()).isEqualTo("PONG");
    }
}
