package com.lambdaworks.redis.support;

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

    // todo: support for client resources

    @Override
    public void afterPropertiesSet() throws Exception {

        if (getRedisURI() == null) {
            URI uri = getUri();

            if (uri.getScheme().equals(RedisURI.URI_SCHEME_REDIS_SENTINEL)) {
                throw new IllegalArgumentException("Sentinel mode not supported when using RedisClusterClient");
            }

            if (!uri.getScheme().equals(RedisURI.URI_SCHEME_REDIS)) {
                throw new IllegalArgumentException("Only plain connections allowed when using RedisClusterClient");
            }

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
        return new RedisClusterClient(getRedisURI());
    }
}
