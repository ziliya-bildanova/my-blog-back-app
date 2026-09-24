package com.example.blog.model;

/**
 * Post image bytes with the content type supplied on upload.
 */
public class ImageData {

    private final byte[] bytes;
    private final String contentType;

    public ImageData(byte[] bytes, String contentType) {
        this.bytes = bytes;
        this.contentType = contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getContentType() {
        return contentType;
    }
}
