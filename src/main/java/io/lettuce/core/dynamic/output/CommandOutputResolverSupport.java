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
