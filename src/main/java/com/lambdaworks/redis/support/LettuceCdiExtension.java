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
import javax.enterprise.inject.spi.*;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisClusterClient;

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

            RedisClientCdiBean clientBean = new RedisClientCdiBean(beanManager, qualifiers, redisUri, clientBeanName);
            register(afterBeanDiscovery, qualifiers, clientBean);

            RedisClusterClientCdiBean clusterClientBean = new RedisClusterClientCdiBean(beanManager, qualifiers, redisUri,
                    clusterClientBeanName);
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
