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

import com.lambdaworks.redis.RedisConnectionPool;

/**
 * Execution-Template which allocates a connection around the run()-call. Use this class as adapter template and implement your
 * redis calls within the run-method.
 * 
 * @param <T> Connection type.
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Will be removed in future versions. Use  {@link ConnectionPoolSupport}.
 */
@Deprecated
public abstract class WithConnection<T> {

    /**
     * Performs connection handling and invokes the run-method with a valid Redis connection.
     * 
     * @param pool the connection pool.
     */
    public WithConnection(RedisConnectionPool<T> pool) {
        T connection = pool.allocateConnection();
        try {
            run(connection);
        } finally {
            pool.freeConnection(connection);
        }
    }

    /**
     * Execution method. Will be called with a valid redis connection.
     * 
     * @param connection the connection
     */
    protected abstract void run(T connection);
}
