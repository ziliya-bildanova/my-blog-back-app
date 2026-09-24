package com.example.blog.dao;

import com.example.blog.model.Comment;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for comments.
 */
public interface CommentDao {

    long insert(long postId, String text);

    void update(long postId, long commentId, String text);

    void delete(long postId, long commentId);

    Optional<Comment> findById(long postId, long commentId);

    List<Comment> findByPostId(long postId);

    long countByPostId(long postId);
}
