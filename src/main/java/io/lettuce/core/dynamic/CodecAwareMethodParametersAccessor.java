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
