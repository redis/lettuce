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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author Mark Paluch
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
        }, methodFilter, importSupplier, typeMutator, (m, c) -> methodCommentMutator != null ? methodCommentMutator.apply(c) : c);

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

        template = JavaParser.parse(templateFile);

        ClassOrInterfaceDeclaration templateTypeDeclaration = (ClassOrInterfaceDeclaration) template.getTypes().get(0);
        resultType = new ClassOrInterfaceDeclaration(EnumSet.of(Modifier.PUBLIC), true, targetName);
        if (templateTypeDeclaration.getExtendedTypes() != null) {
            resultType.setExtendedTypes(templateTypeDeclaration.getExtendedTypes());
        }

        if (!templateTypeDeclaration.getTypeParameters().isEmpty()) {
            resultType.setTypeParameters(new NodeList<>());
            for (TypeParameter typeParameter : templateTypeDeclaration.getTypeParameters()) {
                resultType.getTypeParameters().add(
                        new TypeParameter(typeParameter.getName().getIdentifier(), typeParameter.getTypeBound()));
            }
        }

        resultType.setComment(new JavadocComment(typeDocFunction.apply(templateTypeDeclaration.getComment().get().getContent())));
        result.setComment(template.getComment().orElse(null));

        result.setImports(new NodeList<>());

        result.addType(resultType);
        resultType.setParentNode(result);

        if (template.getImports() != null) {
            result.getImports().addAll(template.getImports());
        }
        List<String> importLines = importSupplier.get();
        for (String importLine : importLines) {
            result.getImports().add(new ImportDeclaration(importLine, false, false));
        }

        new MethodVisitor().visit(template, null);

        if (typeMutator != null) {
            typeMutator.accept(resultType);
        }

        writeResult();

    }

    public void keepMethodSignaturesFor(Set<String> methodSignaturesToKeep) {

        this.methodReturnTypeMutation.put(methodDeclaration -> contains(methodSignaturesToKeep, methodDeclaration),
                MethodDeclaration::getType);
    }

    private void writeResult() throws IOException {

        FileOutputStream fos = new FileOutputStream(target);
        fos.write(result.toString().getBytes());
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

            for (Map.Entry<Predicate<MethodDeclaration>, Function<MethodDeclaration, Type>> entry : entries) {

                if (entry.getKey().test(parsedDeclaration)) {
                    return entry.getValue().apply(parsedDeclaration);
                }
            }

            return null;
        }
    }
}
