package com.example.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/posts request body.
 */
public class CreatePostRequest {

    @NotBlank(message = "Post title is required")
    @Size(max = 255, message = "Post title must be at most 255 characters")
    private String title;

    @NotBlank(message = "Post text is required")
    private String text;

    @NotNull(message = "Tags are required")
    @Size(max = PostValidation.MAX_TAGS, message = "Too many tags")
    private List<@NotBlank(message = "Tag must not be blank")
            @Size(max = PostValidation.MAX_TAG_LENGTH,
                    message = "Tag must be at most 100 characters") String> tags = new ArrayList<>();

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
