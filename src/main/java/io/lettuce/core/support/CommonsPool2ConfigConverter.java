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

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Utility class to adapt Commons Pool 2 configuration to {@link BoundedPoolConfig}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class CommonsPool2ConfigConverter {

    private CommonsPool2ConfigConverter() {
    }

    /**
     * Converts {@link GenericObjectPoolConfig} properties to an immutable {@link BoundedPoolConfig}. Applies max total, min/max
     * idle and test on borrow/create/release configuration.
     *
     * @param config must not be {@code null}.
     * @return the converted {@link BoundedPoolConfig}.
     */
    public static BoundedPoolConfig bounded(GenericObjectPoolConfig<?> config) {

        LettuceAssert.notNull(config, "GenericObjectPoolConfig must not be null");

        return BoundedPoolConfig.builder() //
                .maxTotal(config.getMaxTotal() > 0 ? config.getMaxTotal() : Integer.MAX_VALUE)
                .maxIdle(config.getMaxIdle() > 0 ? config.getMaxIdle() : Integer.MAX_VALUE) //
                .minIdle(config.getMinIdle()) //
                .testOnAcquire(config.getTestOnBorrow()) //
                .testOnCreate(config.getTestOnCreate()) //
                .testOnRelease(config.getTestOnReturn()) //
                .build();
    }

}
