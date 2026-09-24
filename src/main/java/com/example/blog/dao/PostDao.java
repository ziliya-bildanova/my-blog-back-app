package com.example.blog.dao;

import com.example.blog.model.ImageData;
import com.example.blog.model.Post;

import java.util.List;
import java.util.Optional;

/**
 * Persistence operations for posts, tags and post images.
 */
public interface PostDao {

    long insert(String title, String text, List<String> tags);

    void update(long id, String title, String text, List<String> tags);

    void delete(long id);

    boolean exists(long id);

    Optional<Post> findById(long id);

    List<Post> findAll(String search, int limit, int offset);

    long count(String search);

    /**
     * Atomically increments the likes counter and returns the new value.
     */
    int incrementLikes(long id);

    void saveImage(long id, byte[] data, String contentType);

    Optional<ImageData> findImage(long id);
}
