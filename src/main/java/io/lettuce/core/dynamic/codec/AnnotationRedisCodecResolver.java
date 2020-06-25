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
package io.lettuce.core.dynamic.codec;

import java.util.*;
import java.util.stream.Collectors;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.CommandMethod;
import io.lettuce.core.dynamic.annotation.Key;
import io.lettuce.core.dynamic.annotation.Value;
import io.lettuce.core.dynamic.parameter.Parameter;
import io.lettuce.core.dynamic.support.ClassTypeInformation;
import io.lettuce.core.dynamic.support.TypeInformation;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;

/**
 * Annotation-based {@link RedisCodecResolver}. Considers {@code @Key} and {@code @Value} annotations of method parameters to
 * determine a {@link RedisCodec} that is able to handle all involved types.
 *
 * @author Mark Paluch
 * @author Manyanda Chitimbo
 * @since 5.0
 * @see Key
 * @see Value
 */
public class AnnotationRedisCodecResolver implements RedisCodecResolver {

    private final List<RedisCodec<?, ?>> codecs;

    /**
     * Creates a new {@link AnnotationRedisCodecResolver} given a {@link List} of {@link RedisCodec}s.
     *
     * @param codecs must not be {@code null}.
     */
    public AnnotationRedisCodecResolver(List<RedisCodec<?, ?>> codecs) {

        LettuceAssert.notNull(codecs, "List of RedisCodecs must not be null");

        this.codecs = LettuceLists.unmodifiableList(codecs);
    }

    @Override
    public RedisCodec<?, ?> resolve(CommandMethod commandMethod) {

        LettuceAssert.notNull(commandMethod, "CommandMethod must not be null");

        Set<Class<?>> keyTypes = findTypes(commandMethod, Key.class);
        Set<Class<?>> valueTypes = findTypes(commandMethod, Value.class);

        if (keyTypes.isEmpty() && valueTypes.isEmpty()) {

            Voted<RedisCodec<?, ?>> voted = voteForTypeMajority(commandMethod);
            if (voted != null) {
                return voted.subject;
            }

            return codecs.get(0);
        }

        if ((keyTypes.size() == 1 && hasAtMostOne(valueTypes)) || (valueTypes.size() == 1 && hasAtMostOne(keyTypes))) {
            RedisCodec<?, ?> resolvedCodec = resolveCodec(keyTypes, valueTypes);
            if (resolvedCodec != null) {
                return resolvedCodec;
            }
        }

        throw new IllegalStateException(String.format("Cannot resolve Codec for method %s", commandMethod.getMethod()));
    }

    private boolean hasAtMostOne(Collection<?> collection) {
        return collection.size() <= 1;
    }

    private Voted<RedisCodec<?, ?>> voteForTypeMajority(CommandMethod commandMethod) {

        List<Voted<RedisCodec<?, ?>>> votes = codecs.stream().map(redisCodec -> new Voted<RedisCodec<?, ?>>(redisCodec, 0))
                .collect(Collectors.toList());

        commandMethod.getParameters().getBindableParameters().forEach(parameter -> {
            vote(votes, parameter);
        });

        Collections.sort(votes);
        if (votes.isEmpty()) {
            return null;
        }

        Voted<RedisCodec<?, ?>> voted = votes.get(votes.size() - 1);

        if (voted.votes == 0) {
            return null;
        }

        return voted;
    }

    @SuppressWarnings("rawtypes")
    private static void vote(List<Voted<RedisCodec<?, ?>>> votes, Parameter parameter) {

        for (Voted<RedisCodec<?, ?>> vote : votes) {

            ClassTypeInformation<? extends RedisCodec> typeInformation = ClassTypeInformation.from(vote.subject.getClass());

            TypeInformation<?> superTypeInformation = typeInformation.getSuperTypeInformation(RedisCodec.class);

            List<TypeInformation<?>> typeArguments = superTypeInformation.getTypeArguments();

            if (typeArguments.size() != 2) {
                continue;
            }

            TypeInformation<?> parameterType = parameter.getTypeInformation();
            TypeInformation<?> parameterKeyType = ParameterWrappers.getKeyType(parameterType);
            TypeInformation<?> parameterValueType = ParameterWrappers.getValueType(parameterType);

            TypeInformation<?> keyType = typeArguments.get(0);
            TypeInformation<?> valueType = typeArguments.get(1);

            if (keyType.isAssignableFrom(parameterKeyType)) {
                vote.votes++;
            }

            if (valueType.isAssignableFrom(parameterValueType)) {
                vote.votes++;
            }
        }
    }

