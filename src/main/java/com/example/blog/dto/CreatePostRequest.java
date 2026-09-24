package com.example.blog.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/posts request body.
 */
public class CreatePostRequest {

    private String title;
    private String text;
    private List<String> tags = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
