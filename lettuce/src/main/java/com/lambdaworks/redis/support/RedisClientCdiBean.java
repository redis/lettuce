package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 20:14
 */
class RedisClientCdiBean extends AbstractCdiBean<RedisClient> {

    public RedisClientCdiBean(BeanManager beanManager, Set<Annotation> qualifiers, Bean<RedisURI> redisURIBean) {
        super(redisURIBean, beanManager, qualifiers);
    }

    @Override
    public String getName() {
        return "RedisClient";
    }

    @Override
    public Class<?> getBeanClass() {
        return RedisClient.class;
    }

    @Override
    public RedisClient create(CreationalContext<RedisClient> creationalContext) {

        CreationalContext<RedisURI> uriCreationalContext = beanManager.createCreationalContext(redisURIBean);
        RedisURI redisURI = (RedisURI) beanManager.getReference(redisURIBean, RedisURI.class, uriCreationalContext);

        return new RedisClient(redisURI);
    }

    @Override
    public void destroy(RedisClient instance, CreationalContext<RedisClient> creationalContext) {
        instance.shutdown();
    }
}
