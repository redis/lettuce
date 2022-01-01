/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.concurrent.ThreadFactory;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Default {@link ThreadFactoryProvider} implementation.
 *
 * @author Mark Paluch
 */
enum DefaultThreadFactoryProvider implements ThreadFactoryProvider {

    INSTANCE;

    @Override
    public ThreadFactory getThreadFactory(String poolName) {
        return new DefaultThreadFactory(poolName, true);
    }

}
