package com.example.blog.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToPreviewTest {

    @Test
    void shortTextUnchanged() {
        assertThat(PostServiceImpl.toPreview("abc")).isEqualTo("abc");
    }

    @Test
    void boundary128Unchanged() {
        assertThat(PostServiceImpl.toPreview("x".repeat(128))).hasSize(128);
    }

    @Test
    void longTextTruncated() {
        String preview = PostServiceImpl.toPreview("x".repeat(200));
        assertThat(preview).hasSize(129);
        assertThat(preview).endsWith("…");
    }
}
