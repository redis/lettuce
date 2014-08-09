package com.lambdaworks.redis.support;

import java.net.URI;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.lambdaworks.redis.RedisURI;

/**
 * Adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of redis client
 * factories via Spring configuration.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 21:39
 */
public abstract class LettuceFactoryBeanSupport<T> extends AbstractFactoryBean<T> {

    public static final String URI_SCHEME_REDIS_SENTINEL = "redis-sentinel";

    private char[] password = new char[0];
    private URI uri;
    private RedisURI redisURI;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getRedisURI() == null && getUri() == null) {
            throw new IllegalArgumentException("Either uri or redisURI must be set");
        }
        super.afterPropertiesSet();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public RedisURI getRedisURI() {
        return redisURI;
    }

    public void setRedisURI(RedisURI redisURI) {
        this.redisURI = redisURI;
    }

    public String getPassword() {
        return new String(password);
    }

    public void setPassword(String password) {

        if (password == null) {
            this.password = new char[0];
        } else {
            this.password = password.toCharArray();
        }
    }
}
