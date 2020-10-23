/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.apigenerator;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;

import io.lettuce.core.internal.LettuceSets;

/**
 * Create Kotlin Coroutine API based on the templates.
 *
 * @author Mikhael Sokolov
 * @author dengliming
 * @author Mark Paluch
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
class KotlinCompilationUnitFactory {

    private static final Set<String> SKIP_IMPORTS = LettuceSets.unmodifiableSet("java.util.List", "java.util.Set", "java.util.Map");
    private static final Set<String> FLOW_METHODS = LettuceSets.unmodifiableSet("keys", "geohash", "georadius", "georadiusbymember", "hgetall", "hmget", "hkeys", "hvals", "sort", "zpopmin", "zpopmax", "zrange", "zrangebylex", "zrangebyscore", "zrangeWithScores", "zrangebyscoreWithScores", "zunion", "zunionWithScores","zinter", "zinterWithScores", "zrevrange", "zrevrangeWithScores", "zrevrangebylex", "zrevrangebyscore", "zrevrangebyscore", "zrevrangebyscoreWithScores", "mget", "sdiff", "sinter", "smembers", "srandmember", "sunion", "xclaim", "xpending", "xrange", "xread", "xreadgroup", "xrevrange", "smismember");
    private static final Set<String> NON_SUSPENDABLE_METHODS = LettuceSets.unmodifiableSet("isOpen", "flushCommands", "setAutoFlushCommands");

    private static final Set<String> NON_NULLABLE_RESULT_METHODS = LettuceSets.unmodifiableSet("discard", "multi", "exec",
            "watch", "unwatch", "getMasterAddrByName", "master", "reset", "failover", "monitor",
            "RedisSentinelCoroutinesCommands.set", "remove", "RedisSentinelCoroutinesCommands.clientSetname",
            "RedisSentinelCoroutinesCommands.clientKill", "RedisSentinelCoroutinesCommands.clientPause",
            "RedisSentinelCoroutinesCommands.clientList", "RedisSentinelCoroutinesCommands.info",
            "RedisSentinelCoroutinesCommands.ping", "pubsubNumsub", "pubsubNumpat", "echo", "ping", "readOnly", "readWrite");
    private static final Set<String> SKIP_METHODS = LettuceSets.unmodifiableSet("BaseRedisCommands.reset", "getStatefulConnection");
    private static final Map<String, String> RESULT_SPEC;

    static {
        Map<String, String> resultSpec = new HashMap<>();
        resultSpec.put("hgetall", "Flow<KeyValue<K,V>>");
        RESULT_SPEC = resultSpec;
    }

    private static final String FORMATTING_INDENT = "    ";

    private final File templateFile;
    private final File target;
    private final String targetPackage;
    private final String targetName;

    private final Supplier<List<String>> importSupplier;
    private final Function<String, String> commentInjector;

    private final StringBuilder result = new StringBuilder();

    public KotlinCompilationUnitFactory(File templateFile,
                                        File sources,
                                        String targetPackage,
                                        String targetName,
                                        Supplier<List<String>> importSupplier,
                                        Function<String, String> commentInjector) {
        this.templateFile = templateFile;
        this.targetPackage = targetPackage;
        this.targetName = targetName;
        this.importSupplier = importSupplier;
        this.commentInjector = commentInjector;

        this.target = new File(sources, targetPackage.replace('.', '/') + "/" + targetName + ".kt");
    }

    public void create() throws Exception {
        CompilationUnit template = JavaParser.parse(templateFile);

        JavaToken license = template.getTokenRange().get().getBegin();
        result.append(license.asString().replaceAll("Copyright [\\d]{4}-[\\d]{4}", "Copyright 2020"));
        result.append(license.getNextToken().get().asString());
        result.append("\n");

        result.append("package ").append(targetPackage).append("\n\n");

        importSupplier.get().forEach(l -> result.append("import ").append(l).append("\n"));

        template
                .getImports()
                .stream()
                .filter(i -> SKIP_IMPORTS
                        .stream()
                        .noneMatch(si -> si.equals(i.getNameAsString()))
                )
                .forEach(i -> result
                        .append("import ")
                        .append(i.getNameAsString())
                        .append(i.isAsterisk() ? ".*" : "")
                        .append("\n")
                );

        result.append("\n");

        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) template.getTypes().get(0);

        result.append(commentInjector.apply(extractJavadoc(clazz.getJavadoc().get())
                .replaceAll("@author Mark Paluch", "@author \\${author}")
                .replaceAll("@since [0-9].0", "@since \\${since}")
                .replaceAll("\\*/", "* @generated by \\${generator}\r\n */")
        ));

        result.append("@ExperimentalLettuceCoroutinesApi").append("\n");

        NodeList<TypeParameter> typeParameters = clazz.getTypeParameters();

        result
                .append("interface ")
                .append(targetName)
                .append(extractTypeParams(typeParameters, "Any"))
                .append(" ");


        result.append("{\n\n");
        new MethodVisitor().visit(template, null);
        result.append("}\n\n");

        writeResult();
    }

    private class MethodVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(MethodDeclaration method, Object arg) {

            // Skip deprecated and StreamingChannel methods
            if (method.isAnnotationPresent(Deprecated.class)
                    || contains(SKIP_METHODS, method)
                    || method.getParameters().stream().anyMatch(p -> p.getType().asString().contains("StreamingChannel"))) {
                return;
            }
            result
                    .append(FORMATTING_INDENT)
                    .append(extractJavadoc(method.getJavadoc().get()).replace("\n", "\n" + FORMATTING_INDENT))
                    .append(extractAnnotations(method))
                    .append(contains(NON_SUSPENDABLE_METHODS, method) || isFlowable(method) ? "" : "suspend ")
                    .append("fun ")
                    .append(method.getTypeParameters().isNonEmpty() ? extractTypeParams(method.getTypeParameters(), null).concat(" ") : "")
                    .append(method.getNameAsString())
                    .append("(")
                    .append(extractParameters(method))
                    .append(")")
                    .append(customResultType(method).orElse(toKotlinType(method.getType(), isFlowable(method), contains(NON_NULLABLE_RESULT_METHODS, method))))
                    .append("\n\n");
        }

        private String extractAnnotations(MethodDeclaration method) {
            return method
                    .getAnnotations()
                    .stream()
                    .map(a -> a.getNameAsString() + "\n" + FORMATTING_INDENT)
                    .collect(joining());
        }

        private String extractParameters(MethodDeclaration method) {
            return method
                    .getParameters()
                    .stream()
                    .map(p -> (p.isVarArgs() ? "vararg " : "") + p.getName() + toKotlinType(p.getType(), false, true))
                    .collect(joining(", "));
        }

        private Optional<String> customResultType(MethodDeclaration method) {
            ClassOrInterfaceDeclaration declaringClass = (ClassOrInterfaceDeclaration) method.getParentNode().get();
            return RESULT_SPEC
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().equals(method.getNameAsString()) || e.getKey().contains(declaringClass.getNameAsString() + "." + method.getNameAsString()))
                    .findFirst()
                    .map(e -> ": " + e.getValue());
        }

        private boolean contains(Collection<String> haystack, MethodDeclaration method) {
            ClassOrInterfaceDeclaration declaringClass = (ClassOrInterfaceDeclaration) method.getParentNode().get();
            return haystack.contains(method.getNameAsString()) || haystack.contains(declaringClass.getNameAsString() + "." + method.getNameAsString());
        }

        private boolean isCollection(Type type) {
            return type.asString().startsWith("List<") || type.asString().startsWith("Set<");
        }

        private boolean isFlowable(MethodDeclaration method) {
            return contains(FLOW_METHODS, method) && (isCollection(method.getType()) || method.getType().asString().startsWith("Map<"));
        }

        private String toKotlinType(Type type, boolean isFlowable, boolean isForceNonNullable) {
            String fixedType;

            if (type.isTypeParameter() || type.asString().equals("K") || type.asString().equals("V")) {
                fixedType = type.asString();
            } else if (type.isArrayType()) {
                Type componentType = type.asArrayType().getComponentType();
                if (componentType.asString().equals("byte")) {
                    fixedType = "ByteArray";
                } else {
                    fixedType = String.format("Array<%s>", componentType.asString());
                }
            } else if (type.isPrimitiveType()) {
                fixedType = type
                        .asPrimitiveType()
                        .toBoxedType()
                        .getName()
                        .asString()
                        .replace("Integer", "Int")
                        .replace("Object", "Any");
            } else if (isFlowable) {
                fixedType = type
                        .asString()
                        .replace("List", "Flow")
                        .replace("Set", "Flow")
                        .replace("Map", "Flow");
            } else {
                fixedType = type
                        .asString()
                        .replace("void", "Unit")
                        .replace("Object", "Any")
                        .replace("? extends", "out")
                        .replace("? super", "in")
                        .replace(",", ", ");
            }

            boolean nullable = !isForceNonNullable && !isFlowable && !type.isPrimitiveType() && !isCollection(type);

            return fixedType.equals("Unit") ? "" : ": " + (nullable ? fixedType + "?" : fixedType);
        }
    }

    public static String extractTypeParams(NodeList<TypeParameter> typeParams, String bounds) {
        if (typeParams.isEmpty()) {
            return "";
        } else {
            return typeParams
                    .stream()
                    .map(tp -> tp.getName().getIdentifier() + (bounds != null ? " : " + bounds : ""))
                    .collect(joining(", ", "<", ">"));
        }
    }

    public static String extractJavadoc(Javadoc javadoc) {
        String plainJavadoc = javadoc
                .toComment()
                .getContent()
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("* \n", "*\n");

        return String.format("/**%s*/\n", convertToKotlinDoc(plainJavadoc));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void writeResult() throws IOException {
        target.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(target);
        fos.write(result.toString().getBytes());
        fos.close();
    }

    public static String convertToKotlinDoc(String javaDoc) {
        String res = javaDoc;

        res = replaceSurrounding(res, "{@code ", "}", "`", "`");
        res = replaceSurrounding(res, "{@link ", "}", "[", "]");

        return res
                .replace("java.lang.Object", "Any")
                .replace("Object", "Any")
                .replaceAll("\\bdouble\\b", "Double")
                .replaceAll("\\bint\\b", "Integer");
    }

    public static String replaceSurrounding(String original, String prefix, String suffix, String replacePrefix, String replaceSuffix) {
        Matcher matcher = Pattern.compile(Pattern.quote(prefix) + "[a-zA-Z0-9.,#\\-~()*\\s]+" + Pattern.quote(suffix)).matcher(original);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String substr = StringUtils.substringBetween(matcher.group(), prefix, suffix);

            String replacement = (replacePrefix + substr + replaceSuffix)
                    .replace("()", "")
                    .replace(replacePrefix + "#", replacePrefix);

            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
