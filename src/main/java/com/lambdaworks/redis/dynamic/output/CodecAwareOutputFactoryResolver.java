package com.lambdaworks.redis.dynamic.output;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link RedisCodec}-aware implementation of {@link CommandOutputFactoryResolver}. This implementation inspects
 * {@link RedisCodec} regarding its type and enhances {@link OutputSelector} for {@link CommandOutputFactory} resolution.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public class CodecAwareOutputFactoryResolver implements CommandOutputFactoryResolver {

    private final CommandOutputFactoryResolver delegate;
    private final TypeInformation<?> keyType;
    private final TypeInformation<?> valueType;
    private final Map<String, TypeInformation<?>> typeVariables = new HashMap<>();

    /**
     * Create a new {@link CodecAwareOutputFactoryResolver} given {@link CommandOutputFactoryResolver} and {@link RedisCodec}.
     * 
     * @param delegate must not be {@literal null}.
     * @param redisCodec must not be {@literal null}.
     */
    public CodecAwareOutputFactoryResolver(CommandOutputFactoryResolver delegate, RedisCodec<?, ?> redisCodec) {

        LettuceAssert.notNull(delegate, "CommandOutputFactoryResolver delegate must not be null");
        LettuceAssert.notNull(redisCodec, "RedisCodec must not be null");

        this.delegate = delegate;

        ClassTypeInformation<? extends RedisCodec> typeInformation = ClassTypeInformation.from(redisCodec.getClass());
        TypeInformation<?> superTypeInformation = typeInformation.getSuperTypeInformation(RedisCodec.class);
        List<TypeInformation<?>> typeArguments = superTypeInformation.getTypeArguments();

        this.keyType = typeArguments.get(0);
        this.valueType = typeArguments.get(1);

        this.typeVariables.put("K", keyType);
        this.typeVariables.put("V", valueType);
    }

    @Override
    public CommandOutputFactory resolveCommandOutput(OutputSelector outputSelector) {

        Map<String, TypeInformation<?>> typeVariables = new HashMap<>(outputSelector.getTypeVariables());
        typeVariables.putAll(this.typeVariables);

        return delegate.resolveCommandOutput(
                new OutputSelector(outputSelector.getTypeInformation(), isKeyType(outputSelector.getTypeInformation()),
                        isValueType(outputSelector.getTypeInformation()), typeVariables));
    }

    @Override
    public CommandOutputFactory resolveStreamingCommandOutput(OutputSelector outputSelector) {

        Map<String, TypeInformation<?>> typeVariables = new HashMap<>(outputSelector.getTypeVariables());
        typeVariables.putAll(this.typeVariables);

        return delegate.resolveStreamingCommandOutput(
                new OutputSelector(outputSelector.getTypeInformation(), isKeyType(outputSelector.getTypeInformation()),
                        isValueType(outputSelector.getTypeInformation()), typeVariables));
    }

    protected boolean isKeyType(TypeInformation<?> typeInformation) {
        return walkComponentTypeAssignability(typeInformation, keyType);
    }

    protected boolean isValueType(TypeInformation<?> typeInformation) {
        return walkComponentTypeAssignability(typeInformation, valueType);
    }

    private boolean walkComponentTypeAssignability(TypeInformation<?> typeInformation, TypeInformation<?> sourceType) {

        do {
            if (typeInformation.isAssignableFrom(sourceType)) {
                return true;
            }
            typeInformation = typeInformation.getComponentType();
        } while (typeInformation != null);
        return false;
    }
}
