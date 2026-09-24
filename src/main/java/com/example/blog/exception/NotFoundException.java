package com.example.blog.exception;

/**
 * Thrown when a post, comment or image is not found. Mapped to HTTP 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
