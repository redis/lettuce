package com.lambdaworks.redis.support;

import static com.lambdaworks.redis.LettuceStrings.isNotEmpty;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * Factory Bean for {@link RedisClient} instances. Needs either a {@link java.net.URI} or a {@link RedisURI} as input and allows
 * to reuse {@link com.lambdaworks.redis.resource.ClientResources}. URI Formats:
 * {@code
 *     redis-sentinel://host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
 * }
 *
 * {@code
 *     redis://host[:port][/databaseNumber]
 * }
 *
 * @see RedisURI
 * @see ClientResourcesFactoryBean
 * @author Mark Paluch
 * @since 3.0
 */
public class RedisClientFactoryBean extends LettuceFactoryBeanSupport<RedisClient> {

    @Override
    public void afterPropertiesSet() throws Exception {

        if (getRedisURI() == null) {
            RedisURI redisURI = RedisURI.create(getUri());

            if (isNotEmpty(getPassword())) {
                redisURI.setPassword(getPassword());
            }
            setRedisURI(redisURI);
        }

        super.afterPropertiesSet();
    }

    @Override
    protected void destroyInstance(RedisClient instance) throws Exception {
        instance.shutdown();
    }

    @Override
    public Class<?> getObjectType() {
        return RedisClient.class;
    }

    @Override
    protected RedisClient createInstance() throws Exception {

        if (getClientResources() != null) {
            return RedisClient.create(getClientResources(), getRedisURI());
        }
        return RedisClient.create(getRedisURI());
    }
}
