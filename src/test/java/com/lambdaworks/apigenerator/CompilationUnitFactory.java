package com.lambdaworks.apigenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author Mark Paluch
 */
public class CompilationUnitFactory {

    private File templateFile;
    private File sources;
    private File target;
    private String targetPackage;
    private String targetName;

    private Function<String, String> typeDocFunction;
    private Function<MethodDeclaration, Type> methodReturnTypeFunction;
    private Predicate<MethodDeclaration> methodFilter;
    private Supplier<List<String>> importSupplier;
    private Consumer<ClassOrInterfaceDeclaration> typeMutator;
    private Function<Comment, Comment> methodCommentMutator;

    CompilationUnit template;
    CompilationUnit result = new CompilationUnit();
    ClassOrInterfaceDeclaration resultType;

    public CompilationUnitFactory(File templateFile, File sources, String targetPackage, String targetName,
                                  Function<String, String> typeDocFunction,
                                  Function<MethodDeclaration, Type> methodReturnTypeFunction,
                                  Predicate<MethodDeclaration> methodFilter, Supplier<List<String>> importSupplier,
                                  Consumer<ClassOrInterfaceDeclaration> typeMutator,
                                  Function<Comment, Comment> methodCommentMutator) {

        this.templateFile = templateFile;
        this.sources = sources;
        this.targetPackage = targetPackage;
        this.targetName = targetName;
        this.typeDocFunction = typeDocFunction;
        this.methodReturnTypeFunction = methodReturnTypeFunction;
        this.methodFilter = methodFilter;
        this.importSupplier = importSupplier;
        this.typeMutator = typeMutator;
        this.methodCommentMutator = methodCommentMutator;

        this.target = new File(sources, targetPackage.replace('.', '/') + "/" + targetName + ".java");
    }

    public void createInterface() throws Exception {

        result.setPackage(new PackageDeclaration(ASTHelper.createNameExpr(targetPackage)));

        template = JavaParser.parse(templateFile);

        ClassOrInterfaceDeclaration templateTypeDeclaration = (ClassOrInterfaceDeclaration) template.getTypes().get(0);
        resultType = new ClassOrInterfaceDeclaration(ModifierSet.PUBLIC, true, targetName);
        if (templateTypeDeclaration.getExtends() != null) {
            resultType.setExtends(templateTypeDeclaration.getExtends());
        }

        if (!templateTypeDeclaration.getTypeParameters().isEmpty()) {
            resultType.setTypeParameters(new ArrayList<TypeParameter>());
            for (TypeParameter typeParameter : templateTypeDeclaration.getTypeParameters()) {
                resultType.getTypeParameters().add(new TypeParameter(typeParameter.getName(), typeParameter.getTypeBound()));
            }
        }

        resultType.setComment(new JavadocComment(typeDocFunction.apply(templateTypeDeclaration.getComment().getContent())));

        result.setImports(new ArrayList<>());
        ASTHelper.addTypeDeclaration(result, resultType);
        resultType.setParentNode(result);

        if (template.getImports() != null) {
            result.getImports().addAll(template.getImports());
        }
        List<String> importLines = importSupplier.get();
        for (String importLine : importLines) {
            result.getImports().add(new ImportDeclaration(new NameExpr(importLine), false, false));
        }

        new MethodVisitor().visit(template, null);

        if (typeMutator != null) {
            typeMutator.accept(resultType);
        }

        writeResult();

    }

    protected void writeResult() throws IOException {
        FileOutputStream fos = new FileOutputStream(target);
        fos.write(result.toString().getBytes());
        fos.close();
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private class MethodVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(MethodDeclaration n, Object arg) {

            if (!methodFilter.test(n)) {
                return;
            }

            MethodDeclaration method = new MethodDeclaration(n.getModifiers(), methodReturnTypeFunction.apply(n), n.getName());

            if(methodCommentMutator != null){
                method.setComment(methodCommentMutator.apply(n.getComment()));
            }else {
                method.setComment(n.getComment());
            }

            for (Parameter parameter : n.getParameters()) {
                Parameter param = ASTHelper.createParameter(parameter.getType(), parameter.getId().getName());
                param.setVarArgs(parameter.isVarArgs());

                ASTHelper.addParameter(method, param);
            }

            if (n.getTypeParameters() != null) {
                method.setTypeParameters(new ArrayList<>());
                method.getTypeParameters().addAll(n.getTypeParameters());
            }

            ASTHelper.addMember(resultType, method);

        }
    }

}
