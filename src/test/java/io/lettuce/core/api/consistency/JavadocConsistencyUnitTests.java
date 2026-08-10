/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import static io.lettuce.TestTags.UNIT_TEST;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;

/**
 * Verify the parts of the command interfaces that reflection cannot see: parameter names and Javadoc. The rest of the suite
 * works off the compiled classes, where Javadoc is absent entirely and parameter names are erased (the build does not pass
 * {@code -parameters}) — so this is the one check that reads the sources.
 * <p>
 * What is enforced, and why the strictness differs per flavor:
 * <ul>
 * <li><b>Parameter names</b> must match across every flavor. They are part of the public API: Kotlin callers may use named
 * arguments, so renaming a parameter on one flavor only is a source-compatibility break that nothing else catches.</li>
 * <li><b>Documentation completeness</b> — a Javadoc comment, a {@code @param} per parameter and type parameter, and a
 * {@code @return} exactly when the method returns something — is required on every flavor.</li>
 * <li><b>Documentation text</b> (summary, {@code @param}, {@code @return}) must match the sync flavor for the async and
 * node-selection interfaces, which describe the same command with the same words. It is <em>not</em> compared for the
 * <b>reactive</b> flavor, whose prose deliberately describes reactive semantics ({@code armget} documents
 * {@link io.lettuce.core.Value#empty()} where sync documents {@code null}, {@code ftCursorread} documents "a Mono emitting …"),
 * nor for the <b>aggregates</b>, whose hand-written text names its own flavor ({@code getConnection} on the cluster
 * interfaces).</li>
 * <li><b>{@code @since}</b> must be present on the same methods with the same value across all flavors.</li>
 * <li><b>{@code @Deprecated}</b> and the {@code @deprecated} tag must agree. An annotation without a tag leaves users a warning
 * and no migration hint; a tag without an annotation means the compiler never warns at all, which is how
 * {@code RedisAdvancedClusterReactiveCommands#scan(KeyStreamingChannel)} came to be documented as deprecated while compiling
 * clean.</li>
 * </ul>
 * Not enforced: whether the prose is <em>correct</em>, and the Kotlin coroutine KDoc, which JavaParser cannot read.
 */
@Tag(UNIT_TEST)
class JavadocConsistencyUnitTests {

    private static final Pattern DEPRECATED_TAG = Pattern.compile("(?m)^\\s*\\*?\\s*@deprecated\\b");

    @Test
    void deprecatedAnnotationAndJavadocTagAgree() throws Exception {

        SoftAssertions softly = new SoftAssertions();

        for (File source : CommandInterfaceSources.javaSources()) {

            TreeSet<String> annotationWithoutTag = new TreeSet<>();
            TreeSet<String> tagWithoutAnnotation = new TreeSet<>();

            JavaParser.parse(source).accept(new VoidVisitorAdapter<Void>() {

                @Override
                public void visit(MethodDeclaration method, Void arg) {

                    boolean annotated = method.isAnnotationPresent(Deprecated.class);
                    boolean documented = hasDeprecatedTag(method);

                    if (annotated && !documented) {
                        annotationWithoutTag.add(key(method));
                    } else if (documented && !annotated) {
                        tagWithoutAnnotation.add(key(method));
                    }
                }

            }, null);

            softly.assertThat(annotationWithoutTag)
                    .as("%s: @Deprecated without a @deprecated Javadoc tag (users get a warning but no migration hint)",
                            source.getPath())
                    .isEmpty();
            softly.assertThat(tagWithoutAnnotation)
                    .as("%s: @deprecated Javadoc tag without the @Deprecated annotation (the compiler never warns)",
                            source.getPath())
                    .isEmpty();
        }

        softly.assertAll();
    }

    @Test
    void everyMethodIsFullyDocumented() throws Exception {

        SoftAssertions softly = new SoftAssertions();

        for (Class<?> type : CommandInterfaceSources.javaInterfaces()) {

            File source = CommandInterfaceSources.sourceOf(type);
            TreeSet<String> undocumented = new TreeSet<>();
            TreeSet<String> paramMismatch = new TreeSet<>();
            TreeSet<String> returnMismatch = new TreeSet<>();

            for (MethodDoc doc : parse(source).values()) {

                if (!doc.documented) {
                    undocumented.add(doc.key);
                    continue;
                }
                if (!new HashSet<>(doc.taggedParameters).equals(new HashSet<>(doc.declaredParameters()))) {
                    paramMismatch
                            .add(doc.key + " documents " + doc.taggedParameters + ", declares " + doc.declaredParameters());
                }
                if (!doc.alwaysThrows && doc.voidReturn == (doc.returnTag != null)) {
                    returnMismatch.add(doc.key + (doc.voidReturn ? " returns void but has @return" : " has no @return"));
                }
            }

            softly.assertThat(undocumented).as("%s: methods without Javadoc", source.getPath()).isEmpty();
            softly.assertThat(paramMismatch).as("%s: @param does not cover the declared parameters", source.getPath())
                    .isEmpty();
            softly.assertThat(returnMismatch).as("%s: @return must be present exactly for non-void methods", source.getPath())
                    .isEmpty();
        }

        softly.assertAll();
    }

