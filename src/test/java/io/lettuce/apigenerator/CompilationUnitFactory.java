/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.printer.configuration.imports.EclipseImportOrderingStrategy;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;

/**
 * @author Mark Paluch
 * @author Mikhael Sokolov
 */
class CompilationUnitFactory {

    private final File templateFile;

    private final File sources;

    private final File target;

    private final String targetPackage;

    private final String targetName;

    private final Function<String, String> typeDocFunction;

    private final Map<Predicate<MethodDeclaration>, Function<MethodDeclaration, Type>> methodReturnTypeMutation;

    private final Predicate<MethodDeclaration> methodFilter;

    private final Supplier<List<String>> importSupplier;

    private final Consumer<ClassOrInterfaceDeclaration> typeMutator;

    private final Consumer<MethodDeclaration> onMethod;

    private final BiFunction<MethodDeclaration, Comment, Comment> methodCommentMutator;

    private CompilationUnit template;

    private final CompilationUnit result = new CompilationUnit();

    private ClassOrInterfaceDeclaration resultType;

    public CompilationUnitFactory(File templateFile, File sources, String targetPackage, String targetName,
            Function<String, String> typeDocFunction, Function<MethodDeclaration, Type> methodReturnTypeFunction,
            Predicate<MethodDeclaration> methodFilter, Supplier<List<String>> importSupplier,
            Consumer<ClassOrInterfaceDeclaration> typeMutator, Function<Comment, Comment> methodCommentMutator) {
        this(templateFile, sources, targetPackage, targetName, typeDocFunction, methodReturnTypeFunction, methodDeclaration -> {
        }, methodFilter, importSupplier, typeMutator,
                (m, c) -> methodCommentMutator != null ? methodCommentMutator.apply(c) : c);

    }

    public CompilationUnitFactory(File templateFile, File sources, String targetPackage, String targetName,
            Function<String, String> typeDocFunction, Function<MethodDeclaration, Type> methodReturnTypeFunction,
            Consumer<MethodDeclaration> onMethod, Predicate<MethodDeclaration> methodFilter,
            Supplier<List<String>> importSupplier, Consumer<ClassOrInterfaceDeclaration> typeMutator,
            BiFunction<MethodDeclaration, Comment, Comment> methodCommentMutator) {

        this.templateFile = templateFile;
        this.sources = sources;
        this.targetPackage = targetPackage;
        this.targetName = targetName;
        this.typeDocFunction = typeDocFunction;
        this.onMethod = onMethod;
        this.methodFilter = methodFilter;
        this.importSupplier = importSupplier;
        this.typeMutator = typeMutator;
        this.methodCommentMutator = methodCommentMutator;
        this.methodReturnTypeMutation = new LinkedHashMap<>();

        this.methodReturnTypeMutation.put(it -> true, methodReturnTypeFunction);

        this.target = new File(sources, targetPackage.replace('.', '/') + "/" + targetName + ".java");
    }

    public void createInterface() throws Exception {

        result.setPackageDeclaration(new PackageDeclaration(new Name(targetPackage)));

        template = StaticJavaParser.parse(templateFile);

        ClassOrInterfaceDeclaration templateTypeDeclaration = (ClassOrInterfaceDeclaration) template.getTypes().get(0);
        resultType = new ClassOrInterfaceDeclaration(new NodeList<>(new Modifier(Modifier.Keyword.PUBLIC)), true, targetName);
        if (templateTypeDeclaration.getExtendedTypes() != null) {
            resultType.setExtendedTypes(templateTypeDeclaration.getExtendedTypes());
        }

        if (!templateTypeDeclaration.getTypeParameters().isEmpty()) {
            resultType.setTypeParameters(new NodeList<>());
            for (TypeParameter typeParameter : templateTypeDeclaration.getTypeParameters()) {
                resultType.getTypeParameters()
                        .add(new TypeParameter(typeParameter.getName().getIdentifier(), typeParameter.getTypeBound()));
            }
        }

        resultType
                .setComment(new JavadocComment(typeDocFunction.apply(templateTypeDeclaration.getComment().get().getContent())));
        result.setComment(template.getComment().orElse(null));

        result.setImports(new NodeList<>());

        result.addType(resultType);
        resultType.setParentNode(result);

        if (template.getImports() != null) {
            result.getImports().addAll(template.getImports());
        }
        List<String> importLines = importSupplier.get();
        importLines.forEach(importLine -> result.getImports().add(new ImportDeclaration(importLine, false, false)));

        new MethodVisitor().visit(template, null);

        if (typeMutator != null) {
            typeMutator.accept(resultType);
        }

        removeUnusedImports();

        writeResult();

    }

