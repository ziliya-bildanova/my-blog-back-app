package com.example.blog.dao;

import com.example.blog.BaseSpringTest;
import com.example.blog.model.Comment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAO tests for comments. Shares the cached Spring context.
 */
class CommentDaoTest extends BaseSpringTest {

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private PostDao postDao;

    @Test
    void crud() {
        long postId = postDao.insert("Title", "Text", List.of());

        long id = commentDao.insertIfPostExists(postId, "First").orElseThrow();
        commentDao.insertIfPostExists(postId, "Second");

        List<Comment> comments = commentDao.findByPostId(postId);
        assertThat(comments).extracting(Comment::getText).containsExactly("First", "Second");
        assertThat(commentDao.countByPostId(postId)).isEqualTo(2);

        assertThat(commentDao.update(postId, id, "Edited")).isTrue();
        assertThat(commentDao.findById(postId, id).orElseThrow().getText()).isEqualTo("Edited");

        assertThat(commentDao.delete(postId, id)).isTrue();
        assertThat(commentDao.findById(postId, id)).isEmpty();
        assertThat(commentDao.countByPostId(postId)).isEqualTo(1);
    }

    @Test
    void missingRows() {
        long postId = postDao.insert("Title", "Text", List.of());

        assertThat(commentDao.insertIfPostExists(999999L, "x")).isEmpty();
        assertThat(commentDao.update(postId, 999L, "x")).isFalse();
        assertThat(commentDao.delete(postId, 999L)).isFalse();
    }
}
