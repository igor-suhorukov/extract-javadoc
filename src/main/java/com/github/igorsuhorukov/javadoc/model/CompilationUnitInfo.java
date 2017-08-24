package com.github.igorsuhorukov.javadoc.model;

public class CompilationUnitInfo {
    private String packageName;
    private String relativePath;
    private String file;

    public CompilationUnitInfo() {
    }

    public CompilationUnitInfo(String packageName, String relativePath, String file) {
        this.packageName = packageName;
        this.relativePath = relativePath;
        this.file = file;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
