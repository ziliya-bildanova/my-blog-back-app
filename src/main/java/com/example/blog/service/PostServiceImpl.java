package com.example.blog.service;

import com.example.blog.dao.PostDao;
import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.exception.NotFoundException;
import com.example.blog.model.ImageData;
import com.example.blog.model.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link PostService} implementation.
 */
@Service
public class PostServiceImpl implements PostService {

    static final int PREVIEW_LIMIT = 128;

    private final PostDao postDao;

    public PostServiceImpl(PostDao postDao) {
        this.postDao = postDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PostListResponse getPosts(String search, int pageNumber, int pageSize) {
        String query = search == null ? "" : search;
        int size = pageSize < 1 ? 5 : pageSize;
        long total = postDao.count(query);
        int lastPage = (int) Math.max(1, (total + size - 1) / size);
        int page = Math.min(Math.max(pageNumber, 1), lastPage);
        int offset = (page - 1) * size;

        List<PostDto> posts = postDao.findAll(query, size, offset).stream()
                .map(post -> {
                    PostDto dto = PostDto.from(post);
                    dto.setText(toPreview(post.getText()));
                    return dto;
                })
                .toList();

        return new PostListResponse(posts, page > 1, page < lastPage, lastPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getPost(long id) {
        return PostDto.from(requirePost(id));
    }

    @Override
    @Transactional
    public PostDto createPost(CreatePostRequest request) {
        requireTitle(request.getTitle());
        requireText(request.getText());
        long id = postDao.insert(request.getTitle(), request.getText(), request.getTags());
        return PostDto.from(requirePost(id));
    }

    @Override
    @Transactional
    public PostDto updatePost(long id, UpdatePostRequest request) {
        requireTitle(request.getTitle());
        requireText(request.getText());
        postDao.update(id, request.getTitle(), request.getText(), request.getTags());
        return PostDto.from(requirePost(id));
    }

    @Override
    @Transactional
    public void deletePost(long id) {
        postDao.delete(id);
    }

    @Override
    @Transactional
    public int likePost(long id) {
        return postDao.incrementLikes(id);
    }

    @Override
    @Transactional
    public void saveImage(long id, byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        requirePost(id);
        postDao.saveImage(id, data, contentType);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageData getImage(long id) {
        return postDao.findImage(id)
                .orElseThrow(() -> new NotFoundException("Image not found for post: " + id));
    }

    private Post requirePost(long id) {
        return postDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Post not found: " + id));
    }

    private static void requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Post title is required");
        }
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Post text is required");
        }
    }

    static String toPreview(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, PREVIEW_LIMIT) + "…";
    }
}
