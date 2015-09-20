package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
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
import com.google.common.collect.Iterables;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A portable CDI extension which registers beans for lettuce. If there are no RedisURIs there are also no registrations for
 * {@link RedisClient RedisClients}. The extension allows to create {@link RedisClient} and {@link RedisClusterClient}
 * instances. Client instances are provided under the same qualifiers as the {@link RedisURI}.
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
 *    }
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
            if (!contains(qualifiers)) {
                clientBeanName += counter;
                clusterClientBeanName += counter;
                counter++;
            }

            RedisClientCdiBean clientBean = new RedisClientCdiBean(redisUri, beanManager, qualifiers, clientBeanName);
            register(afterBeanDiscovery, qualifiers, clientBean);

            RedisClusterClientCdiBean clusterClientBean = new RedisClusterClientCdiBean(redisUri, beanManager, qualifiers,
                    clusterClientBeanName);
            register(afterBeanDiscovery, qualifiers, clusterClientBean);

        }
    }

    private boolean contains(Set<Annotation> qualifiers) {
        Optional<Annotation> result = Iterables.tryFind(qualifiers, input -> input instanceof Default);
        return result.isPresent();
    }

    private void register(AfterBeanDiscovery afterBeanDiscovery, Set<Annotation> qualifiers, Bean<?> bean) {
        LOGGER.info(String.format("Registering bean '%s' with qualifiers %s.", bean.getBeanClass().getName(), qualifiers));
        afterBeanDiscovery.addBean(bean);
    }
}
