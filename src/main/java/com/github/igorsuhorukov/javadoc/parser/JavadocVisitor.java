package com.github.igorsuhorukov.javadoc.parser;

import com.github.igorsuhorukov.javadoc.model.*;
import com.github.igorsuhorukov.javadoc.model.Type;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavadocVisitor extends ASTVisitor {

    private File file;
    private String relativePath;
    private String sourceText;
    private CompilationUnit compilationUnit;

    private String packageName;
    private List<? extends Comment> commentList;
    private List<JavaDoc> javaDocs = new ArrayList<>();

    public JavadocVisitor(File file, String relativePath, String sourceText) {
        this.file = file;
        this.relativePath = relativePath;
        this.sourceText = sourceText;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        packageName = node.getName().getFullyQualifiedName();
        javaDocs.addAll(getTypes().stream().map(astTypeNode -> {
            JavaDoc javaDoc = getJavaDoc(astTypeNode);
            Type type = getType(astTypeNode);
            type.setUnitInfo(getUnitInfo());
            javaDoc.setSourcePoint(type);
            return javaDoc;
        }).collect(Collectors.toList()));
        javaDocs.addAll(getMethods().stream().map(astMethodNode -> {
            JavaDoc javaDoc = getJavaDoc(astMethodNode);
            Method method = new Method();
            method.setUnitInfo(getUnitInfo());
            method.setName(astMethodNode.getName().getFullyQualifiedName());
            method.setConstructor(astMethodNode.isConstructor());
            fillMethodDeclaration(astMethodNode, method);
            Type type = getType((AbstractTypeDeclaration) astMethodNode.getParent());
            method.setType(type);
            javaDoc.setSourcePoint(method);
            return javaDoc;
        }).collect(Collectors.toList()));
        return super.visit(node);
    }

    private CompilationUnitInfo getUnitInfo() {
        return new CompilationUnitInfo(packageName, relativePath, file.getName());
    }


    @SuppressWarnings("unchecked")
    private void fillMethodDeclaration(MethodDeclaration methodAstNode, Method method) {
        List<SingleVariableDeclaration> parameters = methodAstNode.parameters();
        org.eclipse.jdt.core.dom.Type returnType2 = methodAstNode.getReturnType2();
        method.setParams(parameters.stream().map(param -> param.getType().toString()).collect(Collectors.toList()));
        if(returnType2!=null) {
            method.setReturnType(returnType2.toString());
        }
    }

    private Type getType(AbstractTypeDeclaration astNode) {
        String binaryName = astNode.resolveBinding().getBinaryName();
        Type  type = new Type();
        type.setName(binaryName);
        return type;
    }

    @SuppressWarnings("unchecked")
    private JavaDoc getJavaDoc(BodyDeclaration astNode) {
        JavaDoc javaDoc = new JavaDoc();
        Javadoc javadoc = astNode.getJavadoc();
        List<TagElement> tags = javadoc.tags();
        Optional<TagElement> comment = tags.stream().filter(tag -> tag.getTagName() == null).findFirst();
        comment.ifPresent(tagElement -> javaDoc.setComment(tagElement.toString().replace("\n *","").trim()));
        List<Tag> fragments = tags.stream().filter(tag -> tag.getTagName() != null).map(tag-> {
            Tag tagResult = new Tag();
            tagResult.setName(tag.getTagName());
            tagResult.setFragments(getTags(tag.fragments()));
            return tagResult;
        }).collect(Collectors.toList());
        javaDoc.setTags(fragments);
        return javaDoc;
    }

    @SuppressWarnings("unchecked")
    private List<String> getTags(List fragments){
        return ((List<IDocElement>)fragments).stream().map(Objects::toString).collect(Collectors.toList());
    }
    private List<AbstractTypeDeclaration> getTypes() {
        return commentList.stream().map(ASTNode::getParent).filter(Objects::nonNull).filter(AbstractTypeDeclaration.class::isInstance).map(astNode -> (AbstractTypeDeclaration) astNode).collect(Collectors.toList());
    }

    private List<MethodDeclaration> getMethods() {
        return commentList.stream().map(ASTNode::getParent).filter(Objects::nonNull).filter(MethodDeclaration.class::isInstance).map(astNode -> (MethodDeclaration) astNode).collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(CompilationUnit node) {
        commentList = node.getCommentList();
        this.compilationUnit = node;
        return super.visit(node);
    }

    public List<JavaDoc> getJavaDocs() {
        return javaDocs;
    }
}
