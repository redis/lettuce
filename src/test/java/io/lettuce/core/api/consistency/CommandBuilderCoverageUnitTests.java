/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;

/**
 * Verify that the command interfaces and the command builders cover each other: every interface command has a builder method
 * that constructs it (each command group maps to its builder, e.g. STRING → {@code RedisCommandBuilder}, JSON →
 * {@code RedisJsonCommandBuilder}), and every command-producing builder method is reachable from an interface. This catches
 * dead builder methods and interface methods without an implementation path.
 * <p>
 * Matching is name-based because builder overloads legitimately differ in shape from the API overloads (e.g. streaming variants
 * share a builder method). Renamed counterparts are recorded in {@link KnownApiDeviations#BUILDER_ALIASES};
 * implementation-detail builder methods in {@link KnownApiDeviations#BUILDER_INTERNAL}.
 */
@Tag(UNIT_TEST)
class CommandBuilderCoverageUnitTests {

    @Test
    void syncMethodsHaveBuilderCounterparts() throws Exception {

        SoftAssertions softly = new SoftAssertions();

        for (CommandInterfaces group : CommandInterfaces.values()) {

            Set<String> builderNames = commandMethodNames(Class.forName(group.commandBuilderClassName()));

            Set<String> missing = new HashSet<>();
            for (Method syncMethod : TypeSignatures.apiMethods(group.sync())) {
                if (KnownApiDeviations.contains(KnownApiDeviations.BUILDER_EXCLUDED, syncMethod, group.sync())) {
                    continue;
                }
                String builderName = KnownApiDeviations.BUILDER_ALIASES.getOrDefault(syncMethod.getName(),
                        syncMethod.getName());
                if (!builderNames.contains(builderName)) {
                    missing.add(syncMethod.getName());
                }
            }

            softly.assertThat(missing)
                    .as("%s methods missing on %s", group.sync().getSimpleName(), group.commandBuilderClassName()).isEmpty();
        }

        softly.assertAll();
    }

    @Test
    void builderMethodsAreReachableFromCommandInterfaces() throws Exception {

        Map<String, Set<String>> reachableNamesByBuilder = new HashMap<>();

        for (CommandInterfaces group : CommandInterfaces.values()) {
            Set<String> names = reachableNamesByBuilder.computeIfAbsent(group.commandBuilderClassName(),
                    builder -> new HashSet<>());
            collectMethodNames(names, group.sync());
            collectMethodNames(names, group.async());
        }

        // cluster-only commands (CLUSTER *) are declared on the cluster interfaces and built by RedisCommandBuilder
        Set<String> redisBuilderNames = reachableNamesByBuilder.get(CommandInterfaces.STRING.commandBuilderClassName());
        collectMethodNames(redisBuilderNames, RedisClusterCommands.class);
        collectMethodNames(redisBuilderNames, RedisAdvancedClusterCommands.class);

        // interface methods that map to a renamed builder method make that builder method reachable
        reachableNamesByBuilder.values().forEach(names -> KnownApiDeviations.BUILDER_ALIASES.forEach((api, builder) -> {
            if (names.contains(api)) {
                names.add(builder);
            }
        }));

        SoftAssertions softly = new SoftAssertions();

        for (Map.Entry<String, Set<String>> entry : reachableNamesByBuilder.entrySet()) {

            Class<?> builder = Class.forName(entry.getKey());
            Set<String> unreachable = new HashSet<>();

            for (String name : commandMethodNames(builder)) {
                if (!entry.getValue().contains(name) && !KnownApiDeviations.BUILDER_INTERNAL.contains(name)) {
                    unreachable.add(name);
                }
            }

            softly.assertThat(unreachable).as("dead command methods on %s (no interface counterpart)", builder.getSimpleName())
                    .isEmpty();
        }

        softly.assertAll();
    }

    /**
     * The command-producing methods of a builder: declared, non-private, non-static methods returning a
     * {@code RedisCommand}/{@code Command}.
     */
    private static Set<String> commandMethodNames(Class<?> builder) {

        Set<String> names = new HashSet<>();
        for (Method method : builder.getDeclaredMethods()) {
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers()) || Modifier.isPrivate(method.getModifiers())) {
                continue;
            }
            if (io.lettuce.core.protocol.RedisCommand.class.isAssignableFrom(method.getReturnType())) {
                names.add(method.getName());
            }
        }
        return names;
    }

    private static void collectMethodNames(Set<String> target, Class<?> type) {
        for (Method method : type.getMethods()) {
            target.add(method.getName());
        }
    }

}
