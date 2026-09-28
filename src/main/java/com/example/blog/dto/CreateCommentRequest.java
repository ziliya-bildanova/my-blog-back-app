package com.example.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/posts/{postId}/comments request body.
 * The {@code postId} must equal the post id from the URL.
 */
public class CreateCommentRequest {

    @NotBlank(message = "Comment text is required")
    @Size(max = PostValidation.MAX_COMMENT_LENGTH,
            message = "Comment must be at most 2000 characters")
    private String text;

    @NotNull(message = "Post id is required")
    private Long postId;

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
