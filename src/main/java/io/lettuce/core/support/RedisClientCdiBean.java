package io.lettuce.core.support;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;

/**
 * Factory Bean for {@link RedisClient} instances. Requires a {@link RedisURI} and allows to reuse
 * {@link io.lettuce.core.resource.ClientResources}. URI Formats: {@code
 *     redis-sentinel://host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
 * }
 *
 * {@code
 *     redis://host[:port][/databaseNumber]
 * }
 *
 * @see RedisURI
 * @author Mark Paluch
 * @since 3.0
 */
class RedisClientCdiBean extends AbstractCdiBean<RedisClient> {

    RedisClientCdiBean(Bean<RedisURI> redisURIBean, Bean<ClientResources> clientResourcesBean, BeanManager beanManager,
            Set<Annotation> qualifiers, String name) {
        super(redisURIBean, clientResourcesBean, beanManager, qualifiers, name);
    }

    @Override
    public Class<?> getBeanClass() {
        return RedisClient.class;
    }

    @Override
    public RedisClient create(CreationalContext<RedisClient> creationalContext) {

        CreationalContext<RedisURI> uriCreationalContext = beanManager.createCreationalContext(redisURIBean);
        RedisURI redisURI = (RedisURI) beanManager.getReference(redisURIBean, RedisURI.class, uriCreationalContext);

        if (clientResourcesBean != null) {
            ClientResources clientResources = (ClientResources) beanManager.getReference(clientResourcesBean,
                    ClientResources.class, uriCreationalContext);
            return RedisClient.create(clientResources, redisURI);
        }

        return RedisClient.create(redisURI);
    }

    @Override
    public void destroy(RedisClient instance, CreationalContext<RedisClient> creationalContext) {
        instance.shutdown();
    }

}
