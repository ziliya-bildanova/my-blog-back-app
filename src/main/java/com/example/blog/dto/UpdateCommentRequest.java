package com.example.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/posts/{postId}/comments/{commentId} request body.
 * The {@code id} must equal the comment id from the URL,
 * the {@code postId} must equal the post id from the URL.
 */
public class UpdateCommentRequest {

    @NotNull(message = "Comment id is required")
    private Long id;

    @NotBlank(message = "Comment text is required")
    @Size(max = PostValidation.MAX_COMMENT_LENGTH,
            message = "Comment must be at most 2000 characters")
    private String text;

    @NotNull(message = "Post id is required")
    private Long postId;

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
