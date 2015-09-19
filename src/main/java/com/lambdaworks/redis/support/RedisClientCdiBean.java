package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * Factory Bean for {@link RedisClient} instances. Requires a {@link RedisURI}.. URI Formats:
 * {@code
 *     redis-sentinel://host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
 * }
 *
 * {@code
 *     redis://host[:port][/databaseNumber]
 * }
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class RedisClientCdiBean extends AbstractCdiBean<RedisClient> {

    // todo: support for client resources

    RedisClientCdiBean(BeanManager beanManager, Set<Annotation> qualifiers, Bean<RedisURI> redisURIBean, String name) {
        super(redisURIBean, beanManager, qualifiers, name);
    }

    @Override
    public Class<?> getBeanClass() {
        return RedisClient.class;
    }

    @Override
    public RedisClient create(CreationalContext<RedisClient> creationalContext) {

        CreationalContext<RedisURI> uriCreationalContext = beanManager.createCreationalContext(redisURIBean);
        RedisURI redisURI = (RedisURI) beanManager.getReference(redisURIBean, RedisURI.class, uriCreationalContext);

        return RedisClient.create(redisURI);
    }

    @Override
    public void destroy(RedisClient instance, CreationalContext<RedisClient> creationalContext) {
        instance.shutdown();
    }
}
