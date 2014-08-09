package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 21:36
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SpringTest {

    @Autowired
    @Qualifier("RedisClient1")
    private RedisClient redisClient1;

    @Autowired
    @Qualifier("RedisClient2")
    private RedisClient redisClient2;

    @Autowired
    @Qualifier("RedisClient3")
    private RedisClient redisClient3;

    @Autowired
    @Qualifier("RedisClusterClient1")
    private RedisClusterClient redisClusterClient1;

    @Autowired
    @Qualifier("RedisClusterClient2")
    private RedisClusterClient redisClusterClient2;

    @Test
    public void testSpring() throws Exception {

        assertThat(redisClient1).isNotNull();
        assertThat(redisClient2).isNotNull();
        assertThat(redisClient3).isNotNull();
        assertThat(redisClusterClient1).isNotNull();
        assertThat(redisClusterClient2).isNotNull();
    }
}