    @Test
    void parameterNamesAndSinceTagsMatchAcrossFlavors() throws Exception {

        SoftAssertions softly = new SoftAssertions();

        for (FlavorPair pair : FlavorPair.all()) {

            Map<String, MethodDoc> reference = parse(CommandInterfaceSources.sourceOf(pair.reference));
            Map<String, MethodDoc> flavor = parse(CommandInterfaceSources.sourceOf(pair.flavor));

            TreeSet<String> names = new TreeSet<>();
            TreeSet<String> since = new TreeSet<>();

            for (Map.Entry<String, MethodDoc> entry : reference.entrySet()) {

                MethodDoc expected = entry.getValue();
                MethodDoc actual = flavor.get(entry.getKey());
                if (actual == null) {
                    continue; // presence is the other tests' business
                }

                if (!expected.parameterNames.equals(actual.parameterNames)) {
                    names.add(expected.key + ": " + expected.parameterNames + " vs " + actual.parameterNames);
                }
                if (!Objects.equals(expected.sinceTag, actual.sinceTag)) {
                    since.add(expected.key + ": @since " + expected.sinceTag + " vs " + actual.sinceTag);
                }
            }

            softly.assertThat(names).as("%s vs %s: parameter names differ (Kotlin named arguments make these public API)",
                    pair.reference.getSimpleName(), pair.flavor.getSimpleName()).isEmpty();
            softly.assertThat(since).as("%s vs %s: @since differs", pair.reference.getSimpleName(), pair.flavor.getSimpleName())
                    .isEmpty();
        }

        softly.assertAll();
    }

    @Test
    void documentationTextMatchesSyncFlavorWhereItIsMechanical() throws Exception {

        SoftAssertions softly = new SoftAssertions();

        for (FlavorPair pair : FlavorPair.all()) {

            if (!pair.compareText) {
                continue;
            }

            Map<String, MethodDoc> reference = parse(CommandInterfaceSources.sourceOf(pair.reference));
            Map<String, MethodDoc> flavor = parse(CommandInterfaceSources.sourceOf(pair.flavor));

            TreeSet<String> drift = new TreeSet<>();

            for (Map.Entry<String, MethodDoc> entry : reference.entrySet()) {

                MethodDoc expected = entry.getValue();
                MethodDoc actual = flavor.get(entry.getKey());
                if (actual == null) {
                    continue;
                }

                if (!Objects.equals(expected.summary, actual.summary)) {
                    drift.add(expected.key + " summary: '" + expected.summary + "' vs '" + actual.summary + "'");
                }
                for (Map.Entry<String, String> tag : expected.parameterText.entrySet()) {
                    String other = actual.parameterText.get(tag.getKey());
                    if (other != null && !other.equals(tag.getValue())) {
                        drift.add(expected.key + " @param " + tag.getKey() + ": '" + tag.getValue() + "' vs '" + other + "'");
                    }
                }
                if (expected.returnTag != null && actual.returnTag != null && !expected.returnTag.equals(actual.returnTag)) {
                    drift.add(expected.key + " @return: '" + expected.returnTag + "' vs '" + actual.returnTag + "'");
                }
            }

            softly.assertThat(drift)
                    .as("%s vs %s: documentation text drifted", pair.reference.getSimpleName(), pair.flavor.getSimpleName())
                    .isEmpty();
        }

        softly.assertAll();
    }

    /**
     * A sync interface paired with one of its other flavors. {@code compareText} marks the pairs whose prose is expected to be
     * word-for-word identical — see the class comment for why reactive and the aggregates are excluded.
     */
    private static final class FlavorPair {

        private final Class<?> reference;

        private final Class<?> flavor;

        private final boolean compareText;

        private FlavorPair(Class<?> reference, Class<?> flavor, boolean compareText) {
            this.reference = reference;
            this.flavor = flavor;
            this.compareText = compareText;
        }

