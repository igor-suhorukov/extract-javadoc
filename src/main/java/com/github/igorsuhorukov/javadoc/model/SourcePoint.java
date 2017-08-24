package com.github.igorsuhorukov.javadoc.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Method.class, name = "Method"),
        @JsonSubTypes.Type(value = Type.class, name = "Type") }
)
public class SourcePoint {
    private CompilationUnitInfo unitInfo;

    public CompilationUnitInfo getUnitInfo() {
        return unitInfo;
    }

    public void setUnitInfo(CompilationUnitInfo unitInfo) {
        this.unitInfo = unitInfo;
    }
}
