package com.example.blog.controller;

import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CreateCommentRequest;
import com.example.blog.dto.UpdateCommentRequest;
import com.example.blog.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for post comments.
 */
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentDto> list(@PathVariable("postId") long postId) {
        return commentService.getComments(postId);
    }

    @GetMapping("/{commentId}")
    public CommentDto getOne(@PathVariable("postId") long postId, @PathVariable("commentId") long commentId) {
        return commentService.getComment(postId, commentId);
    }

    @PostMapping
    public CommentDto create(@PathVariable("postId") long postId, @RequestBody CreateCommentRequest request) {
        return commentService.createComment(postId, request);
    }

    @PutMapping("/{commentId}")
    public CommentDto update(
            @PathVariable("postId") long postId,
            @PathVariable("commentId") long commentId,
            @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(postId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable("postId") long postId, @PathVariable("commentId") long commentId) {
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.ok().build();
    }
}
