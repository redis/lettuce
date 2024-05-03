package io.lettuce.core.dynamic;

/**
 * Strategy interface to resolve a {@link CommandFactory}.
 *
 * @since 5.0
 */
@FunctionalInterface
interface CommandFactoryResolver {

    /**
     * Resolve a {@link CommandFactory} given a{@link DeclaredCommandMethod} and {@link RedisCommandsMetadata}.
     *
     * @param method must not be {@code null}.
     * @param redisCommandsMetadata must not be {@code null}.
     * @return the {@link CommandFactory}.
     */
    CommandFactory resolveRedisCommandFactory(CommandMethod method, RedisCommandsMetadata redisCommandsMetadata);

}
