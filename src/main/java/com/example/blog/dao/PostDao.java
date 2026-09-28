package com.example.blog.dao;

import com.example.blog.model.ImageData;
import com.example.blog.model.Post;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for posts, tags and post images.
 * Methods report "not affected" with {@code boolean}/{@code Optional}
 * instead of throwing: translating absence into HTTP errors is
 * the service layer's job.
 */
public interface PostDao {

    long insert(String title, String text, List<String> tags);

    /** @return true if a row was updated. */
    boolean update(long id, String title, String text, List<String> tags);

    /** @return true if a row was deleted. */
    boolean delete(long id);

    boolean exists(long id);

    Optional<Post> findById(long id);

    List<Post> findAll(String search, int limit, long offset);

    long count(String search);

    /**
     * Atomically increments the likes counter.
     *
     * @return the new value, or empty if the post does not exist.
     */
    Optional<Integer> incrementLikes(long id);

    /** @return true if a row was updated. */
    boolean saveImage(long id, byte[] data, String contentType);

    /** Empty if the post has no image (missing post is checked via {@link #exists}). */
    Optional<ImageData> findImage(long id);
}
