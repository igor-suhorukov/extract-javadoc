package com.github.igorsuhorukov.javadoc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.igorsuhorukov.javadoc.model.JavaDoc;

import java.io.File;
import java.util.List;

public class ReadJavaDocModel {
    private ReadJavaDocModel() {
        throw new UnsupportedOperationException("utility class");
    }

    public static List<JavaDoc> readJavaDoc(File inFile) throws java.io.IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inFile, new TypeReference<List<JavaDoc>>(){});
    }
}
