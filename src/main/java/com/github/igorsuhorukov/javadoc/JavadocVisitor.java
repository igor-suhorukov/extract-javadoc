package com.github.igorsuhorukov.javadoc;

import org.eclipse.jdt.core.dom.*;

class JavadocVisitor extends ASTVisitor {

    private final CompilationUnit compilationUnit;

    private String packageName;

    public JavadocVisitor(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        packageName = node.getName().getFullyQualifiedName();
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return super.visit(node);
    }
}
