/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;

/**
 * Verify that the aggregate command interfaces extend the per-group interface of every command group they are supposed to
 * cover, so that a newly registered command group cannot be forgotten on the umbrella interfaces — and that the methods
 * declared directly on the aggregates ({@code auth}, {@code select}, the {@code CLUSTER} commands, …) stay in lockstep across
 * the sync, async and reactive flavors.
 */
@Tag(UNIT_TEST)
class AggregateInterfaceConsistencyUnitTests {

    private static final Set<CommandInterfaces> STANDALONE_GROUPS = EnumSet
            .complementOf(EnumSet.of(CommandInterfaces.SENTINEL));

    private static final Set<CommandInterfaces> CLUSTER_GROUPS = EnumSet
            .complementOf(EnumSet.of(CommandInterfaces.SENTINEL, CommandInterfaces.TRANSACTIONAL));

    @Test
    void standaloneAggregatesCoverAllGroups() {

        SoftAssertions softly = new SoftAssertions();

        for (CommandInterfaces group : STANDALONE_GROUPS) {
            assertExtends(softly, RedisCommands.class, group.sync());
            assertExtends(softly, RedisAsyncCommands.class, group.async());
            assertExtends(softly, RedisReactiveCommands.class, group.reactive());
            assertExtends(softly, io.lettuce.core.api.coroutines.RedisCoroutinesCommands.class, group.coroutines());
        }

        softly.assertAll();
    }

    @Test
    void clusterAggregatesCoverAllClusterGroups() {

        SoftAssertions softly = new SoftAssertions();

        for (CommandInterfaces group : CLUSTER_GROUPS) {
            assertExtends(softly, RedisClusterCommands.class, group.sync());
            assertExtends(softly, RedisClusterAsyncCommands.class, group.async());
            assertExtends(softly, RedisClusterReactiveCommands.class, group.reactive());
            assertExtends(softly, RedisClusterCoroutinesCommands.class, group.coroutines());
        }

        softly.assertAll();
    }

    @Test
    void nodeSelectionAggregatesCoverAllNodeSelectionGroups() {

        SoftAssertions softly = new SoftAssertions();

        for (CommandInterfaces group : CLUSTER_GROUPS) {
            if (!group.hasNodeSelection() || KnownApiDeviations.NODE_SELECTION_AGGREGATE_PENDING.contains(group.name())) {
                continue;
            }
            assertExtends(softly, NodeSelectionCommands.class, group.nodeSelectionSync());
            assertExtends(softly, NodeSelectionAsyncCommands.class, group.nodeSelectionAsync());
        }

        softly.assertAll();
    }

    private static void assertExtends(SoftAssertions softly, Class<?> aggregate, Class<?> groupInterface) {
        softly.assertThat(groupInterface.isAssignableFrom(aggregate))
                .as("%s must extend %s", aggregate.getSimpleName(), groupInterface.getSimpleName()).isTrue();
    }

    /**
     * The sync/async/reactive triples of the aggregate interfaces that declare command methods of their own.
     */
    private static final Class<?>[][] AGGREGATE_FLAVORS = {
            { RedisCommands.class, RedisAsyncCommands.class, RedisReactiveCommands.class },
            { RedisClusterCommands.class, RedisClusterAsyncCommands.class, RedisClusterReactiveCommands.class },
            { RedisAdvancedClusterCommands.class, RedisAdvancedClusterAsyncCommands.class,
                    RedisAdvancedClusterReactiveCommands.class } };

    @Test
    void aggregateDeclaredMethodsExistOnAsyncAndReactiveAggregates() {

        SoftAssertions softly = new SoftAssertions();

        for (Class<?>[] flavors : AGGREGATE_FLAVORS) {
            Class<?> sync = flavors[0];

            for (Method syncMethod : TypeSignatures.apiMethods(sync)) {

                assertCounterpart(softly, sync, syncMethod, flavors[1],
                        TypeSignatures.expectedAsyncReturnType(syncMethod, sync));

                if (!KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_REACTIVE_AGGREGATE, syncMethod, sync)) {
                    assertCounterpart(softly, sync, syncMethod, flavors[2],
                            TypeSignatures.expectedReactiveReturnType(syncMethod, sync));
                }
            }
        }

        softly.assertAll();
    }

    @Test
    void asyncAndReactiveAggregateDeclaredMethodsExistOnSyncAggregate() {

        SoftAssertions softly = new SoftAssertions();

        for (Class<?>[] flavors : AGGREGATE_FLAVORS) {
            Class<?> sync = flavors[0];

            for (Class<?> flavor : new Class<?>[] { flavors[1], flavors[2] }) {
                for (Method method : TypeSignatures.apiMethods(flavor)) {
                    if (KnownApiDeviations.contains(KnownApiDeviations.NOT_ON_SYNC_API, method, sync)
                            || KnownApiDeviations.contains(KnownApiDeviations.REACTIVE_ONLY, method, sync)) {
                        continue;
                    }
                    if (TypeSignatures.findCounterpart(sync, method) == null) {
                        softly.fail("%s is missing on %s", TypeSignatures.describe(flavor, method), sync.getSimpleName());
                    }
                }
            }
        }

        softly.assertAll();
    }

    private void assertCounterpart(SoftAssertions softly, Class<?> sync, Method syncMethod, Class<?> target,
            String expectedReturnType) {

        Method counterpart = TypeSignatures.findCounterpart(target, syncMethod);
        if (counterpart == null) {
            softly.fail("%s is missing on %s", TypeSignatures.describe(sync, syncMethod), target.getSimpleName());
            return;
        }

        if (KnownApiDeviations.contains(KnownApiDeviations.AGGREGATE_FLAVOR_SPECIFIC_RETURN, syncMethod, sync)) {
            return;
        }

        softly.assertThat(TypeSignatures.normalize(counterpart.getGenericReturnType()))
                .as("return type of %s", TypeSignatures.describe(target, counterpart)).isEqualTo(expectedReturnType);
    }

}
