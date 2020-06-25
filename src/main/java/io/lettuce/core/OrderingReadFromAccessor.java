/*
 * Copyright 2019-2020 the original author or authors.
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
 * Accessor for {@link ReadFrom} ordering. Internal utility class.
 *
 * @author Mark Paluch
 * @since 5.2
 */
public abstract class OrderingReadFromAccessor {

    /**
     * Utility constructor.
     */
    private OrderingReadFromAccessor() {
    }

    /**
     * Returns whether this {@link ReadFrom} requires ordering of the resulting
     * {@link io.lettuce.core.models.role.RedisNodeDescription nodes}.
     *
     * @return {@code true} if code using {@link ReadFrom} should retain ordering or {@code false} to allow reordering of
     *         {@link io.lettuce.core.models.role.RedisNodeDescription nodes}.
     * @since 5.2
     * @see ReadFrom#isOrderSensitive()
     */
    public static boolean isOrderSensitive(ReadFrom readFrom) {
        return readFrom.isOrderSensitive();
    }

}
