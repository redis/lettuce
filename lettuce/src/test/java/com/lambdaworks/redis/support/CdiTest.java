package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.enterprise.inject.Produces;

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.AbstractCommandTest;
import com.lambdaworks.redis.RedisConnectionStateListener;
import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 20:04
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
        return RedisURI.Builder.redis(AbstractCommandTest.host, AbstractCommandTest.port).build();
    }

    @Test
    public void testInjection() {

        InjectedClient injectedClient = container.getInstance(InjectedClient.class);
        assertThat(injectedClient.redisClient).isNotNull();
        assertThat(injectedClient.redisClusterClient).isNotNull();

        RedisConnectionStateListener mock = mock(RedisConnectionStateListener.class);

        // do some interaction to force the container a creation of the repositories.
        injectedClient.redisClient.addListener(mock);
        injectedClient.redisClusterClient.addListener(mock);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        container.stopApplicationScope();
        container.shutdownContainer();

    }

}
