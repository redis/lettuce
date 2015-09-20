package com.lambdaworks.redis.support;

import java.net.URI;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.lambdaworks.redis.RedisURI;

/**
 * Adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * {@link com.lambdaworks.redis.RedisClient} factories via Spring configuration.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
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

    /**
     * Set the URI for connecting Redis. The URI follows the URI conventions. See {@link RedisURI} for URL schemes. Either the
     * URI of the RedisURI must be set in order to connect to Redis.
     *
     * @param uri the URI
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    public RedisURI getRedisURI() {
        return redisURI;
    }

    /**
     * Set the RedisURI for connecting Redis. See {@link RedisURI} for URL schemes. Either the URI of the RedisURI must be set
     * in order to connect to Redis.
     *
     * @param redisURI the RedisURI
     */
    public void setRedisURI(RedisURI redisURI) {
        this.redisURI = redisURI;
    }

    public String getPassword() {
        return new String(password);
    }

    /**
     * Sets the password to use for a Redis connection. If the password is set, it has higher precedence than the password
     * provided within the URI meaning the password from the URI is replaced by this one.
     *
     * @param password the password
     */
    public void setPassword(String password) {

        if (password == null) {
            this.password = new char[0];
        } else {
            this.password = password.toCharArray();
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