    private RedisCodec<?, ?> resolveCodec(Set<Class<?>> keyTypes, Set<Class<?>> valueTypes) {
        Class<?> keyType = keyTypes.isEmpty() ? null : keyTypes.iterator().next();
        Class<?> valueType = valueTypes.isEmpty() ? null : valueTypes.iterator().next();

        for (RedisCodec<?, ?> codec : codecs) {
            ClassTypeInformation<?> typeInformation = ClassTypeInformation.from(codec.getClass());
            TypeInformation<?> keyTypeArgument = typeInformation.getTypeArgument(RedisCodec.class, 0);
            TypeInformation<?> valueTypeArgument = typeInformation.getTypeArgument(RedisCodec.class, 1);

            if (keyTypeArgument == null || valueTypeArgument == null) {
                continue;
            }

            boolean keyMatch = false;
            boolean valueMatch = false;

            if (keyType != null) {
                keyMatch = keyTypeArgument.isAssignableFrom(ClassTypeInformation.from(keyType));
            }

            if (valueType != null) {
                valueMatch = valueTypeArgument.isAssignableFrom(ClassTypeInformation.from(valueType));
            }

            if (keyType != null && valueType != null && keyMatch && valueMatch) {
                return codec;
            }

            if (keyType != null && valueType == null && keyMatch) {
                return codec;
            }

            if (keyType == null && valueType != null && valueMatch) {
                return codec;
            }
        }

        return null;
    }

    Set<Class<?>> findTypes(CommandMethod commandMethod, Class<?> annotation) {

        Set<Class<?>> types = new LinkedHashSet<>();

        for (Parameter parameter : commandMethod.getParameters().getBindableParameters()) {

            types.addAll(parameter.getAnnotations().stream()
                    .filter(parameterAnnotation -> annotation.isAssignableFrom(parameterAnnotation.getClass()))
                    .map(parameterAnnotation -> {

                        TypeInformation<?> typeInformation = parameter.getTypeInformation();

                        if (annotation == Key.class && ParameterWrappers.hasKeyType(typeInformation)) {
                            TypeInformation<?> parameterKeyType = ParameterWrappers.getKeyType(typeInformation);
                            return parameterKeyType.getType();
                        }

                        return ParameterWrappers.getValueType(typeInformation).getType();

                    }).collect(Collectors.toList()));
        }

        return types;
    }

    private static class Voted<T> implements Comparable<Voted<?>> {

        private T subject;

        private int votes;

        Voted(T subject, int votes) {
            this.subject = subject;
            this.votes = votes;
        }

        @Override
        public int compareTo(Voted<?> o) {
            return votes - o.votes;
        }

    }

    /**
     * Parameter wrapper support for types that encapsulate one or more parameter values.
     */
    protected static class ParameterWrappers {

        private static final Set<Class<?>> WRAPPERS = new HashSet<>();

        private static final Set<Class<?>> WITH_KEY_TYPE = new HashSet<>();

        private static final Set<Class<?>> WITH_VALUE_TYPE = new HashSet<>();

        static {

            WRAPPERS.add(io.lettuce.core.Value.class);
            WRAPPERS.add(io.lettuce.core.KeyValue.class);
            WRAPPERS.add(io.lettuce.core.ScoredValue.class);
            WRAPPERS.add(io.lettuce.core.Range.class);

            WRAPPERS.add(List.class);
            WRAPPERS.add(Collection.class);
            WRAPPERS.add(Set.class);
            WRAPPERS.add(Iterable.class);
            WRAPPERS.add(Map.class);

            WITH_VALUE_TYPE.add(io.lettuce.core.Value.class);
            WITH_VALUE_TYPE.add(io.lettuce.core.KeyValue.class);
            WITH_KEY_TYPE.add(io.lettuce.core.KeyValue.class);
            WITH_VALUE_TYPE.add(io.lettuce.core.ScoredValue.class);

            WITH_KEY_TYPE.add(Map.class);
            WITH_VALUE_TYPE.add(Map.class);
        }

        /**
         * @param typeInformation must not be {@code null}.
         * @return {@code true} if {@code parameterClass} is a parameter wrapper.
         */
        public static boolean supports(TypeInformation<?> typeInformation) {
            return WRAPPERS.contains(typeInformation.getType())
                    || (typeInformation.getType().isArray() && !(typeInformation.getType().equals(byte[].class)));
        }

        /**
         * @param typeInformation must not be {@code null}.
         * @return {@code true} if the type has a key type variable.
         */
        public static boolean hasKeyType(TypeInformation<?> typeInformation) {
            return WITH_KEY_TYPE.contains(typeInformation.getType());
        }

        /**
         * @param typeInformation must not be {@code null}.
         * @return {@code true} if the type has a value type variable.
         */
        public static boolean hasValueType(TypeInformation<?> typeInformation) {
            return WITH_VALUE_TYPE.contains(typeInformation.getType());
        }

        /**
         * @param typeInformation must not be {@code null}.
         * @return the key type.
         */
        public static TypeInformation<?> getKeyType(TypeInformation<?> typeInformation) {

            if (!supports(typeInformation) || !hasKeyType(typeInformation)) {
                return typeInformation;
            }

            return typeInformation.getComponentType();
        }

        /**
         * @param typeInformation must not be {@code null}.
         * @return the value type.
         */
        public static TypeInformation<?> getValueType(TypeInformation<?> typeInformation) {

            if (!supports(typeInformation) || typeInformation.getComponentType() == null) {
                return typeInformation;
            }

            if (!hasValueType(typeInformation)) {
                return typeInformation.getComponentType();
            }

            List<TypeInformation<?>> typeArguments = typeInformation.getTypeArguments();

            if (hasKeyType(typeInformation)) {
                return typeArguments.get(1);
            }

            return typeArguments.get(0);
        }

    }

}
