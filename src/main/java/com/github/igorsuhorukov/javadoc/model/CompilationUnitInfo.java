package com.github.igorsuhorukov.javadoc.model;

public class CompilationUnitInfo {
    private String packageName;
    private String file;

    public CompilationUnitInfo() {
    }

    public CompilationUnitInfo(String packageName, String file) {
        this.packageName = packageName;
        this.file = file;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
