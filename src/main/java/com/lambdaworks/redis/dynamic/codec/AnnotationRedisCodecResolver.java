package com.lambdaworks.redis.dynamic.codec;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.lambdaworks.redis.dynamic.annotation.Key;
import com.lambdaworks.redis.dynamic.annotation.Value;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.CommandMethod;
import com.lambdaworks.redis.dynamic.parameter.Parameter;
import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceLists;

/**
 * Annotation-based {@link RedisCodecResolver}. Considers {@code @Key} and {@code @Value} annotations of method parameters to
 * determine a {@link RedisCodec} that is able to handle all involved types.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see Key
 * @see Value
 */
public class AnnotationRedisCodecResolver implements RedisCodecResolver {

    private final List<RedisCodec<?, ?>> codecs;

    /**
     * Creates a new {@link AnnotationRedisCodecResolver} given a {@link List} of {@link RedisCodec}s.
     *
     * @param codecs must not be {@literal null}.
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

        if ((keyTypes.size() == 1 && (valueTypes.isEmpty() || valueTypes.size() == 1))
                || (valueTypes.size() == 1 && (keyTypes.isEmpty() || keyTypes.size() == 1))) {

            RedisCodec<?, ?> codec = resolveCodec(keyTypes, valueTypes);
            if (codec != null) {
                return codec;
            }
        }

        throw new IllegalStateException(String.format("Cannot resolve Codec for method %s", commandMethod.getMethod()));
    }

    private Voted<RedisCodec<?, ?>> voteForTypeMajority(CommandMethod commandMethod) {

        List<Voted<RedisCodec<?, ?>>> votes = codecs.stream().map(redisCodec -> new Voted<RedisCodec<?, ?>>(redisCodec, 0))
                .collect(Collectors.toList());

        commandMethod.getParameters().getBindableParameters().forEach(parameter -> {

            for (Voted<RedisCodec<?, ?>> vote : votes) {
                ClassTypeInformation<? extends RedisCodec> typeInformation = ClassTypeInformation.from(vote.subject.getClass());

                TypeInformation<?> superTypeInformation = typeInformation.getSuperTypeInformation(RedisCodec.class);

                List<TypeInformation<?>> typeArguments = superTypeInformation.getTypeArguments();

                if (typeArguments.size() != 2) {
                    continue;
                }

                TypeInformation<?> keyType = typeArguments.get(0);
                TypeInformation<?> valueType = typeArguments.get(1);

                if (keyType.isAssignableFrom(parameter.getTypeInformation())) {
                    vote.votes++;
                }

                if (valueType.isAssignableFrom(parameter.getTypeInformation())) {
                    vote.votes++;
                }
            }
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

    private Set<Class<?>> findTypes(CommandMethod commandMethod, Class<?> annotation) {

        Set<Class<?>> types = new HashSet<>();

        for (Parameter parameter : commandMethod.getParameters().getBindableParameters()) {

            types.addAll(parameter.getAnnotations().stream()
                    .filter(parameterAnnotation -> annotation.isAssignableFrom(parameterAnnotation.getClass()))
                    .map(parameterAnnotation -> {
                        if (parameter.getTypeInformation().isCollectionLike()
                                && !parameter.getTypeInformation().getType().isArray()) {
                            return parameter.getTypeInformation().getComponentType().getType();
                        }
                        return parameter.getTypeInformation().getType();

                    }).collect(Collectors.toList()));
        }

        return types;
    }

    private static class Voted<T> implements Comparable<Voted<?>> {

        private T subject;
        private int votes;

        public Voted(T subject, int votes) {
            this.subject = subject;
            this.votes = votes;
        }

        @Override
        public int compareTo(Voted<?> o) {
            return votes - o.votes;
        }
    }
}
