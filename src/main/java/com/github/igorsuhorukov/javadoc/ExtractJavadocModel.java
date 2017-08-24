package com.github.igorsuhorukov.javadoc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.igorsuhorukov.javadoc.model.JavaDoc;
import com.github.igorsuhorukov.javadoc.parser.JavadocVisitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractJavadocModel {

    private static ThreadLocal<ASTParser> parserCache = ThreadLocal.withInitial( () -> ASTParser.newParser(AST.JLS8));
    private static final String[] SOURCE_PATH = new String[]{System.getProperty("java.io.tmpdir")};
    private static final String[] SOURCE_ENCODING = new String[]{"UTF-8"};
    private static final String INVALID_INPUT_PARAMETERS_COUNT = "Invalid input parameters count";
    private ExtractJavadocModel() {
        throw new UnsupportedOperationException("utility class");
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=2){
            System.out.println("Usage: ExtractJavadocModel inputDiectory outputPathForJavaDocFile");
            throw new IllegalArgumentException(INVALID_INPUT_PARAMETERS_COUNT);
        }
        String inputDirectory = args[0];
        String javaDocFile = args[1];
        parseAndSaveJavaDoc(inputDirectory, javaDocFile);
    }

    public static void parseAndSaveJavaDoc(String inputDirectory, String javaDocFile) throws IOException {
        checkParameters(inputDirectory, javaDocFile);
        List<JavaDoc> javadoc = parseDirectory(inputDirectory);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try (Writer javadocWriter = new FileWriter(new File(javaDocFile))){
            objectMapper.writeValue(javadocWriter, javadoc);
        }
    }

    public static List<JavaDoc> parseDirectory(String inputDirectory) throws IOException {
        checkInputDirectory(inputDirectory);
        Path path = Paths.get(inputDirectory);
        List<Path> inputJavaFiles = Files.walk(path, FileVisitOption.FOLLOW_LINKS)
                .filter(file -> !Files.isDirectory(file) && file.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());
        return inputJavaFiles.stream().parallel().map(ExtractJavadocModel::parseFile).flatMap(List::stream).collect(Collectors.toList());
    }

    public static List<JavaDoc> parseFile(Path source) {
        ASTParser parser = parserCache.get();
        String sourceText;
        try {
            sourceText = new String(Files.readAllBytes(source));
            parser.setSource(sourceText.toCharArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        parser.setResolveBindings(true);
        parser.setEnvironment(new String[]{}, SOURCE_PATH, SOURCE_ENCODING, true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setCompilerOptions(JavaCore.getOptions());

        parser.setUnitName(source.getFileName().toFile().getName());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        JavadocVisitor visitor = new JavadocVisitor(source.getFileName().toFile(), sourceText);
        cu.accept(visitor);
        return visitor.getJavaDocs();
    }

    private static void checkParameters(String inputDirectory, String javaDocFile) {
        checkInputDirectory(inputDirectory);
        checkOutputJavadocPath(javaDocFile);
    }

    private static void checkInputDirectory(String inputDirectory) {
        File inDir = new File(inputDirectory);
        if(!inDir.exists() || !inDir.isDirectory()){
            throw new IllegalArgumentException("Input directory should exist");
        }
    }

    private static void checkOutputJavadocPath(String javaDocFile) {
        File javadocDir = new File(javaDocFile);
        if(javadocDir.isDirectory()){
            throw new IllegalArgumentException("Output file path is directory");
        }
        if(!javadocDir.getParentFile().isDirectory() && !javadocDir.getParentFile().exists()){
            throw new IllegalArgumentException("Output file directory should exist");
        }
    }
}

