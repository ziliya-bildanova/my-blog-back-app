package com.example.blog;

import com.example.blog.config.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scenario through HTTP + real services + embedded H2:
 * create post, filter, read, update, like, comments, image, delete.
 * Shares the cached Spring context (same {@link TestConfig} as other tests).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WebTestConfig.class)
@WebAppConfiguration
@Transactional
class BlogIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void fullScenario() throws Exception {
        MockMvc mockMvc = mockMvc();

        // create
        String createBody = "{\"title\":\"Lalala post\",\"text\":\"Some **markdown** text\",\"tags\":[\"tag_1\"]}";
        String created = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0))
                .andReturn().getResponse().getContentAsString();
        long postId = Long.parseLong(created.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        // list with search + pagination
        mockMvc.perform(get("/api/posts")
                        .param("search", "Lalala").param("pageNumber", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].id").value(postId))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));

        // read single
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lalala post"));

        // update
        String updateBody = "{\"id\":" + postId + ",\"title\":\"Edited\",\"text\":\"New text\",\"tags\":[]}";
        mockMvc.perform(put("/api/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited"));

        // likes
        mockMvc.perform(post("/api/posts/" + postId + "/likes"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // comments
        String commentBody = "{\"text\":\"Nice!\",\"postId\":" + postId + "}";
        String commentCreated = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andReturn().getResponse().getContentAsString();
        long commentId = Long.parseLong(commentCreated.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId));
        mockMvc.perform(get("/api/posts/" + postId + "/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Nice!"));

        String commentUpdate = "{\"id\":" + commentId + ",\"text\":\"Even better\",\"postId\":" + postId + "}";
        mockMvc.perform(put("/api/posts/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Even better"));

        // image upload + download
        MockMultipartFile image = new MockMultipartFile("image", "pic.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/" + postId + "/image")
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/posts/" + postId + "/image"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));

        // delete comment + post (cascades)
        mockMvc.perform(delete("/api/posts/" + postId + "/comments/" + commentId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/posts/" + postId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isNotFound());
    }
}
