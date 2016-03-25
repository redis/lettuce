package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.FastShutdown;
import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.RedisConnectionStateListener;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class CdiTest {

    static CdiTestContainer container;

    @BeforeClass
    public static void setUp() throws Exception {

        container = CdiTestContainerLoader.getCdiContainer();
        container.bootContainer();
        container.startApplicationScope();
    }

    @Produces
    public RedisURI redisURI() {
        return RedisURI.Builder.redis(AbstractRedisClientTest.host, AbstractRedisClientTest.port).build();
    }

    @Produces
    @PersonDB
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    public void shutdownClientResources(@Disposes ClientResources clientResources) throws Exception {
        FastShutdown.shutdown(clientResources);
    }

    @PersonDB
    @Produces
    public RedisURI redisURIQualified() {
        return RedisURI.Builder.redis(AbstractRedisClientTest.host, AbstractRedisClientTest.port + 1).build();
    }

    @Test
    public void testInjection() {

        InjectedClient injectedClient = container.getInstance(InjectedClient.class);
        assertThat(injectedClient.redisClient).isNotNull();
        assertThat(injectedClient.redisClusterClient).isNotNull();

        assertThat(injectedClient.qualifiedRedisClient).isNotNull();
        assertThat(injectedClient.qualifiedRedisClusterClient).isNotNull();

        RedisConnectionStateListener mock = mock(RedisConnectionStateListener.class);

        // do some interaction to force the container a creation of the repositories.
        injectedClient.redisClient.addListener(mock);
        injectedClient.redisClusterClient.addListener(mock);

        injectedClient.qualifiedRedisClient.addListener(mock);
        injectedClient.qualifiedRedisClusterClient.addListener(mock);

        injectedClient.pingRedis();
    }

    @AfterClass
    public static void afterClass() throws Exception {

        container.stopApplicationScope();
        container.shutdownContainer();

    }

}
