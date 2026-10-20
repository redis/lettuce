/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.async.RedisTimeSeriesAsyncCommands;
import io.lettuce.core.api.reactive.RedisTimeSeriesReactiveCommands;
import io.lettuce.core.api.sync.RedisTimeSeriesCommands;
import io.lettuce.core.cluster.api.async.NodeSelectionTimeSeriesAsyncCommands;
import io.lettuce.core.cluster.api.sync.NodeSelectionTimeSeriesCommands;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsRangeArgs;
import io.lettuce.core.timeseries.arguments.TsReadArgs;

/**
 * Cross-cutting checks of the TimeSeries public API: {@code @since 8.0} and no {@code @Experimental} (M02), Reactor stays
 * optional (M03), package-private argument bases (M04).
 */
@Tag(UNIT_TEST)
class TsApiConventionsUnitTests {

    private static final Path MAIN_JAVA = Paths.get("src", "main", "java");

    private static final Path MAIN_KOTLIN = Paths.get("src", "main", "kotlin");

    private static final Pattern SINCE = Pattern.compile("@since\\s+(\\S+)");

    private static final Pattern EXPERIMENTAL = Pattern.compile("@Experimental\\b");

    private static final Pattern PUBLIC_TYPE = Pattern
            .compile("^public\\s+(?:final\\s+|abstract\\s+)*(?:class|interface|enum)\\s+(\\w+)", Pattern.MULTILINE);

    private static final List<Class<?>> COMMAND_INTERFACES = Arrays.asList(RedisTimeSeriesCommands.class,
            RedisTimeSeriesAsyncCommands.class, RedisTimeSeriesReactiveCommands.class, NodeSelectionTimeSeriesCommands.class,
            NodeSelectionTimeSeriesAsyncCommands.class);

    private static List<Path> timeSeriesSources() throws IOException {

        List<Path> sources = new ArrayList<>();
        for (String pkg : new String[] { "io/lettuce/core/timeseries", "io/lettuce/core/timeseries/arguments" }) {
            try (Stream<Path> files = Files.list(MAIN_JAVA.resolve(pkg))) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(sources::add);
            }
        }
        for (Class<?> type : COMMAND_INTERFACES) {
            sources.add(MAIN_JAVA.resolve(type.getName().replace('.', '/') + ".java"));
        }
        sources.add(MAIN_KOTLIN.resolve("io/lettuce/core/api/coroutines/RedisTimeSeriesCoroutinesCommands.kt"));
        sources.add(MAIN_KOTLIN.resolve("io/lettuce/core/api/coroutines/RedisTimeSeriesCoroutinesCommandsImpl.kt"));
        return sources;
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Return the Javadoc block that ends right before {@code position}, ignoring annotations, or {@code ""}.
     */
    private static String javadocBefore(String source, int position) {

        String before = source.substring(0, position).replaceAll("(?m)^\\s*@\\w+(\\([^)]*\\))?\\s*$", "").trim();
        if (!before.endsWith("*/")) {
            return "";
        }
        int start = before.lastIndexOf("/**");
        return start < 0 ? "" : before.substring(start);
    }

    /**
     * Scenario M02: every {@code @since} tag in the TimeSeries sources is {@code 8.0}.
     */
    @Test
    void everySinceTagIs80() throws IOException {

        for (Path source : timeSeriesSources()) {
            Matcher matcher = SINCE.matcher(read(source));
            while (matcher.find()) {
                assertThat(matcher.group(1)).as("@since in %s", source).isEqualTo("8.0");
            }
        }
    }

    /**
     * Scenario M02: no TimeSeries type or method carries {@code @Experimental}.
     */
    @Test
    void noExperimental() throws IOException {

        for (Path source : timeSeriesSources()) {
            assertThat(EXPERIMENTAL.matcher(read(source)).find()).as("@Experimental in %s", source).isFalse();
        }
    }

