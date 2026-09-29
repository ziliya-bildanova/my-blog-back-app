package com.example.blog.controller;

import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.dto.PostListResponse;
import com.example.blog.dto.UpdatePostRequest;
import com.example.blog.exception.NotFoundException;
import com.example.blog.model.ImageData;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice test (Spring Boot Test): controllers + Jackson + validation +
 * exception handler from the real context, services replaced with mocks.
 */
@WebMvcTest(PostController.class)
class PostControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private CommentService commentService;

    private final ArgumentCaptor<CreatePostRequest> createCaptor =
            ArgumentCaptor.forClass(CreatePostRequest.class);
    private final ArgumentCaptor<UpdatePostRequest> updateCaptor =
            ArgumentCaptor.forClass(UpdatePostRequest.class);

    @Test
    void listPostsPassesExactParams() throws Exception {
        PostDto post = postDto(1L);
        when(postService.getPosts(eq("Lalala"), eq(1), eq(5)))
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
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));

        verify(postService).getPosts("Lalala", 1, 5);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void listRequiresAllParams() throws Exception {
        mockMvc.perform(get("/api/posts").param("search", "x").param("pageNumber", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    void getOne() throws Exception {
        when(postService.getPost(eq(1L))).thenReturn(postDto(1L));

        mockMvc.perform(get("/api/posts/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Title"));
        mockMvc.perform(post("/api/posts/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService, times(2)).getPost(1L);
    }

    @Test
    void createPassesBodyToService() throws Exception {
        when(postService.createPost(any())).thenReturn(postDto(1L));

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"text\":\"Text\",\"tags\":[\"a\",\"b\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService).createPost(createCaptor.capture());
        assertThat(createCaptor.getValue().getTitle()).isEqualTo("Title");
        assertThat(createCaptor.getValue().getText()).isEqualTo("Text");
        assertThat(createCaptor.getValue().getTags()).containsExactly("a", "b");
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"text\":\"Text\",\"tags\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    void updatePassesBodyToService() throws Exception {
        when(postService.updatePost(eq(1L), any())).thenReturn(postDto(1L));

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"title\":\"T\",\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(postService).updatePost(eq(1L), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(updateCaptor.getValue().getTitle()).isEqualTo("T");
    }

    @Test
    void deletePost() throws Exception {
        mockMvc.perform(delete("/api/posts/1")).andExpect(status().isOk());

        verify(postService).deletePost(1L);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void likesReturnsPlainNumber() throws Exception {
        when(postService.likePost(eq(1L))).thenReturn(6);

        mockMvc.perform(post("/api/posts/1/likes"))
                .andExpect(status().isOk())
                .andExpect(content().string("6"));

        verify(postService).likePost(1L);
    }

    @Test
    void uploadImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "pic.jpg", "image/jpeg", new byte[]{1, 2});

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/1/image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());

        verify(postService).saveImage(eq(1L), eq(new byte[]{1, 2}), eq("image/jpeg"));
    }

    @Test
    void uploadImageMissingPart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("other", "pic.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/1/image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());

        verify(postService, never()).saveImage(anyLong(), any(), any());
    }

    @Test
    void downloadImage() throws Exception {
        when(postService.getImage(eq(1L))).thenReturn(new ImageData(new byte[]{1, 2, 3}, "image/jpeg"));

        mockMvc.perform(get("/api/posts/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void notFoundMapsTo404WithoutExtraCalls() throws Exception {
        when(postService.getPost(eq(99L))).thenThrow(new NotFoundException("Post not found: 99"));

        mockMvc.perform(get("/api/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(postService).getPost(99L);
        verifyNoMoreInteractions(postService);
    }

    @Test
    void invalidIdMapsTo400Json() throws Exception {
        mockMvc.perform(get("/api/posts/undefined"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(postService);
    }

    private static PostDto postDto(long id) {
        PostDto post = new PostDto();
        post.setId(id);
        post.setTitle("Title");
        post.setText("Text");
        post.setTags(List.of("tag_1"));
        post.setLikesCount(5);
        post.setCommentsCount(1);
        return post;
    }
}
