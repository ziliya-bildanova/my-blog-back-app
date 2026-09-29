package com.example.blog.service;

import com.example.blog.dao.PostDao;
import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.exception.NotFoundException;
import com.example.blog.exception.PayloadTooLargeException;
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
    /** Max feed page size; larger values are rejected with 400. */
    static final int MAX_PAGE_SIZE = 100;
    /** Max uploaded image: 5 MB (mirrors spring.servlet.multipart.max-file-size). */
    static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final PostDao postDao;

    public PostServiceImpl(PostDao postDao) {
        this.postDao = postDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PostListResponse getPosts(String search, int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
        String query = search == null ? "" : search;
        long total = postDao.count(query);
        int lastPage = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int page = Math.min(pageNumber, lastPage);
        long offset = (long) (page - 1) * pageSize;

        // Text comes pre-truncated to a preview from SQL; page beyond lastPage is empty.
        List<PostDto> posts = postDao.findAll(query, pageSize, offset).stream()
                .map(PostDto::from)
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
        if (!Long.valueOf(id).equals(request.getId())) {
            throw new IllegalArgumentException(
                    "Post id in body (" + request.getId() + ") does not match path id (" + id + ")");
        }
        requireTitle(request.getTitle());
        requireText(request.getText());
        if (!postDao.update(id, request.getTitle(), request.getText(), request.getTags())) {
            throw new NotFoundException("Post not found: " + id);
        }
        return PostDto.from(requirePost(id));
    }

    @Override
    @Transactional
    public void deletePost(long id) {
        if (!postDao.delete(id)) {
            throw new NotFoundException("Post not found: " + id);
        }
    }

    @Override
    @Transactional
    public int likePost(long id) {
        return postDao.incrementLikes(id)
                .orElseThrow(() -> new NotFoundException("Post not found: " + id));
    }

    @Override
    @Transactional
    public void saveImage(long id, byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Image file is empty");
        }
        if (data.length > MAX_IMAGE_SIZE) {
            throw new PayloadTooLargeException(
                    "Image exceeds max size of " + MAX_IMAGE_SIZE + " bytes");
        }
        requirePost(id);
        if (!postDao.saveImage(id, data, contentType)) {
            throw new NotFoundException("Post not found: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ImageData getImage(long id) {
        if (!postDao.exists(id)) {
            throw new NotFoundException("Post not found: " + id);
        }
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
