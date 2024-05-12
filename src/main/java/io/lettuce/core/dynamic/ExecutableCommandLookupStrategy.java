package io.lettuce.core.dynamic;

import java.lang.reflect.Method;

/**
 * Strategy interface to resolve {@link ExecutableCommand} from a {@link Method} and {@link RedisCommandsMetadata}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
interface ExecutableCommandLookupStrategy {

    /**
     * Resolve a {@link ExecutableCommand} given the {@link Method} and {@link RedisCommandsMetadata}.
     *
     * @param method must not be {@code null}.
     * @param metadata must not be {@code null}.
     * @return the {@link ExecutableCommand}.
     */
    ExecutableCommand resolveCommandMethod(CommandMethod method, RedisCommandsMetadata metadata);

}
