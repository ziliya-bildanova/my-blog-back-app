package com.example.blog.dao;

import com.example.blog.config.TestConfig;
import com.example.blog.exception.NotFoundException;
import com.example.blog.model.ImageData;
import com.example.blog.model.Post;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAO tests against embedded H2. Shares the cached Spring context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
class PostDaoTest {

    @Autowired
    private PostDao postDao;

    @Autowired
    private CommentDao commentDao;

    @Test
    void insertAndFindById() {
        long id = postDao.insert("Title", "Text", List.of("tag_1", "tag_2"));

        Optional<Post> found = postDao.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Title");
        assertThat(found.get().getTags()).containsExactlyInAnyOrder("tag_1", "tag_2");
        assertThat(found.get().getLikesCount()).isZero();
        assertThat(found.get().getCommentsCount()).isZero();
    }

    @Test
    void update() {
        long id = postDao.insert("Old", "Old text", List.of("old"));

        postDao.update(id, "New", "New text", List.of("new1", "new2"));

        Post post = postDao.findById(id).orElseThrow();
        assertThat(post.getTitle()).isEqualTo("New");
        assertThat(post.getTags()).containsExactlyInAnyOrder("new1", "new2");
    }

    @Test
    void deleteRemovesPostTagsAndComments() {
        long id = postDao.insert("Title", "Text", List.of("t"));
        commentDao.insert(id, "comment");

        postDao.delete(id);

        assertThat(postDao.findById(id)).isEmpty();
        assertThatThrownBy(() -> postDao.delete(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchCountAndPagination() {
        postDao.insert("Lalala post", "body", List.of());
        postDao.insert("Other", "Lalala inside", List.of());
        postDao.insert("Tagged", "body", List.of("Lalala"));
        postDao.insert("Unrelated", "nothing", List.of());

        assertThat(postDao.count("Lalala")).isEqualTo(3);
        assertThat(postDao.count("")).isEqualTo(4);

        List<Post> page = postDao.findAll("Lalala", 2, 0);
        assertThat(page).hasSize(2);
        List<Post> rest = postDao.findAll("Lalala", 2, 2);
        assertThat(rest).hasSize(1);
    }

    @Test
    void incrementLikes() {
        long id = postDao.insert("Title", "Text", List.of());

        assertThat(postDao.incrementLikes(id)).isEqualTo(1);
        assertThat(postDao.incrementLikes(id)).isEqualTo(2);
        assertThat(postDao.findById(id).orElseThrow().getLikesCount()).isEqualTo(2);
    }

    @Test
    void saveAndFindImage() {
        long id = postDao.insert("Title", "Text", List.of());
        assertThat(postDao.findImage(id)).isEmpty();

        byte[] bytes = {1, 2, 3};
        postDao.saveImage(id, bytes, "image/jpeg");

        Optional<ImageData> image = postDao.findImage(id);
        assertThat(image).isPresent();
        assertThat(image.get().getBytes()).isEqualTo(bytes);
        assertThat(image.get().getContentType()).isEqualTo("image/jpeg");
    }
}
