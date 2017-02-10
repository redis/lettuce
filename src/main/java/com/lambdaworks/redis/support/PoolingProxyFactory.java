/*
 * Copyright 2011-2016 the original author or authors.
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

import java.lang.reflect.Proxy;

import com.lambdaworks.redis.RedisConnectionPool;

/**
 * Pooling proxy factory to create transparent pooling proxies. These proxies will allocate internally connections and use
 * always valid connections. You don't need to allocate/free the connections anymore.
 * 
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Will be removed in future versions. Use  {@link ConnectionPoolSupport}.
 */
@Deprecated
public class PoolingProxyFactory {

    /**
     * Utility constructor.
     */
    private PoolingProxyFactory() {

    }

    /**
     * Creates a transparent connection pooling proxy. Will re-check the connection every 5 secs.
     * 
     * @param connectionPool The Redis connection pool
     * @param <T> Type of the connection.
     * @return Transparent pooling proxy.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(RedisConnectionPool<T> connectionPool) {
        Class<?> componentType = connectionPool.getComponentType();

        TransparentPoolingInvocationHandler<T> h = new TransparentPoolingInvocationHandler<>(connectionPool);

        Object o = Proxy.newProxyInstance(PoolingProxyFactory.class.getClassLoader(), new Class<?>[] { componentType }, h);

        return (T) o;
    }

}
