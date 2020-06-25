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

import java.util.concurrent.CompletableFuture;

import org.apache.commons.pool2.PooledObject;

/**
 * An interface defining life-cycle methods for instances to be served by an pool.
 *
 * @param <T> Type of element managed in this factory.
 * @author Mark Paluch
 * @since 5.1
 */
public interface AsyncObjectFactory<T> {

    /**
     * Create an instance that can be served by the pool and wrap it in a {@link PooledObject} to be managed by the pool.
     *
     * @return a {@code PooledObject} wrapping an instance that can be served by the pool.
     */
    CompletableFuture<T> create();

    /**
     * Destroys an instance no longer needed by the pool.
     * <p>
     * It is important for implementations of this method to be aware that there is no guarantee about what state {@code object}
     * will be in and the implementation should be prepared to handle unexpected errors.
     * <p>
     * Also, an implementation must take in to consideration that instances lost to the garbage collector may never be
     * destroyed.
     *
     * @param object a {@code PooledObject} wrapping the instance to be destroyed.
     * @see #validate
     */
    CompletableFuture<Void> destroy(T object);

    /**
     * Ensures that the instance is safe to be returned by the pool.
     *
     * @param object a {@code PooledObject} wrapping the instance to be validated.
     * @return {@code false} if {@code object} is not valid and should be dropped from the pool, {@code true} otherwise.
     */
    CompletableFuture<Boolean> validate(T object);

}
