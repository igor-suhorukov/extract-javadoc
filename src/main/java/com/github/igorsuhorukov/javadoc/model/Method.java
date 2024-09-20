package com.github.igorsuhorukov.javadoc.model;

import java.util.List;

public class Method extends SourcePoint{
    private Type type;
    private String name;
    private boolean constructor;
    private List<String> params;
    private String returnType;
    private int line;

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void setLine(int line) {
        this.line = line;
    }
}
