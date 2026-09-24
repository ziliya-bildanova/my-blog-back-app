package com.example.blog.model;

import java.util.Objects;

/**
 * Comment on a post. Field names match the JSON contract with the frontend.
 */
public class Comment {

    private Long id;
    private String text;
    private Long postId;

    public Comment() {
    }

    public Comment(Long id, String text, Long postId) {
        this.id = id;
        this.text = text;
        this.postId = postId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment comment)) return false;
        return Objects.equals(id, comment.id)
                && Objects.equals(text, comment.text)
                && Objects.equals(postId, comment.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, postId);
    }
}
