package io.lettuce.core.support;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;

/**
 * Factory Bean for {@link RedisClusterClient} instances. Requires a {@link RedisURI} and allows to reuse
 * {@link io.lettuce.core.resource.ClientResources}. URI Format: {@code
 *     redis://[password@]host[:port]
 * }
 *
 * @see RedisURI
 * @author Mark Paluch
 * @since 3.0
 */
class RedisClusterClientCdiBean extends AbstractCdiBean<RedisClusterClient> {

    public RedisClusterClientCdiBean(Bean<RedisURI> redisURIBean, Bean<ClientResources> clientResourcesBean,
            BeanManager beanManager, Set<Annotation> qualifiers, String name) {
        super(redisURIBean, clientResourcesBean, beanManager, qualifiers, name);
    }

    @Override
    public Class<?> getBeanClass() {
        return RedisClusterClient.class;
    }

    @Override
    public RedisClusterClient create(CreationalContext<RedisClusterClient> creationalContext) {

        CreationalContext<RedisURI> uriCreationalContext = beanManager.createCreationalContext(redisURIBean);
        RedisURI redisURI = (RedisURI) beanManager.getReference(redisURIBean, RedisURI.class, uriCreationalContext);

        if (clientResourcesBean != null) {
            ClientResources clientResources = (ClientResources) beanManager.getReference(clientResourcesBean,
                    ClientResources.class, uriCreationalContext);
            return RedisClusterClient.create(clientResources, redisURI);
        }

        return RedisClusterClient.create(redisURI);
    }

    @Override
    public void destroy(RedisClusterClient instance, CreationalContext<RedisClusterClient> creationalContext) {
        instance.shutdown();
    }

}
