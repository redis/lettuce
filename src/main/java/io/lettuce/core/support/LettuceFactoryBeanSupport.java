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

import java.net.URI;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;

/**
 * Adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * {@link io.lettuce.core.RedisClient} factories via Spring configuration.
 *
 * @author Mark Paluch
 * @since 3.0
 * @deprecated since 5.3, use Lettuce through Spring Data Redis. This class will be removed with Lettuce 6.
 */
@Deprecated
public abstract class LettuceFactoryBeanSupport<T> extends AbstractFactoryBean<T> {

    public static final String URI_SCHEME_REDIS_SENTINEL = "redis-sentinel";

    private char[] password = new char[0];

    private URI uri;

    private RedisURI redisURI;

    private ClientResources clientResources;

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
     * @param uri the URI.
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
     * @param redisURI the RedisURI.
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
     * @param password the password.
     */
    public void setPassword(String password) {

        if (password == null) {
            this.password = new char[0];
        } else {
            this.password = password.toCharArray();
        }
    }

    public ClientResources getClientResources() {
        return clientResources;
    }

    /**
     * Set shared client resources to reuse across different client instances. If not set, each client instance will provide
     * their own {@link ClientResources} instance.
     *
     * @param clientResources the client resources.
     */
    public void setClientResources(ClientResources clientResources) {
        this.clientResources = clientResources;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
