package com.example.blog.exception;

/**
 * Thrown when an uploaded file exceeds the allowed size. Mapped to HTTP 413.
 */
public class PayloadTooLargeException extends RuntimeException {

    public PayloadTooLargeException(String message) {
        super(message);
    }
}
