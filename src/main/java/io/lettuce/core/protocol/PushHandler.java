/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.protocol;

import java.util.Collection;

import io.lettuce.core.api.push.PushListener;

/**
 * A handler object that provides access to {@link PushListener}.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface PushHandler {

    /**
     * Add a new {@link PushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void addListener(PushListener listener);

    /**
     * Remove an existing {@link PushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void removeListener(PushListener listener);

    /**
     * Returns a collection of {@link PushListener}.
     *
     * @return the collection of listeners.
     */
    Collection<PushListener> getPushListeners();

}
