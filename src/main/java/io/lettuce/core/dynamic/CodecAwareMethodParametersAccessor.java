package io.lettuce.core.dynamic;

import java.util.Iterator;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.parameter.MethodParametersAccessor;
import io.lettuce.core.dynamic.support.ClassTypeInformation;
import io.lettuce.core.dynamic.support.TypeInformation;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Codec-aware {@link MethodParametersAccessor}. Identifies key and value types by checking value compatibility with
 * {@link RedisCodec} types.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class CodecAwareMethodParametersAccessor implements MethodParametersAccessor {

    private final MethodParametersAccessor delegate;

    private final TypeContext typeContext;

    public CodecAwareMethodParametersAccessor(MethodParametersAccessor delegate, RedisCodec<?, ?> redisCodec) {

        LettuceAssert.notNull(delegate, "MethodParametersAccessor must not be null");
        LettuceAssert.notNull(redisCodec, "RedisCodec must not be null");

        this.delegate = delegate;
        this.typeContext = new TypeContext(redisCodec);
    }

    public CodecAwareMethodParametersAccessor(MethodParametersAccessor delegate, TypeContext typeContext) {

        LettuceAssert.notNull(delegate, "MethodParametersAccessor must not be null");
        LettuceAssert.notNull(typeContext, "TypeContext must not be null");

        this.delegate = delegate;
        this.typeContext = typeContext;
    }

    @Override
    public int getParameterCount() {
        return delegate.getParameterCount();
    }

    @Override
    public Object getBindableValue(int index) {
        return delegate.getBindableValue(index);
    }

    @Override
    public boolean isKey(int index) {

        if (delegate.isValue(index)) {
            return false;
        }

        if (delegate.isKey(index)) {
            return true;
        }

        Object bindableValue = getBindableValue(index);

        if (bindableValue != null && typeContext.keyType.getType().isAssignableFrom(bindableValue.getClass())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isValue(int index) {

        if (delegate.isKey(index)) {
            return false;
        }

        if (delegate.isValue(index)) {
            return true;
        }

        Object bindableValue = getBindableValue(index);

        if (bindableValue != null && typeContext.valueType.getType().isAssignableFrom(bindableValue.getClass())) {
            return true;
        }

        return false;
    }

    @Override
    public Iterator<Object> iterator() {
        return delegate.iterator();
    }

    @Override
    public int resolveParameterIndex(String name) {
        return delegate.resolveParameterIndex(name);
    }

    @Override
    public boolean isBindableNullValue(int index) {
        return delegate.isBindableNullValue(index);
    }

    /**
     * Cacheable type context for a {@link RedisCodec}.
     */
    public static class TypeContext {

        final TypeInformation<?> keyType;

        final TypeInformation<?> valueType;

        @SuppressWarnings("rawtypes")
        public TypeContext(RedisCodec<?, ?> redisCodec) {

            LettuceAssert.notNull(redisCodec, "RedisCodec must not be null");

            ClassTypeInformation<? extends RedisCodec> typeInformation = ClassTypeInformation.from(redisCodec.getClass());

            this.keyType = typeInformation.getTypeArgument(RedisCodec.class, 0);
            this.valueType = typeInformation.getTypeArgument(RedisCodec.class, 1);
        }

    }

}
