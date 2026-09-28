package com.example.blog.service;

import com.example.blog.BaseSpringTest;
import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CreateCommentRequest;
import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.UpdateCommentRequest;
import com.example.blog.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service tests for comments. Shares the cached Spring context.
 */
class CommentServiceTest extends BaseSpringTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostService postService;

    @Test
    void crud() {
        long postId = createPost();

        CommentDto created = createComment(postId, "Hello");
        assertThat(created.getId()).isNotNull();
        assertThat(created.getPostId()).isEqualTo(postId);

        assertThat(commentService.getComments(postId)).hasSize(1);

        UpdateCommentRequest update = new UpdateCommentRequest();
        update.setId(created.getId());
        update.setText("Edited");
        update.setPostId(postId);
        CommentDto updated = commentService.updateComment(postId, created.getId(), update);
        assertThat(updated.getText()).isEqualTo("Edited");

        assertThat(commentService.getComment(postId, created.getId()).getText()).isEqualTo("Edited");

        commentService.deleteComment(postId, created.getId());
        assertThat(commentService.getComments(postId)).isEmpty();
    }

    @Test
    void missingPostOrComment() {
        assertThatThrownBy(() -> commentService.getComments(999999L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> createComment(999999L, "x"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Post not found");

        long postId = createPost();
        assertThatThrownBy(() -> commentService.getComment(postId, 999999L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commentService.deleteComment(postId, 999999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void idMismatchRejected() {
        long postId = createPost();

        CreateCommentRequest create = new CreateCommentRequest();
        create.setText("x");
        create.setPostId(postId + 1);
        assertThatThrownBy(() -> commentService.createComment(postId, create))
                .isInstanceOf(IllegalArgumentException.class);

        CommentDto created = createComment(postId, "Hello");
        UpdateCommentRequest update = new UpdateCommentRequest();
        update.setId(created.getId() + 1);
        update.setText("x");
        update.setPostId(postId);
        assertThatThrownBy(() -> commentService.updateComment(postId, created.getId(), update))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private long createPost() {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Title");
        request.setText("Text");
        request.setTags(List.of());
        PostDto post = postService.createPost(request);
        return post.getId();
    }

    private CommentDto createComment(long postId, String text) {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setText(text);
        request.setPostId(postId);
        return commentService.createComment(postId, request);
    }
}
