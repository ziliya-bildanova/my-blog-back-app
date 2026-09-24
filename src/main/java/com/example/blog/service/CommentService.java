package com.example.blog.service;

import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CreateCommentRequest;
import com.example.blog.dto.UpdateCommentRequest;

import java.util.List;

/**
 * Business logic for comments.
 */
public interface CommentService {

    List<CommentDto> getComments(long postId);

    CommentDto getComment(long postId, long commentId);

    CommentDto createComment(long postId, CreateCommentRequest request);

    CommentDto updateComment(long postId, long commentId, UpdateCommentRequest request);

    void deleteComment(long postId, long commentId);
}
