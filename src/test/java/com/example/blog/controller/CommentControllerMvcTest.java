package com.example.blog.controller;

import com.example.blog.dto.CommentDto;
import com.example.blog.dto.CreateCommentRequest;
import com.example.blog.dto.UpdateCommentRequest;
import com.example.blog.exception.GlobalExceptionHandler;
import com.example.blog.exception.NotFoundException;
import com.example.blog.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test for comments: routing, JSON, validation, errors.
 * The service is mocked; no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class CommentControllerMvcTest {

    @Mock
    private CommentService commentService;

    @Captor
    private ArgumentCaptor<CreateCommentRequest> createCaptor;

    @Captor
    private ArgumentCaptor<UpdateCommentRequest> updateCaptor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listComments() throws Exception {
        when(commentService.getComments(eq(1L))).thenReturn(List.of(commentDto(2L, 1L)));

        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].text").value("Text"))
                .andExpect(jsonPath("$[0].postId").value(1));

        verify(commentService).getComments(1L);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void getOneComment() throws Exception {
        when(commentService.getComment(eq(1L), eq(2L))).thenReturn(commentDto(2L, 1L));

        mockMvc.perform(get("/api/posts/1/comments/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(commentService).getComment(1L, 2L);
    }

    @Test
    void createCommentPassesBody() throws Exception {
        when(commentService.createComment(eq(1L), any())).thenReturn(commentDto(2L, 1L));

        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hi\",\"postId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(commentService).createComment(eq(1L), createCaptor.capture());
        assertThat(createCaptor.getValue().getText()).isEqualTo("Hi");
        assertThat(createCaptor.getValue().getPostId()).isEqualTo(1L);
    }

    @Test
    void createCommentRejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \",\"postId\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void updateCommentPassesBody() throws Exception {
        when(commentService.updateComment(eq(1L), eq(2L), any())).thenReturn(commentDto(2L, 1L));

        mockMvc.perform(put("/api/posts/1/comments/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2,\"text\":\"New\",\"postId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Text"));

        verify(commentService).updateComment(eq(1L), eq(2L), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(2L);
        assertThat(updateCaptor.getValue().getText()).isEqualTo("New");
    }

    @Test
    void deleteComment() throws Exception {
        mockMvc.perform(delete("/api/posts/1/comments/2"))
                .andExpect(status().isOk());

        verify(commentService).deleteComment(1L, 2L);
        verifyNoMoreInteractions(commentService);
    }

    @Test
    void missingCommentMapsTo404() throws Exception {
        when(commentService.getComment(eq(1L), eq(99L)))
                .thenThrow(new NotFoundException("Comment not found: 99 for post 1"));

        mockMvc.perform(get("/api/posts/1/comments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(commentService).getComment(1L, 99L);
        verifyNoMoreInteractions(commentService);
    }

    private static CommentDto commentDto(long id, long postId) {
        CommentDto dto = new CommentDto();
        dto.setId(id);
        dto.setText("Text");
        dto.setPostId(postId);
        return dto;
    }
}
