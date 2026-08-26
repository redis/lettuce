/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Locates the command interface sources on disk. Two checks in this suite reason about the source rather than the compiled
 * classes: {@link CatalogCompletenessUnitTests} because the Kotlin interfaces are easiest to enumerate by file name, and
 * {@link JavadocConsistencyUnitTests} because Javadoc is not retained in bytecode.
 */
final class CommandInterfaceSources {

    static final File JAVA_ROOT = new File("src/main/java/io/lettuce/core");

    static final File KOTLIN_ROOT = new File("src/main/kotlin/io/lettuce/core");

    static final File TEMPLATE_ROOT = new File("src/main/templates/io/lettuce/core/api");

    /**
     * The packages holding Java command interfaces, relative to {@link #JAVA_ROOT}.
     */
    static final String[] JAVA_PACKAGES = { "api/sync", "api/async", "api/reactive", "cluster/api/sync", "cluster/api/async",
            "cluster/api/reactive", "sentinel/api/sync", "sentinel/api/async", "sentinel/api/reactive", "pubsub/api/sync",
            "pubsub/api/async", "pubsub/api/reactive" };

    /**
     * The packages holding Kotlin coroutine command interfaces, relative to {@link #KOTLIN_ROOT}.
     */
    static final String[] KOTLIN_PACKAGES = { "api/coroutines", "cluster/api/coroutines", "sentinel/api/coroutines" };

    private CommandInterfaceSources() {
    }

    /**
     * The Java source file declaring {@code type}. Kotlin interfaces have no Java source and must not be passed in.
     */
    static File sourceOf(Class<?> type) {
        return new File("src/main/java", type.getName().replace('.', '/') + ".java");
    }

    /**
     * Every Java command interface of both catalogs: the per-group sync/async/reactive and node-selection interfaces, the
     * aggregates, and the node-selection umbrellas. The Kotlin coroutine interfaces are excluded — JavaParser cannot read them,
     * so their KDoc is outside the reach of these checks.
     */
    static List<Class<?>> javaInterfaces() {

        List<Class<?>> types = new ArrayList<>();

        for (CommandInterfaces group : CommandInterfaces.values()) {
            add(types, group.sync(), group.async(), group.reactive(), group.nodeSelectionSync(), group.nodeSelectionAsync());
        }
        for (AggregateInterfaces aggregate : AggregateInterfaces.values()) {
            add(types, aggregate.sync(), aggregate.async(), aggregate.reactive());
        }
        add(types, io.lettuce.core.cluster.api.sync.NodeSelectionCommands.class,
                io.lettuce.core.cluster.api.async.NodeSelectionAsyncCommands.class);

        return types;
    }

    private static void add(List<Class<?>> target, Class<?>... types) {
        for (Class<?> type : types) {
            if (type != null && !target.contains(type)) {
                target.add(type);
            }
        }
    }

    /**
     * Every {@code *Commands.java} of the Java command interface packages, plus the templates they are generated from, sorted
     * by path for stable test output.
     */
    static List<File> javaSources() {

        List<File> files = new ArrayList<>();

        for (String pkg : JAVA_PACKAGES) {
            collect(files, new File(JAVA_ROOT, pkg), ".java");
        }
        collect(files, TEMPLATE_ROOT, ".java");

        files.sort(Comparator.comparing(File::getPath));
        return files;
    }

    /**
     * Collect the {@code *Commands<extension>} files of a single directory.
     */
    static void collect(List<File> target, File directory, String extension) {

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        Arrays.stream(files).filter(f -> f.getName().endsWith("Commands" + extension)).forEach(target::add);
    }

}
