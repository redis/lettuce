package com.lambdaworks.redis.support;

import java.net.URI;

import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * Factory Bean for RedisClient instances. Needs either a URI or a RedisURI as input. URI Format: <code>
 *     redis://host[:port][/databaseNumber]
 * </code>
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 21:30
 */
public class RedisClusterClientFactoryBean extends LettuceFactoryBeanSupport<RedisClusterClient> {

    @Override
    public void afterPropertiesSet() throws Exception {

        if (getRedisURI() == null) {
            URI uri = getUri();

            RedisURI.Builder builder = null;
            if (uri.getScheme().equals("redis-sentinel")) {
                throw new IllegalArgumentException("Sentinel mode not supported when using RedisClusterClient");
            } else {
                if (uri.getPort() != -1) {
                    builder = RedisURI.Builder.redis(uri.getHost(), uri.getPort());
                } else {
                    builder = RedisURI.Builder.redis(uri.getHost());
                }
            }

            if (LettuceStrings.isNotEmpty(getPassword())) {
                builder.withPassword(getPassword());
            }

            if (LettuceStrings.isNotEmpty(uri.getPath())) {
                String pathSuffix = uri.getPath().substring(1);

                if (LettuceStrings.isNotEmpty(pathSuffix)) {

                    builder.withDatabase(Integer.parseInt(pathSuffix));
                }
            }

            setRedisURI(builder.build());

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
