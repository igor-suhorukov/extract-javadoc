package com.github.igorsuhorukov.javadoc;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Parser {

    private static final String[] SOURCE_PATH = new String[]{System.getProperty("java.io.tmpdir")};
    private static final String[] SOURCE_ENCODING = new String[]{"UTF-8"};

    public static void main(String[] args) throws Exception {
        Path path = Paths.get(args[0]);
        Stream<Path> walk = Files.walk(path, FileVisitOption.FOLLOW_LINKS);
        walk.filter(file -> !Files.isDirectory(file) && file.getFileName().toString().endsWith(".java")).forEach(Parser::parseFile);
    }

    private static void parseFile(Path source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        try {
            parser.setSource(new String(Files.readAllBytes(source)).toCharArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        parser.setResolveBindings(false);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setCompilerOptions(JavaCore.getOptions());

        parser.setUnitName(source.getFileName().toFile().getName());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new JavadocVisitor(cu));
    }
}

