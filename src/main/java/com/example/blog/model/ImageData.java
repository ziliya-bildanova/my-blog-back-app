package com.example.blog.model;

import java.util.Arrays;

/**
 * Post image bytes with the content type supplied on upload.
 * Defensive copies protect the internal array from outside mutation.
 */
public class ImageData {

    private final byte[] bytes;
    private final String contentType;

    public ImageData(byte[] bytes, String contentType) {
        this.bytes = bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
        this.contentType = contentType;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String getContentType() {
        return contentType;
    }
}
