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
package io.lettuce.core.internal;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link AsyncCloseable} is a resource that can be closed. The {@link #closeAsync()} method is invoked to request resources
 * release that the object is holding (such as open files).
 *
 * @since 5.1
 * @author Mark Paluch
 */
public interface AsyncCloseable {

    /**
     * Requests to close this object and releases any system resources associated with it. If the object is already closed then
     * invoking this method has no effect.
     * <p>
     * Calls to this method return a {@link CompletableFuture} that is notified with the outcome of the close request.
     */
    CompletableFuture<Void> closeAsync();

}
