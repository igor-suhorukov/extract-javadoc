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
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractJavadocModel {

    private static ThreadLocal<ASTParser> parserCache = ThreadLocal.withInitial( () -> ASTParser.newParser(AST.JLS8));
    private static final String[] SOURCE_PATH = new String[]{System.getProperty("java.io.tmpdir")};
    private static final String UTF_8 = "UTF-8";
    static final String XZ_ARCHIVE_EXTENSION = ".xz";
    static final String JAVA_FILE_EXTENSION = ".java";
    static final String JAR_FILE_EXTENSION = ".jar";
    static final String ZIP_FILE_EXTENSION = ".zip";
    private static final int COMPILATION_TIMEOUT = 1;
    private static final int MAX_COMPRESSION_RATIO = 9;
    private static final String[] SOURCE_ENCODING = new String[]{UTF_8};
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
        List<JavaDoc> javadoc = parsePath(inputDirectory);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try (Writer javadocWriter = getWriter(javaDocFile)){
            objectMapper.writeValue(javadocWriter, javadoc);
        }
    }

    private static Writer getWriter(String javaDocFile) throws IOException {
        OutputStream outputStream;
        File javadocFile = new File(javaDocFile);
        if(javaDocFile.toLowerCase().endsWith(XZ_ARCHIVE_EXTENSION)){
            LZMA2Options filterOptions = new LZMA2Options();
            filterOptions.setPreset(MAX_COMPRESSION_RATIO);
            outputStream = new XZOutputStream(new FileOutputStream(javadocFile), filterOptions);
        } else {
            outputStream = new FileOutputStream(javadocFile);
        }
        return new OutputStreamWriter(outputStream, UTF_8);
    }

    public static List<JavaDoc> parsePath(String inputPath) throws IOException {
        checkInputPath(inputPath);
        return isZipFile(inputPath) ? parseSourcesFromZipArchive(inputPath) : parseSourcesFromDirectory(inputPath);
    }

    public static List<JavaDoc> parseFile(String javaSourceText, String fileName, String relativePath) {
        ASTParser parser = parserCache.get();
        parser.setSource(javaSourceText.toCharArray());
        parser.setResolveBindings(true);
        parser.setEnvironment(new String[]{}, SOURCE_PATH, SOURCE_ENCODING, true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setCompilerOptions(JavaCore.getOptions());

        parser.setUnitName(fileName);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        JavadocVisitor visitor = new JavadocVisitor(fileName, relativePath, javaSourceText);
        cu.accept(visitor);
        return visitor.getJavaDocs();
    }

    private static List<JavaDoc> parseSourcesFromDirectory(String inputPath) throws IOException {
        Path path = Paths.get(inputPath);
        List<Path> inputJavaFiles = Files.walk(path, FileVisitOption.FOLLOW_LINKS)
                .filter(file -> !Files.isDirectory(file) && file.getFileName().toString().endsWith(JAVA_FILE_EXTENSION))
                .collect(Collectors.toList());
        return inputJavaFiles.stream().parallel().map(javaSource -> {
            try {
                String sourceText = new String(Files.readAllBytes(javaSource));
                String relativePath = new File(javaSource.toFile().getAbsolutePath()).getParentFile().getAbsolutePath().replace(path.toFile().getAbsolutePath() + File.separator, "");
                String fileName = javaSource.toFile().getName();
                return parseFile(sourceText, fileName, relativePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(List::stream).collect(Collectors.toList());
    }

    private static List<JavaDoc> parseSourcesFromZipArchive(String inputPath) throws IOException{
        List<JavaDoc> javadocs = new CopyOnWriteArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(ForkJoinPool.getCommonPoolParallelism());
        try (InputStream jarInStream = new FileInputStream(inputPath)){
            ZipInputStream zip = new ZipInputStream(jarInStream);
            ZipEntry zipEntry;
            while((zipEntry = zip.getNextEntry())!=null) {
                String name = zipEntry.getName();
                if (name.endsWith(ExtractJavadocModel.JAVA_FILE_EXTENSION)) {
                    String fileName = name.substring(name.lastIndexOf("/")+1, name.length());
                    String relativePath = name.substring(0, name.lastIndexOf("/"));
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(zip));
                    String javaSourceText = buffer.lines().collect(Collectors.joining("\n"));
                    executorService.submit(()-> javadocs.addAll(ExtractJavadocModel.parseFile(javaSourceText, fileName, relativePath)));
                }
            }
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(COMPILATION_TIMEOUT, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        return javadocs;
    }


    private static void checkParameters(String inputDirectory, String javaDocFile) {
        checkInputPath(inputDirectory);
        checkOutputJavadocPath(javaDocFile);
    }

    private static void checkInputPath(String inputPath) {
        File inDir = new File(inputPath);
        if(inDir.exists() && isZipFile(inputPath)){
            return;
        }
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

    private static boolean isZipFile(String inputPath) {
        return inputPath.endsWith(JAR_FILE_EXTENSION) || inputPath.endsWith(ZIP_FILE_EXTENSION);
    }
}

