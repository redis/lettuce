/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.lettuce.core.dynamic.support.ClassTypeInformation;
import io.lettuce.core.dynamic.support.TypeInformation;
import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 */
class ConversionService {

    private Map<ConvertiblePair, Function<?, ?>> converterMap = new HashMap<>(10);

    /**
     * Register a converter {@link Function}.
     *
     * @param converter the converter.
     */
    @SuppressWarnings("rawtypes")
    public void addConverter(Function<?, ?> converter) {

        LettuceAssert.notNull(converter, "Converter must not be null");

        ClassTypeInformation<? extends Function> classTypeInformation = ClassTypeInformation.from(converter.getClass());
        TypeInformation<?> typeInformation = classTypeInformation.getSuperTypeInformation(Function.class);
        List<TypeInformation<?>> typeArguments = typeInformation.getTypeArguments();

        ConvertiblePair pair = new ConvertiblePair(typeArguments.get(0).getType(), typeArguments.get(1).getType());
        converterMap.put(pair, converter);
    }

    @SuppressWarnings("unchecked")
    public <S, T> T convert(S source, Class<T> targetType) {

        LettuceAssert.notNull(source, "Source must not be null");

        return (T) getConverter(source.getClass(), targetType).apply(source);
    }

    public <S, T> boolean canConvert(Class<S> sourceType, Class<T> targetType) {
        return findConverter(sourceType, targetType).isPresent();
    }

    @SuppressWarnings("unchecked")
    Function<Object, Object> getConverter(Class<?> source, Class<?> target) {
        return findConverter(source, target).orElseThrow(() -> new IllegalArgumentException(
                String.format("No converter found for %s to %s conversion", source.getName(), target.getName())));
    }

    private Optional<Function<Object, Object>> findConverter(Class<?> source, Class<?> target) {
        LettuceAssert.notNull(source, "Source type must not be null");
        LettuceAssert.notNull(target, "Target type must not be null");

        for (ConvertiblePair pair : converterMap.keySet()) {

            if (pair.getSourceType().isAssignableFrom(source) && target.isAssignableFrom(pair.getTargetType())) {
                return Optional.of((Function) converterMap.get(pair));
            }
        }
        return Optional.empty();
    }

    /**
     * Holder for a source-to-target class pair.
     */
    final class ConvertiblePair {

        private final Class<?> sourceType;

        private final Class<?> targetType;

        /**
         * Create a new source-to-target pair.
         *
         * @param sourceType the source type
         * @param targetType the target type
         */
        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {

            LettuceAssert.notNull(sourceType, "Source type must not be null");
            LettuceAssert.notNull(targetType, "Target type must not be null");
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public Class<?> getSourceType() {
            return this.sourceType;
        }

        public Class<?> getTargetType() {
            return this.targetType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || other.getClass() != ConvertiblePair.class) {
                return false;
            }
            ConvertiblePair otherPair = (ConvertiblePair) other;
            return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
        }

        @Override
        public int hashCode() {
            return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
        }

        @Override
        public String toString() {
            return (this.sourceType.getName() + " -> " + this.targetType.getName());
        }

    }

}
