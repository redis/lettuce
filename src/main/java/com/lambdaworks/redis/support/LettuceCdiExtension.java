package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A portable CDI extension which registers beans for lettuce. If there are no RedisURIs there are also no registrations for
 * {@link RedisClient RedisClients}. The extension allows to create {@link RedisClient} and {@link RedisClusterClient}
 * instances. Client instances are provided under the same qualifiers as the {@link RedisURI}. {@link ClientResources} can be
 * shared across multiple client instances (Standalone, Cluster) by providing a {@link ClientResources} bean with the same
 * qualifiers as the {@link RedisURI}.
 * 
 * <p>
 * <strong>Example:</strong>
 * </p>
 * 
 * <pre>
 * <code>
 *  public class Producers {
 *     &commat;Produces
 *     public RedisURI redisURI() {
 *         return RedisURI.Builder.redis(AbstractCommandTest.host, AbstractCommandTest.port).build();
 *     }
 *     
 *     &commat;Produces
 *     public ClientResources clientResources() {
 *         return DefaultClientResources.create()
 *     }
 *     
 *     public void shutdownClientResources(@Disposes ClientResources clientResources) throws Exception {
 *         clientResources.shutdown().get();
 *     }
 * </code>
 * </pre>
 * 
 *
 * <pre>
 *  <code>
 *   public class Consumer {
 *      &commat;Inject
 *      private RedisClient client;
 *      
 *      &commat;Inject
 *      private RedisClusterClient clusterClient;
 * }     
 *  </code>
 * </pre>
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class LettuceCdiExtension implements Extension {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(LettuceCdiExtension.class);

    private final Map<Set<Annotation>, Bean<RedisURI>> redisUris = Maps.newConcurrentMap();
    private final Map<Set<Annotation>, Bean<ClientResources>> clientResources = Maps.newConcurrentMap();

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
                Set<Annotation> qualifiers = new HashSet<Annotation>(bean.getQualifiers());
                if (bean.isAlternative() || !redisUris.containsKey(qualifiers)) {
                    LOGGER.debug(String.format("Discovered '%s' with qualifiers %s.", RedisURI.class.getName(), qualifiers));
                    redisUris.put(qualifiers, (Bean<RedisURI>) bean);
                }
            }

            if (ClientResources.class.isAssignableFrom((Class<?>) type)) {
                Set<Annotation> qualifiers = new HashSet<Annotation>(bean.getQualifiers());
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
            if (!contains(qualifiers, Default.class)) {
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

    private boolean contains(Set<Annotation> qualifiers, Class<Default> defaultClass) {
        Optional<Annotation> result = Iterables.tryFind(qualifiers, new Predicate<Annotation>() {
            @Override
            public boolean apply(Annotation input) {
                return input instanceof Default;
            }
        });
        return result.isPresent();
    }

    private void register(AfterBeanDiscovery afterBeanDiscovery, Set<Annotation> qualifiers, Bean<?> bean) {
        LOGGER.info(String.format("Registering bean '%s' with qualifiers %s.", bean.getBeanClass().getName(), qualifiers));
        afterBeanDiscovery.addBean(bean);
    }
}
