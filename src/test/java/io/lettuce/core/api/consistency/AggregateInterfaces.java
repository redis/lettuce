/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * Catalog of the aggregate ("umbrella") command interfaces and their per-flavor counterparts. Unlike the per-group interfaces
 * in {@link CommandInterfaces}, these were never generated from a template: they extend the group interfaces and add command
 * methods of their own ({@code auth}, {@code select}, the {@code CLUSTER} commands, the PubSub subscriptions, …), which must
 * still stay in lockstep across flavors.
 * <p>
 * Only the Java flavors are listed. The coroutine aggregate is derived by naming convention on the Kotlin side
 * ({@code AggregateInterfaces.coroutines()} in {@code KnownKotlinApiDeviations.kt}), so these Java test sources carry no
 * compile-time reference to the Kotlin flavor — and that derivation yields {@code null} for
 * {@code RedisAdvancedClusterCommands} and {@code RedisPubSubCommands}, which have no coroutine counterpart at all.
 *
 * @see CommandInterfaces
 */
public enum AggregateInterfaces {

    STANDALONE(RedisCommands.class, RedisAsyncCommands.class, RedisReactiveCommands.class),

    CLUSTER(RedisClusterCommands.class, RedisClusterAsyncCommands.class, RedisClusterReactiveCommands.class),

    ADVANCED_CLUSTER(RedisAdvancedClusterCommands.class, RedisAdvancedClusterAsyncCommands.class,
            RedisAdvancedClusterReactiveCommands.class),

    PUBSUB(RedisPubSubCommands.class, RedisPubSubAsyncCommands.class, RedisPubSubReactiveCommands.class);

    private final Class<?> sync;

    private final Class<?> async;

    private final Class<?> reactive;

    AggregateInterfaces(Class<?> sync, Class<?> async, Class<?> reactive) {
        this.sync = sync;
        this.async = async;
        this.reactive = reactive;
    }

    public Class<?> sync() {
        return sync;
    }

    public Class<?> async() {
        return async;
    }

    public Class<?> reactive() {
        return reactive;
    }

}
