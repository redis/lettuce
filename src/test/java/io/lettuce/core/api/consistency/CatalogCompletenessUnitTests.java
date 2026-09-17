/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.api.async.NodeSelectionAsyncCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;

/**
 * Verify that {@link CommandInterfaces} and {@link AggregateInterfaces} together account for <em>every</em> command interface
 * that ships in {@code src/main}. Both catalogs are maintained by hand, and an interface missing from them is invisible to the
 * entire consistency suite — the exact failure the suite exists to prevent.
 * <p>
 * The check works off the source tree rather than a classpath scan so that it also covers the Kotlin coroutine interfaces,
 * whose per-flavor naming ({@code *CoroutinesCommands.kt} next to {@code *CoroutinesCommandsImpl.kt}) is easiest to filter by
 * file name.
 */
@Tag(UNIT_TEST)
class CatalogCompletenessUnitTests {

    private static final File JAVA_ROOT = CommandInterfaceSources.JAVA_ROOT;

    private static final File KOTLIN_ROOT = CommandInterfaceSources.KOTLIN_ROOT;

    private static final String[] JAVA_PACKAGES = CommandInterfaceSources.JAVA_PACKAGES;

    private static final String[] KOTLIN_PACKAGES = CommandInterfaceSources.KOTLIN_PACKAGES;

    @Test
    void everyCommandInterfaceIsRegisteredInACatalog() {

        Set<String> registered = registeredInterfaceNames();
        Set<String> unregistered = new TreeSet<>();

        for (String pkg : JAVA_PACKAGES) {
            collectInterfaceNames(unregistered, new File(JAVA_ROOT, pkg), ".java", registered);
        }
        for (String pkg : KOTLIN_PACKAGES) {
            collectInterfaceNames(unregistered, new File(KOTLIN_ROOT, pkg), ".kt", registered);
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(unregistered).as(
                "command interfaces missing from CommandInterfaces/AggregateInterfaces — register them so the consistency suite covers them")
                .isEmpty();
        softly.assertAll();
    }

    /**
     * Every scanned package must exist — a renamed or moved package would otherwise silently reduce the scan to nothing.
     */
    @Test
    void everyScannedPackageExists() {

        SoftAssertions softly = new SoftAssertions();

        for (String pkg : JAVA_PACKAGES) {
            File directory = new File(JAVA_ROOT, pkg);
            softly.assertThat(directory).as("scanned package %s", directory).isDirectory();
        }
        for (String pkg : KOTLIN_PACKAGES) {
            File directory = new File(KOTLIN_ROOT, pkg);
            softly.assertThat(directory).as("scanned package %s", directory).isDirectory();
        }

        softly.assertAll();
    }

    private static void collectInterfaceNames(Set<String> unregistered, File directory, String extension,
            Set<String> registered) {

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            String name = file.getName();
            if (!name.endsWith("Commands" + extension)) {
                continue;
            }
            String simpleName = name.substring(0, name.length() - extension.length());
            if (!registered.contains(simpleName)) {
                unregistered.add(simpleName);
            }
        }
    }

    private static Set<String> registeredInterfaceNames() {

        Set<String> names = new LinkedHashSet<>();

        for (CommandInterfaces group : CommandInterfaces.values()) {
            add(names, group.sync(), group.async(), group.reactive(), group.nodeSelectionSync(), group.nodeSelectionAsync());
            names.add(coroutinesName(group.sync()));
        }

        for (AggregateInterfaces aggregate : AggregateInterfaces.values()) {
            add(names, aggregate.sync(), aggregate.async(), aggregate.reactive());
            names.add(coroutinesName(aggregate.sync()));
        }

        // the node-selection umbrella interfaces, verified by AggregateInterfaceConsistencyUnitTests
        add(names, NodeSelectionCommands.class, NodeSelectionAsyncCommands.class);

        return names;
    }

    /**
     * The coroutine interface name a sync interface maps to, by the same convention the Kotlin side resolves reflectively
     * ({@code AggregateInterfaces.coroutines()}/{@code CommandInterfaces.coroutines()} in {@code KnownKotlinApiDeviations.kt}).
     * Derived as a plain string so that these Java test sources hold no reference to the Kotlin flavor; whether the interface
     * actually exists is the Kotlin side's business, this check only needs to know which names are accounted for.
     */
    private static String coroutinesName(Class<?> sync) {

        String simpleName = sync.getSimpleName();
        return simpleName.substring(0, simpleName.length() - "Commands".length()) + "CoroutinesCommands";
    }

    private static void add(Set<String> names, Class<?>... types) {
        for (Class<?> type : types) {
            if (type != null) {
                names.add(type.getSimpleName());
            }
        }
    }

}
