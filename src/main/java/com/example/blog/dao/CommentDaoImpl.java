package com.example.blog.dao;

import com.example.blog.exception.NotFoundException;
import com.example.blog.model.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link CommentDao} implementation based on Spring {@link JdbcTemplate}.
 */
@Repository
public class CommentDaoImpl implements CommentDao {

    private static final RowMapper<Comment> COMMENT_ROW_MAPPER = (rs, rowNum) ->
            new Comment(rs.getLong("id"), rs.getString("text"), rs.getLong("post_id"));

    private final JdbcTemplate jdbcTemplate;

    public CommentDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long insert(long postId, String text) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO comments (post_id, text) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, postId);
            ps.setString(2, text);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @Override
    public void update(long postId, long commentId, String text) {
        int rows = jdbcTemplate.update(
                "UPDATE comments SET text = ? WHERE id = ? AND post_id = ?", text, commentId, postId);
        if (rows == 0) {
            throw new NotFoundException("Comment not found: " + commentId + " for post " + postId);
        }
    }

    @Override
    public void delete(long postId, long commentId) {
        int rows = jdbcTemplate.update(
                "DELETE FROM comments WHERE id = ? AND post_id = ?", commentId, postId);
        if (rows == 0) {
            throw new NotFoundException("Comment not found: " + commentId + " for post " + postId);
        }
    }

    @Override
    public Optional<Comment> findById(long postId, long commentId) {
        List<Comment> comments = jdbcTemplate.query(
                "SELECT id, post_id, text FROM comments WHERE id = ? AND post_id = ?",
                COMMENT_ROW_MAPPER, commentId, postId);
        return comments.stream().findFirst();
    }

    @Override
    public List<Comment> findByPostId(long postId) {
        return jdbcTemplate.query(
                "SELECT id, post_id, text FROM comments WHERE post_id = ? ORDER BY id",
                COMMENT_ROW_MAPPER, postId);
    }

    @Override
    public long countByPostId(long postId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ?", Long.class, postId);
        return count == null ? 0 : count;
    }
}
