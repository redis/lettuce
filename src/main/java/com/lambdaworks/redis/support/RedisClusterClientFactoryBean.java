package com.lambdaworks.redis.support;

import static com.google.common.base.Preconditions.checkArgument;
import static com.lambdaworks.redis.LettuceStrings.isNotEmpty;

import java.net.URI;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * Factory Bean for {@link RedisClusterClient} instances. Needs either a {@link URI} or a {@link RedisURI} as input. URI Format:
 * {@code
 *     redis://[password@]host[:port]
 * }
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class RedisClusterClientFactoryBean extends LettuceFactoryBeanSupport<RedisClusterClient> {

    @Override
    public void afterPropertiesSet() throws Exception {

        if (getRedisURI() == null) {
            URI uri = getUri();

            checkArgument(!uri.getScheme().equals(RedisURI.URI_SCHEME_REDIS_SENTINEL),
                    "Sentinel mode not supported when using RedisClusterClient");
            checkArgument(uri.getScheme().equals(RedisURI.URI_SCHEME_REDIS),
                    "Only plain connections allowed when using RedisClusterClient");

            RedisURI redisURI = RedisURI.create(uri);
            if (isNotEmpty(getPassword())) {
                redisURI.setPassword(getPassword());
            }
            setRedisURI(redisURI);
        }

        super.afterPropertiesSet();

    }

    @Override
    protected void destroyInstance(RedisClusterClient instance) throws Exception {
        instance.shutdown();
    }

    @Override
    public Class<?> getObjectType() {
        return RedisClusterClient.class;
    }

    @Override
    protected RedisClusterClient createInstance() throws Exception {

        return RedisClusterClient.create(getRedisURI());
    }
}
