package com.github.igorsuhorukov.javadoc.model;

import java.util.List;

public class JavaDoc {
    private String comment;
    private List<Tag> tags;
    private SourcePoint sourcePoint;

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void setSourcePoint(SourcePoint sourcePoint) {
        this.sourcePoint = sourcePoint;
    }
}
