/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command implementation class as a provider for a specific command API interface. Classes annotated with this
 * annotation are discovered via {@link java.util.ServiceLoader} and must provide:
 * <ul>
 * <li>A static {@code create(StatefulConnection)} method that returns the command API instance</li>
 * <li>Optionally, a static {@code supports(StatefulConnection)} method for custom connection filtering</li>
 * </ul>
 *
 * <p>
 * Example:
 * 
 * <pre>
 * {@code
 * &#64;CommandsFor(api = RedisAsyncCommands.class, connection = StatefulRedisConnection.class)
 * public class RedisAsyncCommandsImpl<K, V> implements RedisAsyncCommands<K, V> {
 *
 *     public static Object create(StatefulConnection<?, ?> connection) {
 *         StatefulRedisConnection conn = (StatefulRedisConnection) connection;
 *         return new RedisAsyncCommandsImpl<>(conn, conn.getCodec(), ...);
 *     }
 * }
 * }
 * </pre>
 *
 * @since 7.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandsFor {

    /**
     * @return the command API interface this implementation provides
     */
    Class<?> api();

    /**
     * @return the connection type this implementation supports
     */
    Class<?> connection();

}
