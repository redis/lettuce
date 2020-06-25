/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
