/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.dynamic.output;

/**
 * Strategy interface to resolve a {@link CommandOutputFactory} based on a {@link OutputSelector}. Resolution of
 * {@link CommandOutputFactory} is based on {@link io.lettuce.core.dynamic.CommandMethod} result types and can be
 * influenced whether the result type is a key or value result type. Additional type variables (based on the used
 * {@link io.lettuce.core.codec.RedisCodec} are hints to improve output resolution.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see OutputSelector
 */
public interface CommandOutputFactoryResolver {

    /**
     * Resolve a regular {@link CommandOutputFactory} that produces the {@link io.lettuce.core.output.CommandOutput}
     * result component type.
     *
     * @param outputSelector must not be {@literal null}.
     * @return the {@link CommandOutputFactory} if resolved, {@literal null} otherwise.
     */
    CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector);

    /**
     * Resolve a streaming {@link CommandOutputFactory} that produces the {@link io.lettuce.core.output.StreamingOutput}
     * result component type.
     *
     * @param outputSelector must not be {@literal null}.
     * @return the {@link CommandOutputFactory} that implements {@link io.lettuce.core.output.StreamingOutput} if
     *         resolved, {@literal null} otherwise.
     */
    CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector);
}