        static List<FlavorPair> all() {

            List<FlavorPair> pairs = new ArrayList<>();

            for (CommandInterfaces group : CommandInterfaces.values()) {
                pairs.add(new FlavorPair(group.sync(), group.async(), true));
                pairs.add(new FlavorPair(group.sync(), group.reactive(), false));
                if (group.hasNodeSelection()) {
                    pairs.add(new FlavorPair(group.sync(), group.nodeSelectionSync(), true));
                    pairs.add(new FlavorPair(group.sync(), group.nodeSelectionAsync(), true));
                }
            }
            for (AggregateInterfaces aggregate : AggregateInterfaces.values()) {
                pairs.add(new FlavorPair(aggregate.sync(), aggregate.async(), false));
                pairs.add(new FlavorPair(aggregate.sync(), aggregate.reactive(), false));
            }

            return pairs;
        }

    }

    /**
     * The documentation of one method, normalized for comparison: whitespace collapsed, tags split out.
     */
    private static final class MethodDoc {

        private String key;

        private final List<String> typeParameters = new ArrayList<>();

        private final List<String> parameterNames = new ArrayList<>();

        private final List<String> taggedParameters = new ArrayList<>();

        private final Map<String, String> parameterText = new LinkedHashMap<>();

        private boolean documented;

        private boolean voidReturn;

        /**
         * Documented as always throwing {@code UnsupportedOperationException} — the cluster interfaces override the
         * node-specific commands with stubs that never return, so they carry {@code @throws} instead of {@code @return}.
         */
        private boolean alwaysThrows;

        private String summary;

        private String returnTag;

        private String sinceTag;

        /**
         * The {@code @param} names a complete Javadoc comment must carry: the type parameters, then the value parameters.
         */
        private List<String> declaredParameters() {

            List<String> declared = new ArrayList<>();
            typeParameters.forEach(name -> declared.add("<" + name + ">"));
            declared.addAll(parameterNames);
            return declared;
        }

    }

    /**
     * Parse a command interface, keyed by method name and source-level parameter types so the same method can be matched across
     * flavors (only the return type changes between them).
     */
    private static Map<String, MethodDoc> parse(File source) throws Exception {

        Map<String, MethodDoc> methods = new LinkedHashMap<>();

        JavaParser.parse(source).accept(new VoidVisitorAdapter<Void>() {

            @Override
            public void visit(MethodDeclaration method, Void arg) {

                MethodDoc doc = new MethodDoc();
                doc.key = key(method);
                doc.voidReturn = method.getType().isVoidType();
                method.getTypeParameters().forEach(it -> doc.typeParameters.add(it.getNameAsString()));
                method.getParameters().forEach(it -> doc.parameterNames.add(it.getNameAsString()));

                Optional<Javadoc> javadoc = method.getJavadoc();
                doc.documented = javadoc.isPresent();

                if (javadoc.isPresent()) {

                    doc.summary = collapse(javadoc.get().getDescription().toText());

                    for (JavadocBlockTag tag : javadoc.get().getBlockTags()) {
                        String content = collapse(tag.getContent().toText());
                        switch (tag.getTagName()) {
                            case "param":
                                String name = tag.getName().orElse("?");
                                doc.taggedParameters.add(name);
                                doc.parameterText.put(name, content);
                                break;
                            case "return":
                                doc.returnTag = content;
                                break;
                            case "since":
                                doc.sinceTag = content;
                                break;
                            case "throws":
                            case "exception":
                                doc.alwaysThrows |= (tag.getName().orElse("") + " " + content).trim()
                                        .startsWith("UnsupportedOperationException");
                                break;
                            default:
                                break;
                        }
                    }
                }

                methods.put(doc.key, doc);
            }

        }, null);

        return methods;
    }

    private static boolean hasDeprecatedTag(MethodDeclaration method) {

        Optional<JavadocComment> javadoc = method.getJavadocComment();
        return javadoc.isPresent() && DEPRECATED_TAG.matcher(javadoc.get().getContent()).find();
    }

    private static String key(MethodDeclaration method) {

        StringBuilder builder = new StringBuilder(method.getNameAsString()).append("(");
        for (int i = 0; i < method.getParameters().size(); i++) {
            Parameter parameter = method.getParameter(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameter.getType().toString().replace(" ", "")).append(parameter.isVarArgs() ? "..." : "");
        }
        return builder.append(")").toString();
    }

    private static String collapse(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

}
