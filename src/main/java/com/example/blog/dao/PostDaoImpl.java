package com.example.blog.dao;

import com.example.blog.exception.NotFoundException;
import com.example.blog.model.ImageData;
import com.example.blog.model.Post;
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
 * {@link PostDao} implementation based on Spring {@link JdbcTemplate}.
 */
@Repository
public class PostDaoImpl implements PostDao {

    private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setTitle(rs.getString("title"));
        post.setText(rs.getString("text"));
        post.setLikesCount(rs.getInt("likes_count"));
        post.setCommentsCount(rs.getInt("comments_count"));
        return post;
    };

    private final JdbcTemplate jdbcTemplate;

    public PostDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long insert(String title, String text, List<String> tags) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO posts (title, text, likes_count) VALUES (?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            ps.setString(2, text);
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        replaceTags(id, tags);
        return id;
    }

    @Override
    public void update(long id, String title, String text, List<String> tags) {
        int rows = jdbcTemplate.update(
                "UPDATE posts SET title = ?, text = ? WHERE id = ?", title, text, id);
        if (rows == 0) {
            throw new NotFoundException("Post not found: " + id);
        }
        replaceTags(id, tags);
    }

    @Override
    public void delete(long id) {
        int rows = jdbcTemplate.update("DELETE FROM posts WHERE id = ?", id);
        if (rows == 0) {
            throw new NotFoundException("Post not found: " + id);
        }
    }

    @Override
    public boolean exists(long id) {
        Long found = jdbcTemplate.query(
                "SELECT id FROM posts WHERE id = ?", rs -> rs.next() ? rs.getLong(1) : null, id);
        return found != null;
    }

    @Override
    public Optional<Post> findById(long id) {
        List<Post> posts = jdbcTemplate.query(
                "SELECT p.id, p.title, p.text, p.likes_count, "
                        + "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count "
                        + "FROM posts p WHERE p.id = ?",
                POST_ROW_MAPPER, id);
        if (posts.isEmpty()) {
            return Optional.empty();
        }
        Post post = posts.get(0);
        post.setTags(findTags(id));
        return Optional.of(post);
    }

    @Override
    public List<Post> findAll(String search, int limit, int offset) {
        List<Post> posts;
        if (search == null || search.isBlank()) {
            posts = jdbcTemplate.query(
                    "SELECT p.id, p.title, p.text, p.likes_count, "
                            + "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count "
                            + "FROM posts p ORDER BY p.id DESC LIMIT ? OFFSET ?",
                    POST_ROW_MAPPER, limit, offset);
        } else {
            String like = "%" + search + "%";
            posts = jdbcTemplate.query(
                    "SELECT p.id, p.title, p.text, p.likes_count, "
                            + "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count "
                            + "FROM posts p WHERE p.title LIKE ? OR p.text LIKE ? "
                            + "OR EXISTS (SELECT 1 FROM post_tags t WHERE t.post_id = p.id AND t.tag LIKE ?) "
                            + "ORDER BY p.id DESC LIMIT ? OFFSET ?",
                    POST_ROW_MAPPER, like, like, like, limit, offset);
        }
        for (Post post : posts) {
            post.setTags(findTags(post.getId()));
        }
        return posts;
    }

    @Override
    public long count(String search) {
        if (search == null || search.isBlank()) {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM posts", Long.class);
            return total == null ? 0 : total;
        }
        String like = "%" + search + "%";
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts p WHERE p.title LIKE ? OR p.text LIKE ? "
                        + "OR EXISTS (SELECT 1 FROM post_tags t WHERE t.post_id = p.id AND t.tag LIKE ?)",
                Long.class, like, like, like);
        return total == null ? 0 : total;
    }

    @Override
    public int incrementLikes(long id) {
        int rows = jdbcTemplate.update(
                "UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?", id);
        if (rows == 0) {
            throw new NotFoundException("Post not found: " + id);
        }
        Integer likes = jdbcTemplate.queryForObject(
                "SELECT likes_count FROM posts WHERE id = ?", Integer.class, id);
        return likes == null ? 0 : likes;
    }

    @Override
    public void saveImage(long id, byte[] data, String contentType) {
        int rows = jdbcTemplate.update(
                "UPDATE posts SET image = ?, image_content_type = ? WHERE id = ?", data, contentType, id);
        if (rows == 0) {
            throw new NotFoundException("Post not found: " + id);
        }
    }

    @Override
    public Optional<ImageData> findImage(long id) {
        if (!exists(id)) {
            throw new NotFoundException("Post not found: " + id);
        }
        return jdbcTemplate.query(
                "SELECT image, image_content_type FROM posts WHERE id = ?",
                rs -> {
                    if (!rs.next() || rs.getBytes(1) == null) {
                        return Optional.empty();
                    }
                    return Optional.of(new ImageData(rs.getBytes(1), rs.getString(2)));
                }, id);
    }

    private List<String> findTags(long postId) {
        return jdbcTemplate.queryForList(
                "SELECT tag FROM post_tags WHERE post_id = ? ORDER BY tag", String.class, postId);
    }

    private void replaceTags(long postId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = ?", postId);
        if (tags == null) {
            return;
        }
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                jdbcTemplate.update(
                        "INSERT INTO post_tags (post_id, tag) VALUES (?, ?)", postId, tag);
            }
        }
    }
}
