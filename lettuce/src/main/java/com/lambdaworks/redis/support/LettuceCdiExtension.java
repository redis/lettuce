/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;

import com.lambdaworks.redis.RedisURI;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A portable CDI extension which registers beans for lettuce. If there are no RedisURIs there are also no registrations for
 * RedisClients.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class LettuceCdiExtension implements Extension {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(LettuceCdiExtension.class);

    private final Map<Set<Annotation>, Bean<RedisURI>> redisUris = new HashMap<Set<Annotation>, Bean<RedisURI>>();

    public LettuceCdiExtension() {
        LOGGER.info("Activating CDI extension for lettuce.");
    }

    /**
     * Implementation of a an observer which checks for RedisURI beans and stores them in {@link #redisUris} for later
     * association with corresponding repository beans.
     *
     * @param <T> The type.
     * @param processBean The annotated type as defined by CDI.
     */
    @SuppressWarnings("unchecked")
    <T> void processBean(@Observes ProcessBean<T> processBean) {
        Bean<T> bean = processBean.getBean();
        for (Type type : bean.getTypes()) {
            // Check if the bean is an RedisURI.
            if (type instanceof Class<?> && RedisURI.class.isAssignableFrom((Class<?>) type)) {
                Set<Annotation> qualifiers = new HashSet<Annotation>(bean.getQualifiers());
                if (bean.isAlternative() || !redisUris.containsKey(qualifiers)) {
                    LOGGER.debug(String.format("Discovered '%s' with qualifiers %s.", RedisURI.class.getName(), qualifiers));
                    redisUris.put(qualifiers, (Bean<RedisURI>) bean);
                }
            }
        }
    }

    /**
     * Implementation of a an observer which registers beans to the CDI container for the detected Spring Data repositories.
     * <p>
     * The repository beans are associated to the EntityManagers using their qualifiers.
     *
     * @param beanManager The BeanManager instance.
     */
    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        for (Entry<Set<Annotation>, Bean<RedisURI>> entry : redisUris.entrySet()) {

            Bean<RedisURI> redisUri = entry.getValue();
            Set<Annotation> qualifiers = entry.getKey();

            RedisClientCdiBean clientBean = new RedisClientCdiBean(beanManager, qualifiers, redisUri);
            register(afterBeanDiscovery, qualifiers, clientBean);

            RedisClusterClientCdiBean clusterClientBean = new RedisClusterClientCdiBean(beanManager, qualifiers, redisUri);
            register(afterBeanDiscovery, qualifiers, clusterClientBean);

        }
    }

    private void register(AfterBeanDiscovery afterBeanDiscovery, Set<Annotation> qualifiers, Bean<?> bean) {
        LOGGER.info(String.format("Registering bean '%s' with qualifiers %s.", bean.getBeanClass().getName(), qualifiers));
        afterBeanDiscovery.addBean(bean);
    }
}
