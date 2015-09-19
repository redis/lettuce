package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * Factory Bean for {@link RedisClusterClient} instances. Requires a {@link RedisURI}. URI Format:
 * {@code
 *     redis://[password@]host[:port]
 * }
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class RedisClusterClientCdiBean extends AbstractCdiBean<RedisClusterClient> {

    // todo: support for client resources

    public RedisClusterClientCdiBean(BeanManager beanManager, Set<Annotation> qualifiers, Bean<RedisURI> redisURIBean,
            String name) {
        super(redisURIBean, beanManager, qualifiers, name);
    }

    @Override
    public Class<?> getBeanClass() {
        return RedisClusterClient.class;
    }

    @Override
    public RedisClusterClient create(CreationalContext<RedisClusterClient> creationalContext) {

        CreationalContext<RedisURI> uriCreationalContext = beanManager.createCreationalContext(redisURIBean);
        RedisURI redisURI = (RedisURI) beanManager.getReference(redisURIBean, RedisURI.class, uriCreationalContext);

        return new RedisClusterClient(redisURI);
    }

    @Override
    public void destroy(RedisClusterClient instance, CreationalContext<RedisClusterClient> creationalContext) {
        instance.shutdown();
    }
}
