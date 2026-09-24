package com.example.blog.service;

import com.example.blog.dao.CommentDao;
import com.example.blog.dao.PostDao;
import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CreateCommentRequest;
import com.example.blog.dto.UpdateCommentRequest;
import com.example.blog.exception.NotFoundException;
import com.example.blog.model.Comment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link CommentService} implementation.
 */
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentDao commentDao;
    private final PostDao postDao;

    public CommentServiceImpl(CommentDao commentDao, PostDao postDao) {
        this.commentDao = commentDao;
        this.postDao = postDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getComments(long postId) {
        requirePost(postId);
        return commentDao.findByPostId(postId).stream()
                .map(CommentDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getComment(long postId, long commentId) {
        requirePost(postId);
        return CommentDto.from(requireComment(postId, commentId));
    }

    @Override
    @Transactional
    public CommentDto createComment(long postId, CreateCommentRequest request) {
        requirePost(postId);
        if (request.getText() == null || request.getText().isBlank()) {
            throw new IllegalArgumentException("Comment text is required");
        }
        long id = commentDao.insert(postId, request.getText());
        return CommentDto.from(requireComment(postId, id));
    }

    @Override
    @Transactional
    public CommentDto updateComment(long postId, long commentId, UpdateCommentRequest request) {
        requirePost(postId);
        if (request.getText() == null || request.getText().isBlank()) {
            throw new IllegalArgumentException("Comment text is required");
        }
        commentDao.update(postId, commentId, request.getText());
        return CommentDto.from(requireComment(postId, commentId));
    }

    @Override
    @Transactional
    public void deleteComment(long postId, long commentId) {
        requirePost(postId);
        commentDao.delete(postId, commentId);
    }

    private void requirePost(long postId) {
        if (!postDao.exists(postId)) {
            throw new NotFoundException("Post not found: " + postId);
        }
    }

    private Comment requireComment(long postId, long commentId) {
        return commentDao.findById(postId, commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment not found: " + commentId + " for post " + postId));
    }
}
