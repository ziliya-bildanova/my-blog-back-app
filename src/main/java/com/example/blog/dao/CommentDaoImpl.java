package com.example.blog.dao;

import com.example.blog.model.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
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
    public Optional<Long> insertIfPostExists(long postId, String text) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rows = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO comments (post_id, text) SELECT id, ? FROM posts WHERE id = ?",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, text);
            ps.setLong(2, postId);
            return ps;
        }, keyHolder);
        if (rows == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(keyHolder.getKey()).map(Number::longValue);
    }

    @Override
    public boolean update(long postId, long commentId, String text) {
        return jdbcTemplate.update(
                "UPDATE comments SET text = ? WHERE id = ? AND post_id = ?",
                text, commentId, postId) > 0;
    }

    @Override
    public boolean delete(long postId, long commentId) {
        return jdbcTemplate.update(
                "DELETE FROM comments WHERE id = ? AND post_id = ?",
                commentId, postId) > 0;
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
