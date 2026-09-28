package com.example.blog.dto;

/**
 * Shared validation limits (mirror the DB column sizes from schema.sql).
 */
public final class PostValidation {

    /** posts.title VARCHAR(255). */
    public static final int MAX_TITLE_LENGTH = 255;
    /** post_tags.tag VARCHAR(100). */
    public static final int MAX_TAG_LENGTH = 100;
    /** Max number of tags per post. */
    public static final int MAX_TAGS = 10;
    /** comments.text VARCHAR(2000). */
    public static final int MAX_COMMENT_LENGTH = 2000;

    private PostValidation() {
    }
}
