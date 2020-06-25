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
package io.lettuce.core.dynamic.output;

import io.lettuce.core.dynamic.support.ResolvableType;

/**
 * Base class for {@link CommandOutputFactory} resolution such as {@link OutputRegistryCommandOutputFactoryResolver}.
 * <p>
 * This class provides methods to check provider/selector type assignability. Subclasses are responsible for calling methods in
 * this class in the correct order.
 *
 * @author Mark Paluch
 */
public abstract class CommandOutputResolverSupport {

    /**
     * Overridable hook to check whether {@code selector} can be assigned from the provider type {@code provider}.
     * <p>
     * This method descends the component type hierarchy and considers primitive/wrapper type conversion.
     *
     * @param selector must not be {@code null}.
     * @param provider must not be {@code null}.
     * @return {@code true} if selector can be assigned from its provider type.
     */
    protected boolean isAssignableFrom(OutputSelector selector, OutputType provider) {

        ResolvableType selectorType = selector.getOutputType();
        ResolvableType resolvableType = provider.withCodec(selector.getRedisCodec());

        return selectorType.isAssignableFrom(resolvableType);
    }

}
