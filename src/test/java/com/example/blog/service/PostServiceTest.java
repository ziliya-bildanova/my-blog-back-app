package com.example.blog.service;

import com.example.blog.BaseSpringTest;
import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.exception.NotFoundException;
import com.example.blog.exception.PayloadTooLargeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service tests with the real DAO layer on embedded H2.
 * Shares the cached Spring context.
 */
class PostServiceTest extends BaseSpringTest {

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
    void listSearchIsCaseInsensitiveAndEscapesWildcards() {
        create("100% sale", "body");
        create("Other", "price 1000", List.of());

        assertThat(postService.getPosts("100%", 1, 5).getPosts()).hasSize(1);
        assertThat(postService.getPosts("SALE", 1, 5).getPosts()).hasSize(1);
    }

    @Test
    void invalidPaginationRejected() {
        assertThatThrownBy(() -> postService.getPosts("", 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> postService.getPosts("", 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> postService.getPosts("", 1, 101))
                .isInstanceOf(IllegalArgumentException.class);
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
    void updateIdMismatchRejected() {
        PostDto created = create("Title", "Text");
        UpdatePostRequest update = new UpdatePostRequest();
        update.setId(created.getId() + 1);
        update.setTitle("New");
        update.setText("New text");

        assertThatThrownBy(() -> postService.updatePost(created.getId(), update))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThatThrownBy(() -> postService.deletePost(999999L))
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

    @Test
    void imageErrors() {
        PostDto created = create("Title", "Text");

        // Post without image vs missing post are different errors.
        assertThatThrownBy(() -> postService.getImage(created.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Image not found");
        assertThatThrownBy(() -> postService.getImage(999999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Post not found");
        assertThatThrownBy(() -> postService.saveImage(created.getId(), new byte[0], "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> postService.saveImage(
                        created.getId(), new byte[(int) PostServiceImpl.MAX_IMAGE_SIZE + 1], "image/png"))
                .isInstanceOf(PayloadTooLargeException.class);
    }

    private PostDto create(String title, String text) {
        return create(title, text, List.of("tag_1", "tag_2"));
    }

    private PostDto create(String title, String text, List<String> tags) {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(title);
        request.setText(text);
        request.setTags(tags);
        return postService.createPost(request);
    }
}
