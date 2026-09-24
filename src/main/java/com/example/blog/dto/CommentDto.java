package com.example.blog.dto;

import com.example.blog.model.Comment;

/**
 * Single comment representation in API responses.
 */
public class CommentDto {

    private Long id;
    private String text;
    private Long postId;

    public CommentDto() {
    }

    public static CommentDto from(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setPostId(comment.getPostId());
        return dto;
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
}
