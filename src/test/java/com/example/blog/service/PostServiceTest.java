package com.example.blog.service;

import com.example.blog.config.TestConfig;
import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service tests with the real DAO layer on embedded H2.
 * Shares the cached Spring context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Test
    void listTruncatesLongTextAndPaginates() {
        create("Lalala 1", "a".repeat(200));
        create("Lalala 2", "short");
        create("Lalala 3", "b".repeat(128));

        PostListResponse page1 = postService.getPosts("Lalala", 1, 2);
        assertThat(page1.getPosts()).hasSize(2);
        assertThat(page1.isHasPrev()).isFalse();
        assertThat(page1.isHasNext()).isTrue();
        assertThat(page1.getLastPage()).isEqualTo(2);
        // newest first
        assertThat(page1.getPosts().get(0).getText()).hasSize(128);
        assertThat(page1.getPosts().get(1).getText()).isEqualTo("short");

        PostListResponse page2 = postService.getPosts("Lalala", 2, 2);
        assertThat(page2.getPosts()).hasSize(1);
        assertThat(page2.isHasPrev()).isTrue();
        assertThat(page2.isHasNext()).isFalse();
        assertThat(page2.getPosts().get(0).getText()).endsWith("…");
    }

    @Test
    void getPostReturnsFullText() {
        PostDto created = create("Title", "x".repeat(200));

        PostDto found = postService.getPost(created.getId());

        assertThat(found.getText()).hasSize(200);
        assertThat(found.getLikesCount()).isZero();
        assertThat(found.getCommentsCount()).isZero();
    }

    @Test
    void createAndUpdate() {
        PostDto created = create("Title", "Text");
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTags()).containsExactly("tag_1", "tag_2");

        UpdatePostRequest update = new UpdatePostRequest();
        update.setId(created.getId());
        update.setTitle("New title");
        update.setText("New text");
        update.setTags(List.of());
        PostDto updated = postService.updatePost(created.getId(), update);

        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getTags()).isEmpty();
    }

    @Test
    void validationAndNotFound() {
        CreatePostRequest bad = new CreatePostRequest();
        bad.setTitle(" ");
        bad.setText("text");
        assertThatThrownBy(() -> postService.createPost(bad))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> postService.getPost(999999L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> postService.likePost(999999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void likeAndImage() {
        PostDto created = create("Title", "Text");

        assertThat(postService.likePost(created.getId())).isEqualTo(1);
        assertThat(postService.getPost(created.getId()).getLikesCount()).isEqualTo(1);

        byte[] bytes = {10, 20, 30};
        postService.saveImage(created.getId(), bytes, "image/png");
        assertThat(postService.getImage(created.getId()).getBytes()).isEqualTo(bytes);
    }

    private PostDto create(String title, String text) {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(title);
        request.setText(text);
        request.setTags(List.of("tag_1", "tag_2"));
        return postService.createPost(request);
    }
}