    public void keepMethodSignaturesFor(Set<String> methodSignaturesToKeep) {

        this.methodReturnTypeMutation.put(methodDeclaration -> contains(methodSignaturesToKeep, methodDeclaration),
                MethodDeclaration::getType);
    }

    private void writeResult() throws IOException {
        // Configure PrettyPrinter with IntelliJ import ordering strategy
        DefaultPrinterConfiguration config = new DefaultPrinterConfiguration();
        EclipseImportOrderingStrategy strategy = new EclipseImportOrderingStrategy();
        strategy.setSortImportsAlphabetically(true);
        DefaultConfigurationOption option = new DefaultConfigurationOption(
                DefaultPrinterConfiguration.ConfigOption.SORT_IMPORTS_STRATEGY, strategy);
        config.addOption(option);

        DefaultPrettyPrinter printer = new DefaultPrettyPrinter(config);
        String formattedCode = printer.print(result);

        FileOutputStream fos = new FileOutputStream(target);
        fos.write(formattedCode.getBytes());
        fos.close();
    }

    public static Type createParametrizedType(String baseType, String... typeArguments) {

        NodeList<Type> args = new NodeList<>();

        Arrays.stream(typeArguments).map(it -> {

            if (it.contains("[]")) {
                return it;
            }

            return StringUtils.capitalize(it);
        }).map(it -> new ClassOrInterfaceType(null, it)).forEach(args::add);

        return new ClassOrInterfaceType(null, new SimpleName(baseType), args);
    }

    public static boolean contains(Collection<String> haystack, MethodDeclaration needle) {

        ClassOrInterfaceDeclaration declaringClass = (ClassOrInterfaceDeclaration) needle.getParentNode().get();

        return haystack.contains(needle.getNameAsString())
                || haystack.contains(declaringClass.getNameAsString() + "." + needle.getNameAsString());
    }

    public void removeUnusedImports() {
        ClassOrInterfaceDeclaration declaringClass = (ClassOrInterfaceDeclaration) result.getChildNodes().get(1);

        List<ImportDeclaration> optimizedImports = result.getImports().stream()
                .filter(i -> i.isAsterisk() || i.isStatic() || isImportUsed(declaringClass, i)).collect(Collectors.toList());

        result.setImports(NodeList.nodeList(optimizedImports));
    }

    private boolean isImportUsed(ClassOrInterfaceDeclaration declaringClass, ImportDeclaration importDeclaration) {
        String importIdentifier = importDeclaration.getName().getIdentifier();

        // Check if import is used in types
        boolean usedInTypes = declaringClass.findFirst(Type.class, t -> {
            String fullType = t.toString();
            // Match patterns like: Flux<X<Long>>, X, X<Long> where X is importIdentifier
            String regex = "\\b" + Pattern.quote(importIdentifier) + "(?:<[^>]*>)?\\b";
            return Pattern.compile(regex).matcher(fullType).find();
        }).isPresent();

        // Check if import is used in annotations
        boolean usedInAnnotations = declaringClass.findFirst(AnnotationExpr.class, a -> {
            String annotationName = a.getNameAsString();
            return annotationName.equals(importIdentifier);
        }).isPresent();

        return usedInTypes || usedInAnnotations;
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private class MethodVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(MethodDeclaration parsedDeclaration, Object arg) {

            if (!methodFilter.test(parsedDeclaration)) {
                return;
            }

            Type returnType = getMethodReturnType(parsedDeclaration);

            MethodDeclaration method = new MethodDeclaration(parsedDeclaration.getModifiers(),
                    parsedDeclaration.getAnnotations(), parsedDeclaration.getTypeParameters(), returnType,
                    parsedDeclaration.getName(), parsedDeclaration.getParameters(), parsedDeclaration.getThrownExceptions(),
                    null);

            if (methodCommentMutator != null) {
                method.setComment(methodCommentMutator.apply(method, parsedDeclaration.getComment().orElse(null)));
            } else {
                method.setComment(parsedDeclaration.getComment().orElse(null));
            }

            onMethod.accept(method);

            resultType.addMember(method);
        }

        private Type getMethodReturnType(MethodDeclaration parsedDeclaration) {

            List<Map.Entry<Predicate<MethodDeclaration>, Function<MethodDeclaration, Type>>> entries = new ArrayList<>(
                    methodReturnTypeMutation.entrySet());

            Collections.reverse(entries);

            return entries.stream().filter(entry -> entry.getKey().test(parsedDeclaration)).findFirst()
                    .map(entry -> entry.getValue().apply(parsedDeclaration)).orElse(null);
        }

    }

}
