/*
 * Copyright 2017-2020 the original author or authors.
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

import io.lettuce.core.internal.LettuceAssert;

/**
 * Basic implementation of a pool configured through {@link BasePoolConfig}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public abstract class BasePool {

    private final boolean testOnCreate;

    private final boolean testOnAcquire;

    private final boolean testOnRelease;

    /**
     * Create a new pool given {@link BasePoolConfig}.
     *
     * @param poolConfig must not be {@code null}.
     */
    protected BasePool(BasePoolConfig poolConfig) {

        LettuceAssert.notNull(poolConfig, "PoolConfig must not be null");

        this.testOnCreate = poolConfig.isTestOnCreate();
        this.testOnAcquire = poolConfig.isTestOnAcquire();
        this.testOnRelease = poolConfig.isTestOnRelease();
    }

    /**
     * Returns whether objects created for the pool will be validated before being returned from the acquire method. Validation
     * is performed by the {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the pool. If the
     * object fails to validate, then acquire will fail.
     *
     * @return {@code true} if newly created objects are validated before being returned from the acquire method.
     */
    public boolean isTestOnCreate() {
        return testOnCreate;
    }

    /**
     * Returns whether objects acquired from the pool will be validated before being returned from the acquire method.
     * Validation is performed by the {@link AsyncObjectFactory#validate(Object)} method of the factory associated with the
     * pool. If the object fails to validate, it will be removed from the pool and destroyed, and a new attempt will be made to
     * borrow an object from the pool.
     *
     * @return {@code true} if objects are validated before being returned from the acquire method.
     */
    public boolean isTestOnAcquire() {
        return testOnAcquire;
    }

    /**
     * Returns whether objects borrowed from the pool will be validated when they are returned to the pool via the release
     * method. Validation is performed by the {@link AsyncObjectFactory#validate(Object)} method of the factory associated with
     * the pool. Returning objects that fail validation are destroyed rather then being returned the pool.
     *
     * @return {@code true} if objects are validated on return to the pool via the release method.
     */
    public boolean isTestOnRelease() {
        return testOnRelease;
    }

    /**
     * Set the {@link StackTraceElement} for the given {@link Throwable}, using the {@link Class} and method name.
     */
    static <T extends Throwable> T unknownStackTrace(T cause, Class<?> clazz, String method) {
        cause.setStackTrace(new StackTraceElement[] { new StackTraceElement(clazz.getName(), method, null, -1) });
        return cause;
    }

}
