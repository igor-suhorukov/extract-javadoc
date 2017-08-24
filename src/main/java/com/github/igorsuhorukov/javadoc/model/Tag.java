package com.github.igorsuhorukov.javadoc.model;

import java.util.List;

public class Tag {
    private String name;
    private List<String> fragments;

    public void setName(String name) {
        this.name = name;
    }

    public void setFragments(List<String> fragments) {
        this.fragments = fragments;
    }
}
