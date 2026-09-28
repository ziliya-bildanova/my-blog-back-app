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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scenario through HTTP + real services + embedded H2,
 * asserting every required response field.
 * Shares the cached Spring context.
 */
class BlogIntegrationTest extends BaseSpringTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void fullScenario() throws Exception {
        MockMvc mockMvc = mockMvc();

        // create: every required field
        String createBody = "{\"title\":\"Lalala post\",\"text\":\"Some **markdown** text\","
                + "\"tags\":[\"tag_1\",\"old_tag\"]}";
        String created = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Lalala post"))
                .andExpect(jsonPath("$.text").value("Some **markdown** text"))
                .andExpect(jsonPath("$.tags[0]").value("old_tag"))
                .andExpect(jsonPath("$.tags[1]").value("tag_1"))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0))
                .andReturn().getResponse().getContentAsString();
        long postId = Long.parseLong(created.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        // list with search + pagination: every page field + every post field
        mockMvc.perform(get("/api/posts")
                        .param("search", "Lalala").param("pageNumber", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].id").value(postId))
                .andExpect(jsonPath("$.posts[0].title").value("Lalala post"))
                .andExpect(jsonPath("$.posts[0].text").value("Some **markdown** text"))
                .andExpect(jsonPath("$.posts[0].tags[0]").value("old_tag"))
                .andExpect(jsonPath("$.posts[0].tags[1]").value("tag_1"))
                .andExpect(jsonPath("$.posts[0].likesCount").value(0))
                .andExpect(jsonPath("$.posts[0].commentsCount").value(0))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));

        // read single
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lalala post"))
                .andExpect(jsonPath("$.tags.length()").value(2));

        // update replaces tags: old tag gone, new tag present
        String updateBody = "{\"id\":" + postId
                + ",\"title\":\"Edited\",\"text\":\"New text\",\"tags\":[\"new_tag\"]}";
        mockMvc.perform(put("/api/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited"))
                .andExpect(jsonPath("$.tags[0]").value("new_tag"))
                .andExpect(jsonPath("$.tags.length()").value(1));
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("new_tag"))
                .andExpect(jsonPath("$.tags.length()").value(1));

        // likes: plain number in body, then reflected in the post
        mockMvc.perform(post("/api/posts/" + postId + "/likes"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));

        // comments
        String commentBody = "{\"text\":\"Nice!\",\"postId\":" + postId + "}";
        String commentCreated = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON).content(commentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").value("Nice!"))
                .andExpect(jsonPath("$.postId").value(postId))
                .andReturn().getResponse().getContentAsString();
        long commentId = Long.parseLong(commentCreated.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId))
                .andExpect(jsonPath("$[0].text").value("Nice!"))
                .andExpect(jsonPath("$[0].postId").value(postId));
        mockMvc.perform(get("/api/posts/" + postId + "/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Nice!"))
                .andExpect(jsonPath("$.postId").value(postId));

        String commentUpdate = "{\"id\":" + commentId + ",\"text\":\"Even better\",\"postId\":" + postId + "}";
        mockMvc.perform(put("/api/posts/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON).content(commentUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Even better"))
                .andExpect(jsonPath("$.postId").value(postId));

        // post now counts the comment
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsCount").value(1));

        // image upload + download with content type
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
                .andExpect(content().contentType("image/jpeg"))
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
