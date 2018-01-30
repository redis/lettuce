/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.*;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A portable CDI extension which registers beans for lettuce. If there are no RedisURIs there are also no registrations for
 * {@link RedisClient RedisClients}. The extension allows to create {@link RedisClient} and {@link RedisClusterClient}
 * instances. Client instances are provided under the same qualifiers as the {@link RedisURI}. {@link ClientResources} can be
 * shared across multiple client instances (Standalone, Cluster) by providing a {@link ClientResources} bean with the same
 * qualifiers as the {@link RedisURI}.
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * public class Producers {
 *     &#064;Produces
 *     public RedisURI redisURI() {
 *         return RedisURI.Builder.redis(&quot;localhost&quot;, 6379).build();
 *     }
 *
 *     &#064;Produces
 *     public ClientResources clientResources() {
 *         return DefaultClientResources.create()
 *     }
 *
 *     public void shutdownClientResources(@Disposes ClientResources clientResources) throws Exception {
 *         clientResources.shutdown().get();
 *     }
 * }
 * </pre>
 *
 *
 * <pre class="code">
 * public class Consumer {
 *     &#064;Inject
 *     private RedisClient client;
 *
 *     &#064;Inject
 *     private RedisClusterClient clusterClient;
 * }
 * </pre>
 *
 * @author Mark Paluch
 */
public class LettuceCdiExtension implements Extension {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(LettuceCdiExtension.class);

    private final Map<Set<Annotation>, Bean<RedisURI>> redisUris = new ConcurrentHashMap<>();
    private final Map<Set<Annotation>, Bean<ClientResources>> clientResources = new ConcurrentHashMap<>();

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
            if (!(type instanceof Class<?>)) {
                continue;
            }

            // Check if the bean is an RedisURI.
            if (RedisURI.class.isAssignableFrom((Class<?>) type)) {
                Set<Annotation> qualifiers = LettuceSets.newHashSet(bean.getQualifiers());
                if (bean.isAlternative() || !redisUris.containsKey(qualifiers)) {
                    LOGGER.debug(String.format("Discovered '%s' with qualifiers %s.", RedisURI.class.getName(), qualifiers));
                    redisUris.put(qualifiers, (Bean<RedisURI>) bean);
                }
            }

            if (ClientResources.class.isAssignableFrom((Class<?>) type)) {
                Set<Annotation> qualifiers = LettuceSets.newHashSet(bean.getQualifiers());
                if (bean.isAlternative() || !clientResources.containsKey(qualifiers)) {
                    LOGGER.debug(String.format("Discovered '%s' with qualifiers %s.", ClientResources.class.getName(),
                            qualifiers));
                    clientResources.put(qualifiers, (Bean<ClientResources>) bean);
                }
            }
        }
    }

    /**
     * Implementation of a an observer which registers beans to the CDI container for the detected RedisURIs.
     * <p>
     * The repository beans are associated to the EntityManagers using their qualifiers.
     *
     * @param beanManager The BeanManager instance.
     */
    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        int counter = 0;
        for (Entry<Set<Annotation>, Bean<RedisURI>> entry : redisUris.entrySet()) {

            Bean<RedisURI> redisUri = entry.getValue();
            Set<Annotation> qualifiers = entry.getKey();

            String clientBeanName = RedisClient.class.getSimpleName();
            String clusterClientBeanName = RedisClusterClient.class.getSimpleName();
            if (!containsDefault(qualifiers)) {
                clientBeanName += counter;
                clusterClientBeanName += counter;
                counter++;
            }

            Bean<ClientResources> clientResources = this.clientResources.get(qualifiers);

            RedisClientCdiBean clientBean = new RedisClientCdiBean(redisUri, clientResources, beanManager, qualifiers,
                    clientBeanName);
            register(afterBeanDiscovery, qualifiers, clientBean);

            RedisClusterClientCdiBean clusterClientBean = new RedisClusterClientCdiBean(redisUri, clientResources, beanManager,
                    qualifiers, clusterClientBeanName);
            register(afterBeanDiscovery, qualifiers, clusterClientBean);

        }
    }

    private boolean containsDefault(Set<Annotation> qualifiers) {
        return qualifiers.stream().filter(input -> input instanceof Default).findFirst().isPresent();
    }

    private void register(AfterBeanDiscovery afterBeanDiscovery, Set<Annotation> qualifiers, Bean<?> bean) {
        LOGGER.info(String.format("Registering bean '%s' with qualifiers %s.", bean.getBeanClass().getName(), qualifiers));
        afterBeanDiscovery.addBean(bean);
    }
}
