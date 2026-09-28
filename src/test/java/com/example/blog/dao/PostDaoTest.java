package com.example.blog.dao;

import com.example.blog.BaseSpringTest;
import com.example.blog.model.ImageData;
import com.example.blog.model.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAO tests against embedded H2. Shares the cached Spring context.
 */
class PostDaoTest extends BaseSpringTest {

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
    void duplicateTagsStoredOnce() {
        long id = postDao.insert("Title", "Text", List.of("t", "t", "t"));

        assertThat(postDao.findById(id).orElseThrow().getTags()).containsExactly("t");
    }

    @Test
    void update() {
        long id = postDao.insert("Old", "Old text", List.of("old"));

        assertThat(postDao.update(id, "New", "New text", List.of("new1", "new2"))).isTrue();

        Post post = postDao.findById(id).orElseThrow();
        assertThat(post.getTitle()).isEqualTo("New");
        assertThat(post.getTags()).containsExactlyInAnyOrder("new1", "new2");
        assertThat(postDao.update(999999L, "x", "y", List.of())).isFalse();
    }

    @Test
    void deleteRemovesPostTagsAndComments() {
        long id = postDao.insert("Title", "Text", List.of("t"));
        commentDao.insertIfPostExists(id, "comment");

        assertThat(postDao.delete(id)).isTrue();

        assertThat(postDao.findById(id)).isEmpty();
        assertThat(postDao.delete(id)).isFalse();
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
    void searchIsCaseInsensitive() {
        postDao.insert("LALALA post", "body", List.of());

        assertThat(postDao.count("lalala")).isEqualTo(1);
        assertThat(postDao.findAll("LaLaLa", 5, 0)).hasSize(1);
    }

    @Test
    void searchEscapesWildcards() {
        postDao.insert("Sale", "100% discount_today", List.of());
        postDao.insert("Other", "price 1000", List.of());

        // Literal % must not match "1000", literal _ must not match any char.
        assertThat(postDao.count("100%")).isEqualTo(1);
        assertThat(postDao.count("discount_today")).isEqualTo(1);
        assertThat(postDao.count("discountXtoda")).isEqualTo(0);
        assertThat(postDao.findAll("100%", 5, 0)).hasSize(1);
    }

    @Test
    void listReturnsPreviewNotFullText() {
        postDao.insert("Title", "x".repeat(200), List.of());

        Post post = postDao.findAll("", 5, 0).get(0);

        assertThat(post.getText()).hasSize(129).endsWith("…");

        // Detail query still returns the full text.
        assertThat(postDao.findById(post.getId()).orElseThrow().getText()).hasSize(200);
    }

    @Test
    void incrementLikes() {
        long id = postDao.insert("Title", "Text", List.of());

        assertThat(postDao.incrementLikes(id)).contains(1);
        assertThat(postDao.incrementLikes(id)).contains(2);
        assertThat(postDao.findById(id).orElseThrow().getLikesCount()).isEqualTo(2);
        assertThat(postDao.incrementLikes(999999L)).isEmpty();
    }

    @Test
    void saveAndFindImage() {
        long id = postDao.insert("Title", "Text", List.of());
        assertThat(postDao.findImage(id)).isEmpty();

        byte[] bytes = {1, 2, 3};
        assertThat(postDao.saveImage(id, bytes, "image/jpeg")).isTrue();

        Optional<ImageData> image = postDao.findImage(id);
        assertThat(image).isPresent();
        assertThat(image.get().getBytes()).isEqualTo(bytes);
        assertThat(image.get().getContentType()).isEqualTo("image/jpeg");
        assertThat(postDao.saveImage(999999L, bytes, "image/jpeg")).isFalse();
    }
}
