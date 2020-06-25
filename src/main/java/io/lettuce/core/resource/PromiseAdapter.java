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
package io.lettuce.core.resource;

import io.netty.util.concurrent.*;

/**
 * Utility class to support netty's future handling.
 *
 * @author Mark Paluch
 * @since 3.4
 */
class PromiseAdapter {

    /**
     * Create a promise that emits a {@code Boolean} value on completion of the {@code future}
     *
     * @param future the future.
     * @return Promise emitting a {@code Boolean} value. {@code true} if the {@code future} completed successfully, otherwise
     *         the cause wil be transported.
     */
    static Promise<Boolean> toBooleanPromise(Future<?> future) {

        DefaultPromise<Boolean> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        if (future.isDone() || future.isCancelled()) {
            if (future.isSuccess()) {
                result.setSuccess(true);
            } else {
                result.setFailure(future.cause());
            }
            return result;
        }

        future.addListener((GenericFutureListener<Future<Object>>) f -> {

            if (f.isSuccess()) {
                result.setSuccess(true);
            } else {
                result.setFailure(f.cause());
            }
        });
        return result;
    }

}