    /**
     * Scenario M02: no TimeSeries type or method carries the {@code @Experimental} annotation at runtime.
     */
    @Test
    void noExperimentalAnnotationAtRuntime() throws Exception {

        List<Class<?>> types = new ArrayList<>(COMMAND_INTERFACES);
        types.addAll(Arrays.asList(TsAggregation.class, TsAggregationType.class, TsDuplicatePolicy.class,
                TsDuplicatePolicyType.class, TsEncoding.class, TsEncodingType.class, TsInfoValue.class, TsInfoValue.Rule.class,
                TsInfoValue.Chunk.class, TsInfoValueParser.class, TsCreateArgs.class, TsAlterArgs.class,
                TsCreateArgs.Builder.class, TsAlterArgs.Builder.class));

        for (Class<?> type : types) {
            assertThat(Arrays.stream(type.getAnnotations()).map(a -> a.annotationType().getSimpleName()))
                    .as("annotations of %s", type).doesNotContain("Experimental");
            for (Method method : type.getDeclaredMethods()) {
                assertThat(Arrays.stream(method.getAnnotations()).map(a -> a.annotationType().getSimpleName()))
                        .as("annotations of %s", method).doesNotContain("Experimental");
            }
        }
    }

    /**
     * Scenario M02: every public top-level and nested type in the TimeSeries sources has {@code @since 8.0} in its Javadoc.
     */
    @Test
    void everyPublicTypeHasSince80() throws IOException {

        Pattern nested = Pattern.compile(
                "^\\s+public\\s+(?:static\\s+|final\\s+|abstract\\s+)*(?:class|interface|enum)\\s+(\\w+)", Pattern.MULTILINE);

        for (Path source : timeSeriesSources()) {
            if (!source.toString().endsWith(".java")) {
                continue;
            }
            String text = read(source);
            for (Pattern pattern : new Pattern[] { PUBLIC_TYPE, nested }) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    assertThat(javadocBefore(text, matcher.start())).as("Javadoc of %s in %s", matcher.group(1), source)
                            .contains("@since 8.0");
                }
            }
        }
    }

    /**
     * Scenario M02: every method of the TimeSeries command interfaces has {@code @since 8.0}.
     */
    @Test
    void everyCommandMethodHasSince80() throws IOException {

        for (Class<?> type : COMMAND_INTERFACES) {

            String text = read(MAIN_JAVA.resolve(type.getName().replace('.', '/') + ".java"));
            for (Method method : type.getDeclaredMethods()) {
                Matcher matcher = Pattern.compile("\\s" + method.getName() + "\\(").matcher(text);
                int declarations = 0;
                while (matcher.find()) {
                    int lineStart = text.lastIndexOf('\n', matcher.start()) + 1;
                    String line = text.substring(lineStart, matcher.end()).trim();
                    if (line.startsWith("*") || line.startsWith("/") || line.startsWith("return")) {
                        continue;
                    }
                    declarations++;
                    assertThat(javadocBefore(text, lineStart)).as("Javadoc of %s", method).contains("@since 8.0");
                }
                assertThat(declarations).as("declarations of %s", method).isGreaterThan(0);
            }
        }
    }

    /**
     * Scenario M02: every public setter and builder factory of the argument types has {@code @since 8.0} (design §3.2).
     */
    @Test
    void everyArgumentSetterHasSince80() throws IOException {

        Pattern publicMethod = Pattern.compile("^\\s+public\\s+(?!class|static class|interface|enum)[^=;]*\\(",
                Pattern.MULTILINE);

        for (String name : new String[] { "TsCreateArgs", "TsAlterArgs", "BaseTsSeriesArgs", "BaseTsCreateArgs", "TsAddArgs",
                "TsIncrByArgs", "BaseTsRangeArgs", "TsRangeArgs", "TsReadArgs" }) {

            Path source = MAIN_JAVA.resolve("io/lettuce/core/timeseries/arguments/" + name + ".java");
            String text = read(source);
            Matcher matcher = publicMethod.matcher(text);
            while (matcher.find()) {
                String before = text.substring(0, matcher.start());
                if (before.trim().endsWith("@Override")) {
                    continue;
                }
                assertThat(javadocBefore(text, matcher.start())).as("Javadoc before '%s' in %s", matcher.group().trim(), name)
                        .contains("@since 8.0");
            }
        }
    }

    /**
     * Scenario M03: the non-reactive TimeSeries classes do not reference any {@code reactor.*} type in their bytecode.
     */
    @Test
    void nonReactiveClassesDoNotReferenceReactor() throws Exception {

        List<Path> classFiles = new ArrayList<>();
        for (Class<?> anchor : new Class<?>[] { TsInfoValue.class, TsCreateArgs.class }) {
            Path dir = classFile(anchor).getParent();
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.toString().endsWith(".class")).forEach(classFiles::add);
            }
        }
        for (Class<?> type : new Class<?>[] { RedisTimeSeriesCommands.class, RedisTimeSeriesAsyncCommands.class,
                NodeSelectionTimeSeriesCommands.class, NodeSelectionTimeSeriesAsyncCommands.class,
                Class.forName("io.lettuce.core.RedisTimeSeriesCommandBuilder") }) {
            classFiles.add(classFile(type));
        }

        assertThat(classFiles).hasSizeGreaterThan(15);
        for (Path classFile : classFiles) {
            assertThat(referencesReactor(classFile)).as("reactor reference in %s", classFile).isFalse();
        }
    }

    /**
     * Scenario M03: sanity check of the scan: the reactive interface does reference Reactor.
     */
    @Test
    void reactiveInterfaceReferencesReactor() throws Exception {
        assertThat(referencesReactor(classFile(RedisTimeSeriesReactiveCommands.class))).isTrue();
    }

    private static Path classFile(Class<?> type) throws URISyntaxException {

        URL url = type.getResource(type.getSimpleName() + ".class");
        assertThat(url).as("class file of %s", type).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("file");
        return Paths.get(url.toURI());
    }

    private static boolean referencesReactor(Path classFile) throws IOException {

        String constants = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
        return constants.contains("reactor/");
    }

    /**
     * Scenario M04: the argument base classes are package-private; the concrete argument classes are public.
     */
    @Test
    void argumentBasesArePackagePrivate() throws ClassNotFoundException {

        Class<?> seriesBase = Class.forName("io.lettuce.core.timeseries.arguments.BaseTsSeriesArgs");
        Class<?> createBase = Class.forName("io.lettuce.core.timeseries.arguments.BaseTsCreateArgs");

        assertThat(Modifier.isPublic(seriesBase.getModifiers())).isFalse();
        assertThat(Modifier.isPublic(createBase.getModifiers())).isFalse();
        assertThat(Modifier.isPublic(TsCreateArgs.class.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(TsAlterArgs.class.getModifiers())).isTrue();
        assertThat(TsCreateArgs.class.getSuperclass()).isEqualTo(createBase);
        assertThat(createBase.getSuperclass()).isEqualTo(seriesBase);
        assertThat(TsAlterArgs.class.getSuperclass()).isEqualTo(seriesBase);
    }

    /**
     * Scenario M04 (range layer): {@code BaseTsRangeArgs} is package-private; {@code TsRangeArgs} and {@code TsReadArgs} are
     * public, and {@code TsRangeArgs} extends the range base.
     */
    @Test
    void rangeArgumentBaseIsPackagePrivate() throws ClassNotFoundException {

        Class<?> rangeBase = Class.forName("io.lettuce.core.timeseries.arguments.BaseTsRangeArgs");

        assertThat(Modifier.isPublic(rangeBase.getModifiers())).isFalse();
        assertThat(Modifier.isPublic(TsRangeArgs.class.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(TsReadArgs.class.getModifiers())).isTrue();
        assertThat(TsRangeArgs.class.getSuperclass()).isEqualTo(rangeBase);
    }

    /**
     * Scenario M04: the raw value-type implementations are not public; the interfaces and enums are.
     */
    @Test
    void rawValueTypesAreNotPublic() throws ClassNotFoundException {

        for (String raw : new String[] { "RawTsAggregation", "RawTsDuplicatePolicy", "RawTsEncoding" }) {
            Class<?> type = Class.forName("io.lettuce.core.timeseries." + raw);
            assertThat(Modifier.isPublic(type.getModifiers())).as(raw).isFalse();
        }

        List<Class<?>> publicTypes = Arrays.asList(TsAggregation.class, TsAggregationType.class, TsDuplicatePolicy.class,
                TsDuplicatePolicyType.class, TsEncoding.class, TsEncodingType.class, TsInfoValue.class);
        assertThat(publicTypes.stream().filter(t -> !Modifier.isPublic(t.getModifiers())).collect(Collectors.toList()))
                .isEmpty();
    }

}
