package com.example.blog.dao;

import com.example.blog.config.TestConfig;
import com.example.blog.exception.NotFoundException;
import com.example.blog.model.Comment;
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
 * DAO tests for comments. Shares the cached Spring context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
class CommentDaoTest {

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private PostDao postDao;

    @Test
    void crud() {
        long postId = postDao.insert("Title", "Text", List.of());

        long id = commentDao.insert(postId, "First");
        commentDao.insert(postId, "Second");

        List<Comment> comments = commentDao.findByPostId(postId);
        assertThat(comments).extracting(Comment::getText).containsExactly("First", "Second");
        assertThat(commentDao.countByPostId(postId)).isEqualTo(2);

        commentDao.update(postId, id, "Edited");
        assertThat(commentDao.findById(postId, id).orElseThrow().getText()).isEqualTo("Edited");

        commentDao.delete(postId, id);
        assertThat(commentDao.findById(postId, id)).isEmpty();
        assertThat(commentDao.countByPostId(postId)).isEqualTo(1);
    }

    @Test
    void updateMissingCommentThrows404() {
        long postId = postDao.insert("Title", "Text", List.of());

        assertThatThrownBy(() -> commentDao.update(postId, 999L, "x"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> commentDao.delete(postId, 999L))
                .isInstanceOf(NotFoundException.class);
    }
}
