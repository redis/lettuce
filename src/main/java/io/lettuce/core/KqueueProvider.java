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
package io.lettuce.core;

/**
 * Wraps and provides kqueue classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence of
 * the {@literal netty-transport-native-kqueue} library during runtime. Internal API.
 *
 * @author Mark Paluch
 * @since 4.4
 * @deprecated since 6.0, use {@link io.lettuce.core.resource.KqueueProvider} instead.
 */
@Deprecated
public class KqueueProvider {

    /**
     * @return {@code true} if kqueue is available.
     */
    public static boolean isAvailable() {
        return io.lettuce.core.resource.KqueueProvider.isAvailable();
    }

}
