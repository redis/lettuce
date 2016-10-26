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

import java.lang.reflect.Method;

import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;

/**
 * Invocation Handler with transparent pooling. This handler is thread-safe.
 * 
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Will be removed in future versions. Use  {@link ConnectionPoolSupport}.
 */
@Deprecated
public class TransparentPoolingInvocationHandler<T> extends AbstractInvocationHandler {

    private RedisConnectionPool<T> pool;

    public TransparentPoolingInvocationHandler(RedisConnectionPool<T> pool) {
        this.pool = pool;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        if (pool == null) {
            throw new RedisException("Connection pool is closed");
        }

        if (method.getName().equals("close")) {
            pool.close();
            pool = null;
            return null;
        }

        T connection = pool.allocateConnection();
        try {
            return method.invoke(connection, args);
        } finally {
            pool.freeConnection(connection);
        }
    }

    public RedisConnectionPool<T> getPool() {
        return pool;
    }

}
