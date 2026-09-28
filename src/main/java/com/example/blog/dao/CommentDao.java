package com.example.blog.dao;

import com.example.blog.model.Comment;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for comments.
 * Absence is reported with {@code boolean}/{@code Optional};
 * translating it into errors is the service layer's job.
 */
public interface CommentDao {

    /**
     * Inserts a comment in a single statement that also checks the post exists,
     * so no race window remains between a SELECT and the INSERT.
     *
     * @return the new id, or empty if the post does not exist.
     */
    Optional<Long> insertIfPostExists(long postId, String text);

    /** @return true if a row was updated. */
    boolean update(long postId, long commentId, String text);

    /** @return true if a row was deleted. */
    boolean delete(long postId, long commentId);

    Optional<Comment> findById(long postId, long commentId);

    List<Comment> findByPostId(long postId);

    long countByPostId(long postId);
}
