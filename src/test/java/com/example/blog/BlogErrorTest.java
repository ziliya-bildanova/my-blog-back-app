package com.example.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error scenarios: validation, missing resources, malformed requests.
 * Every case asserts the status and a JSON error body.
 * Shares the cached Spring context.
 */
class BlogErrorTest extends BaseSpringTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private String createPost() throws Exception {
        String body = mockMvc().perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");
    }

    @Test
    void listValidation() throws Exception {
        MockMvc mockMvc = mockMvc();
        // missing required parameter
        mockMvc.perform(get("/api/posts").param("search", "x").param("pageNumber", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        // pageNumber < 1, pageSize < 1, pageSize over max
        mockMvc.perform(get("/api/posts").param("search", "").param("pageNumber", "0").param("pageSize", "5"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/posts").param("search", "").param("pageNumber", "1").param("pageSize", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/posts").param("search", "").param("pageNumber", "1").param("pageSize", "101"))
                .andExpect(status().isBadRequest());
        // not a number
        mockMvc.perform(get("/api/posts").param("search", "").param("pageNumber", "abc").param("pageSize", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void postValidation() throws Exception {
        MockMvc mockMvc = mockMvc();
        // missing title
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isBadRequest());
        // blank title
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isBadRequest());
        // title over 255
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "t".repeat(256) + "\",\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isBadRequest());
        // too many tags
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"text\":\"X\",\"tags\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\"]}"))
                .andExpect(status().isBadRequest());
        // tag over 100
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"text\":\"X\",\"tags\":[\"" + "t".repeat(101) + "\"]}"))
                .andExpect(status().isBadRequest());
        // malformed JSON, empty body, wrong content type
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/posts").contentType(MediaType.TEXT_PLAIN)
                        .content("{\"title\":\"T\",\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void postIdMismatchAndMissing() throws Exception {
        MockMvc mockMvc = mockMvc();
        String postId = createPost();

        mockMvc.perform(put("/api/posts/" + postId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + (Long.parseLong(postId) + 1)
                                + ",\"title\":\"T\",\"text\":\"X\",\"tags\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/posts/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/api/posts/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentErrors() throws Exception {
        MockMvc mockMvc = mockMvc();
        String postId = createPost();

        // empty text
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \",\"postId\":" + postId + "}"))
                .andExpect(status().isBadRequest());
        // text over 2000
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + "c".repeat(2001) + "\",\"postId\":" + postId + "}"))
                .andExpect(status().isBadRequest());
        // postId mismatch
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hi\",\"postId\":" + (Long.parseLong(postId) + 1) + "}"))
                .andExpect(status().isBadRequest());
        // comment to missing post (no raw FK 500 — clean 404)
        mockMvc.perform(post("/api/posts/999999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hi\",\"postId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        // edit/delete missing comment
        mockMvc.perform(put("/api/posts/" + postId + "/comments/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999999,\"text\":\"Hi\",\"postId\":" + postId + "}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/posts/" + postId + "/comments/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void imageErrors() throws Exception {
        MockMvc mockMvc = mockMvc();
        String postId = createPost();

        // no image uploaded yet
        mockMvc.perform(get("/api/posts/" + postId + "/image"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        // empty file
        MockMultipartFile empty = new MockMultipartFile("image", "e.jpg", "image/jpeg", new byte[0]);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/" + postId + "/image")
                        .file(empty)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());
        // missing multipart part
        MockMultipartFile other = new MockMultipartFile("other", "o.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/" + postId + "/image")
                        .file(other)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        // oversized file -> 413 (service-level check; MockMvc has no container limits)
        MockMultipartFile big = new MockMultipartFile("image", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/" + postId + "/image")
                        .file(big)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isPayloadTooLarge());
        // image for missing post
        MockMultipartFile file = new MockMultipartFile("image", "p.jpg", "image/jpeg", new byte[]{1, 2});
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/999999/image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNotFound());
    }
}
