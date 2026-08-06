/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verify that no entry of {@link KnownApiDeviations} has gone stale. Every table is an opt-out: a key that matches no method
 * silently disables nothing today, but keeps claiming a deviation that no longer exists, and — because the loosest key form is
 * a bare method name — a leftover entry silently exempts the next command that happens to share that name.
 * <p>
 * Each table is registered in {@link Table} together with the flavor its keys are written against, mirroring the call sites in
 * the consistency tests (e.g. {@code NOT_ON_SYNC_API} is looked up with async and reactive methods, everything else with sync
 * methods). {@link #everyDeviationTableIsRegistered()} makes sure a newly added table cannot skip this check.
 * <p>
 * The coroutine tables live in {@code KnownKotlinApiDeviations} (Kotlin test sources) and are checked the same way by
 * {@code KotlinCoroutinesConsistencyUnitTests.deviationTableEntriesAreNotStale}, because Java test sources cannot reference the
 * Kotlin ones.
 */
@Tag(UNIT_TEST)
class DeviationTableStalenessUnitTests {

    /**
     * The flavor a table's keys are matched against, mirroring how the consistency tests look the table up.
     */
    enum Scope {
        SYNC, ASYNC, REACTIVE, NODE_SELECTION_ASYNC, COMMAND_BUILDER, COMMAND_GROUP_NAME
    }

    /**
     * The registry of deviation tables. The set of entries is cross-checked against the public fields of
     * {@link KnownApiDeviations} by {@link #everyDeviationTableIsRegistered()}.
     */
    enum Table {

        NOT_ON_SYNC_API(KnownApiDeviations.NOT_ON_SYNC_API, Scope.ASYNC, Scope.REACTIVE),

        KEEP_SYNC_RESULT_TYPE_ASYNC(KnownApiDeviations.KEEP_SYNC_RESULT_TYPE_ASYNC, Scope.SYNC),

        KEEP_SYNC_RESULT_TYPE_REACTIVE(KnownApiDeviations.KEEP_SYNC_RESULT_TYPE_REACTIVE, Scope.SYNC),

        AGGREGATE_FLAVOR_SPECIFIC_RETURN(KnownApiDeviations.AGGREGATE_FLAVOR_SPECIFIC_RETURN, Scope.SYNC),

        NOT_ON_REACTIVE_AGGREGATE(KnownApiDeviations.NOT_ON_REACTIVE_AGGREGATE, Scope.SYNC),

        REACTIVE_ONLY(KnownApiDeviations.REACTIVE_ONLY, Scope.REACTIVE),

        REACTIVE_EXTRA_DEPRECATED(KnownApiDeviations.REACTIVE_EXTRA_DEPRECATED, Scope.SYNC),

        REACTIVE_PARAMETER_FLAVOR_SPECIFIC(KnownApiDeviations.REACTIVE_PARAMETER_FLAVOR_SPECIFIC, Scope.SYNC),

        FORCE_FLUX(KnownApiDeviations.FORCE_FLUX, Scope.SYNC),

        REACTIVE_VALUE_WRAP(KnownApiDeviations.REACTIVE_VALUE_WRAP, Scope.SYNC),

        REACTIVE_RESULT_OVERRIDES(KnownApiDeviations.REACTIVE_RESULT_OVERRIDES.keySet(), Scope.SYNC),

        NODE_SELECTION_EXCLUDED(KnownApiDeviations.NODE_SELECTION_EXCLUDED, Scope.SYNC),

        NOT_ON_NODE_SELECTION_SYNC(KnownApiDeviations.NOT_ON_NODE_SELECTION_SYNC, Scope.SYNC, Scope.NODE_SELECTION_ASYNC),

        NODE_SELECTION_EXTRA_DEPRECATED(KnownApiDeviations.NODE_SELECTION_EXTRA_DEPRECATED, Scope.SYNC),

        NODE_SELECTION_AGGREGATE_PENDING(KnownApiDeviations.NODE_SELECTION_AGGREGATE_PENDING, Scope.COMMAND_GROUP_NAME),

        BUILDER_EXCLUDED(KnownApiDeviations.BUILDER_EXCLUDED, Scope.SYNC),

        BUILDER_ALIASES(KnownApiDeviations.BUILDER_ALIASES.keySet(), Scope.SYNC),

        BUILDER_INTERNAL(KnownApiDeviations.BUILDER_INTERNAL, Scope.COMMAND_BUILDER);

        private final Set<String> keys;

        private final Set<Scope> scopes;

        Table(Set<String> keys, Scope... scopes) {
            this.keys = keys;
            this.scopes = EnumSet.copyOf(Arrays.asList(scopes));
        }

        Set<String> keys() {
            return keys;
        }

        Set<Scope> scopes() {
            return scopes;
        }

    }

    @ParameterizedTest
    @EnumSource(Table.class)
    void everyDeviationTableEntryMatchesSomething(Table table) {

        Set<String> unmatched = new TreeSet<>();

        for (String key : table.keys()) {
            if (!matchesAnything(key, table.scopes())) {
                unmatched.add(key);
            }
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(unmatched)
                .as("stale %s entries — they match no %s method and should be removed", table.name(), table.scopes()).isEmpty();
        softly.assertAll();
    }

    /**
     * Every table declared on {@link KnownApiDeviations} must be registered in {@link Table}, otherwise it escapes the
     * staleness check.
     */
    @Test
    void everyDeviationTableIsRegistered() {

        Set<String> registered = new HashSet<>();
        for (Table table : Table.values()) {
            registered.add(table.name());
        }

        Set<String> unregistered = new TreeSet<>();
        for (Field field : KnownApiDeviations.class.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Set.class.isAssignableFrom(field.getType()) && !Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            if (!registered.contains(field.getName())) {
                unregistered.add(field.getName());
            }
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(unregistered)
                .as("deviation tables missing from the staleness registry — add them to DeviationTableStalenessUnitTests.Table")
                .isEmpty();
        softly.assertAll();
    }

    private static boolean matchesAnything(String key, Set<Scope> scopes) {

        for (Scope scope : scopes) {
            if (matchesAnything(key, scope)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnything(String key, Scope scope) {

        Set<String> singleton = Collections.singleton(key);

        switch (scope) {
            case COMMAND_GROUP_NAME:
                for (CommandInterfaces group : CommandInterfaces.values()) {
                    if (group.name().equals(key)) {
                        return true;
                    }
                }
                return false;

            case COMMAND_BUILDER:
                return commandBuilderMethodNames().contains(key);

            default:
                for (CommandInterfaces group : CommandInterfaces.values()) {
                    Class<?> flavor = flavor(group, scope);
                    if (flavor != null && matches(singleton, flavor, group.sync())) {
                        return true;
                    }
                }
                for (AggregateInterfaces aggregate : AggregateInterfaces.values()) {
                    Class<?> flavor = flavor(aggregate, scope);
                    if (flavor != null && matches(singleton, flavor, aggregate.sync())) {
                        return true;
                    }
                }
                return false;
        }
    }

    /**
     * The interface whose methods a key of the given scope is matched against. Keys stay qualified by the <em>sync</em> group
     * name in all scopes, mirroring the lookups in the consistency tests.
     */
    private static Class<?> flavor(CommandInterfaces group, Scope scope) {
        switch (scope) {
            case SYNC:
                return group.sync();
            case ASYNC:
                return group.async();
            case REACTIVE:
                return group.reactive();
            case NODE_SELECTION_ASYNC:
                return group.nodeSelectionAsync();
            default:
                return null;
        }
    }

    private static Class<?> flavor(AggregateInterfaces aggregate, Scope scope) {
        switch (scope) {
            case SYNC:
                return aggregate.sync();
            case ASYNC:
                return aggregate.async();
            case REACTIVE:
                return aggregate.reactive();
            default:
                return null;
        }
    }

    private static boolean matches(Set<String> singleton, Class<?> flavor, Class<?> syncGroup) {

        for (Method method : TypeSignatures.apiMethods(flavor)) {
            if (KnownApiDeviations.contains(singleton, method, syncGroup)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> commandBuilderMethodNames() {

        Set<String> names = new LinkedHashSet<>();

        for (CommandInterfaces group : CommandInterfaces.values()) {
            try {
                Class<?> builder = Class.forName(group.commandBuilderClassName());
                for (Method method : builder.getDeclaredMethods()) {
                    if (io.lettuce.core.protocol.RedisCommand.class.isAssignableFrom(method.getReturnType())) {
                        names.add(method.getName());
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unknown command builder " + group.commandBuilderClassName(), e);
            }
        }

        return names;
    }

}
