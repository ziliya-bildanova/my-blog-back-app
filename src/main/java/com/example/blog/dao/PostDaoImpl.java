package com.example.blog.dao;

import com.example.blog.model.ImageData;
import com.example.blog.model.Post;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link PostDao} implementation based on Spring {@link JdbcTemplate}.
 * <ul>
 *   <li>Feed text is truncated to a 128-char preview in SQL, full CLOBs are never loaded for the list.</li>
 *   <li>Tags for a whole page are loaded with one batch query (no N+1).</li>
 *   <li>Search is case-insensitive, {@code %} and {@code _} are escaped;
 *       SELECT and COUNT share the same condition.</li>
 * </ul>
 */
@Repository
public class PostDaoImpl implements PostDao {

    /** Feed preview, mirrors {@code PostServiceImpl.PREVIEW_LIMIT}. */
    private static final String PREVIEW_EXPR =
            "CASE WHEN LENGTH(p.text) > 128 THEN SUBSTRING(p.text, 1, 128) || '…' ELSE p.text END";

    private static final String POST_COLUMNS =
            "p.id, p.title, " + PREVIEW_EXPR + " AS text, p.likes_count, "
                    + "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments_count ";

    private static final String SEARCH_CONDITION =
            "LOWER(p.title) LIKE ? ESCAPE '!'"
                    + " OR LOWER(p.text) LIKE ? ESCAPE '!'"
                    + " OR EXISTS (SELECT 1 FROM post_tags t WHERE t.post_id = p.id"
                    + " AND LOWER(t.tag) LIKE ? ESCAPE '!')";

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
    public boolean update(long id, String title, String text, List<String> tags) {
        int rows = jdbcTemplate.update(
                "UPDATE posts SET title = ?, text = ? WHERE id = ?", title, text, id);
        if (rows == 0) {
            return false;
        }
        replaceTags(id, tags);
        return true;
    }

    @Override
    public boolean delete(long id) {
        return jdbcTemplate.update("DELETE FROM posts WHERE id = ?", id) > 0;
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
        attachTags(posts);
        return Optional.of(posts.get(0));
    }

    @Override
    public List<Post> findAll(String search, int limit, long offset) {
        SearchClause clause = searchClause(search);
        List<Object> params = new ArrayList<>(clause.params);
        params.add(limit);
        params.add(offset);
        List<Post> posts = jdbcTemplate.query(
                "SELECT " + POST_COLUMNS + "FROM posts p"
                        + clause.where + " ORDER BY p.id DESC LIMIT ? OFFSET ?",
                POST_ROW_MAPPER, params.toArray());
        attachTags(posts);
        return posts;
    }

    @Override
    public long count(String search) {
        SearchClause clause = searchClause(search);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts p" + clause.where,
                Long.class, clause.params.toArray());
        return total == null ? 0 : total;
    }

    @Override
    public Optional<Integer> incrementLikes(long id) {
        int rows = jdbcTemplate.update(
                "UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?", id);
        if (rows == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT likes_count FROM posts WHERE id = ?", Integer.class, id));
    }

    @Override
    public boolean saveImage(long id, byte[] data, String contentType) {
        return jdbcTemplate.update(
                "UPDATE posts SET image = ?, image_content_type = ? WHERE id = ?",
                data, contentType, id) > 0;
    }

    @Override
    public Optional<ImageData> findImage(long id) {
        return jdbcTemplate.query(
                "SELECT image, image_content_type FROM posts WHERE id = ?",
                rs -> {
                    if (!rs.next() || rs.getBytes(1) == null) {
                        return Optional.empty();
                    }
                    return Optional.of(new ImageData(rs.getBytes(1), rs.getString(2)));
                }, id);
    }

    /** Builds the WHERE fragment (shared by SELECT and COUNT) plus its parameters. */
    private static SearchClause searchClause(String search) {
        if (search == null || search.isBlank()) {
            return new SearchClause("", List.of());
        }
        String like = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
        return new SearchClause(" WHERE " + SEARCH_CONDITION, List.of(like, like, like));
    }

    /** Escapes LIKE wildcards so % and _ are searched literally ('!' is the ESCAPE char). */
    static String escapeLike(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private void attachTags(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (Post post : posts) {
            ids.add(post.getId());
        }
        Map<Long, List<String>> tagsByPost = new LinkedHashMap<>();
        for (Long id : ids) {
            tagsByPost.put(id, new ArrayList<>());
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        jdbcTemplate.query(
                "SELECT post_id, tag FROM post_tags WHERE post_id IN (" + placeholders + ")"
                        + " ORDER BY post_id, tag",
                rs -> {
                    List<String> tags = tagsByPost.get(rs.getLong(1));
                    if (tags != null) {
                        tags.add(rs.getString(2));
                    }
                }, ids.toArray());
        for (Post post : posts) {
            post.setTags(tagsByPost.get(post.getId()));
        }
    }

    private void replaceTags(long postId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = ?", postId);
        if (tags == null) {
            return;
        }
        // Deduplicate: post_tags has PRIMARY KEY(post_id, tag).
        for (String tag : new LinkedHashSet<>(tags)) {
            if (tag != null && !tag.isBlank()) {
                jdbcTemplate.update(
                        "INSERT INTO post_tags (post_id, tag) VALUES (?, ?)", postId, tag);
            }
        }
    }

    private record SearchClause(String where, List<Object> params) {
    }
}
