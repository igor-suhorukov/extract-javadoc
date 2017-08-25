package com.github.igorsuhorukov.javadoc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.igorsuhorukov.javadoc.model.JavaDoc;
import org.tukaani.xz.XZInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ReadJavaDocModel {
    private ReadJavaDocModel() {
        throw new UnsupportedOperationException("utility class");
    }

    public static List<JavaDoc> readJavaDoc(File inFile) throws java.io.IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = getInputStream(inFile)){
            return objectMapper.readValue(inputStream, new TypeReference<List<JavaDoc>>(){});
        }
    }

    private static InputStream getInputStream(File inFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(inFile);
        if(inFile.getName().toLowerCase().endsWith(ExtractJavadocModel.XZ_ARCHIVE_EXTENSION)){
            return new XZInputStream(inputStream);
        } else {
            return inputStream;
        }
    }
}
