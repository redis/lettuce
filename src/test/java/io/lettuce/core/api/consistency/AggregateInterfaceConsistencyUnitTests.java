/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.util.EnumSet;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;

/**
 * Verify that the aggregate command interfaces extend the per-group interface of every command group they are supposed to
 * cover, so that a newly registered command group cannot be forgotten on the umbrella interfaces.
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

}
