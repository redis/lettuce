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
package com.lambdaworks.redis.dynamic.codec;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.CommandMethod;

/**
 * Strategy interface to resolve a {@link RedisCodec} for a {@link CommandMethod}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface RedisCodecResolver {

    /**
     * Resolve a {@link RedisCodec} for the given {@link CommandMethod}.
     * 
     * @param commandMethod must not be {@literal null}.
     * @return the resolved {@link RedisCodec} or {@literal null} if not resolvable.
     */
    RedisCodec<?, ?> resolve(CommandMethod commandMethod);
}
