package com.example.blog.controller;

import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.model.ImageData;
import com.example.blog.service.PostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST endpoints for posts, likes and post images.
 * The frontend runs on another origin (nginx :80), so CORS is open.
 */
@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public PostListResponse list(
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "5") int pageSize) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    /**
     * Single post. GET is used by the frontend; POST is also accepted
     * for compatibility with the spec text.
     */
    @RequestMapping(value = "/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public PostDto getOne(@PathVariable("id") long id) {
        return postService.getPost(id);
    }

    @PostMapping
    public PostDto create(@RequestBody CreatePostRequest request) {
        return postService.createPost(request);
    }

    @PutMapping("/{id}")
    public PostDto update(@PathVariable("id") long id, @RequestBody UpdatePostRequest request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        postService.deletePost(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/likes", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> like(@PathVariable("id") long id) {
        return ResponseEntity.ok(String.valueOf(postService.likePost(id)));
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(
            @PathVariable("id") long id,
            @RequestParam("image") MultipartFile image) throws IOException {
        postService.saveImage(id, image.getBytes(), image.getContentType());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") long id) {
        ImageData image = postService.getImage(id);
        MediaType contentType;
        try {
            contentType = image.getContentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(image.getContentType());
        } catch (IllegalArgumentException ex) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(contentType).body(image.getBytes());
    }
}
