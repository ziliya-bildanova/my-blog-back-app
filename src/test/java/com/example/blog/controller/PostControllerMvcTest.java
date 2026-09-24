package com.example.blog.controller;

import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.exception.GlobalExceptionHandler;
import com.example.blog.exception.NotFoundException;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test: controllers + Jackson + exception handler,
 * services are mocked. No Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class PostControllerMvcTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostController(postService), new CommentController(commentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listPosts() throws Exception {
        PostDto post = new PostDto();
        post.setId(1L);
        post.setTitle("Title");
        post.setText("Text");
        post.setTags(List.of("tag_1"));
        post.setLikesCount(5);
        post.setCommentsCount(1);
        when(postService.getPosts(anyString(), anyInt(), anyInt()))
                .thenReturn(new PostListResponse(List.of(post), false, false, 1));

        mockMvc.perform(get("/api/posts")
                        .param("search", "Lalala")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].id").value(1))
                .andExpect(jsonPath("$.posts[0].tags[0]").value("tag_1"))
                .andExpect(jsonPath("$.posts[0].likesCount").value(5))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void getOneCreateUpdateDelete() throws Exception {
        PostDto post = new PostDto();
        post.setId(1L);
        post.setTitle("Title");
        post.setText("Text");
        post.setTags(List.of());
        when(postService.getPost(1L)).thenReturn(post);
        when(postService.createPost(any())).thenReturn(post);
        when(postService.updatePost(anyLong(), any())).thenReturn(post);

        mockMvc.perform(get("/api/posts/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(post("/api/posts/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        String body = "{\"title\":\"Title\",\"text\":\"Text\",\"tags\":[]}";
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(put("/api/posts/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(delete("/api/posts/1")).andExpect(status().isOk());
    }

    @Test
    void likesReturnsPlainNumber() throws Exception {
        when(postService.likePost(1L)).thenReturn(6);

        mockMvc.perform(post("/api/posts/1/likes"))
                .andExpect(status().isOk())
                .andExpect(content().string("6"));
    }

    @Test
    void notFoundMapsTo404() throws Exception {
        when(postService.getPost(99L)).thenThrow(new NotFoundException("Post not found: 99"));

        mockMvc.perform(get("/api/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
