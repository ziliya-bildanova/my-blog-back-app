package com.example.blog.service;

import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.model.ImageData;

/**
 * Business logic for posts.
 */
public interface PostService {

    PostListResponse getPosts(String search, int pageNumber, int pageSize);

    PostDto getPost(long id);

    PostDto createPost(CreatePostRequest request);

    PostDto updatePost(long id, UpdatePostRequest request);

    void deletePost(long id);

    int likePost(long id);

    void saveImage(long id, byte[] data, String contentType);

    ImageData getImage(long id);
}
