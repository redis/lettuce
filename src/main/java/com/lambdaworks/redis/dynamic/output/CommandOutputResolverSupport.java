package com.lambdaworks.redis.dynamic.output;

import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.dynamic.support.TypeVariableTypeInformation;
import com.lambdaworks.redis.internal.LettuceClassUtils;

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
     * @param selector must not be {@literal null}.
     * @param provider must not be {@literal null}.
     * @return {@literal true} if selector can be assigned from its provider type.
     */
    protected boolean isAssignableFrom(OutputSelector selector, OutputType provider) {

        TypeInformation<?> outputTypeInformation = provider.getTypeInformation();
        TypeInformation<?> selectorTypeInformation = selector.getTypeInformation();

        do {

            if (outputTypeInformation instanceof TypeVariableTypeInformation) {
                if (selector.containsTypeVariable(outputTypeInformation.toString())) {
                    outputTypeInformation = selector.getTypeVariable(outputTypeInformation.toString());
                }
            }

            if (outputTypeInformation.getType() == Object.class && selectorTypeInformation.getComponentType() != null) {

                if (provider.getPrimaryType() == OutputRegistry.KeySurrogate.class) {
                    return selector.isKey();
                }

                if (provider.getPrimaryType() == OutputRegistry.ValueSurrogate.class) {
                    return selector.isValue();
                }
            }

            if (!isAssignableFrom(selectorTypeInformation, outputTypeInformation)) {
                return false;
            }

            outputTypeInformation = outputTypeInformation.getComponentType();
            selectorTypeInformation = selectorTypeInformation.getComponentType();

        } while (outputTypeInformation != null && outputTypeInformation.getComponentType() != outputTypeInformation
                && selectorTypeInformation != null && selectorTypeInformation.getComponentType() != selectorTypeInformation);
        return true;
    }

    /**
     * Overridable hook to check whether {@code selector} can be assigned from the provider type {@code provider}.
     * 
     * @param selector must not be {@literal null}.
     * @param provider must not be {@literal null}.
     * @return {@literal true} if selector can be assigned from its provider type.
     */
    protected boolean isAssignableFrom(TypeInformation<?> selector, TypeInformation<?> provider) {

        return selector.isAssignableFrom(provider) || LettuceClassUtils.isAssignable(selector.getType(), provider.getType());
    }
}
