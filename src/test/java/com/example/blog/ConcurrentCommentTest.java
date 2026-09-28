package com.example.blog;

import com.example.blog.dto.CreatePostRequest;
import com.example.blog.dto.PostDto;
import com.example.blog.exception.NotFoundException;
import com.example.blog.service.CommentService;
import com.example.blog.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A post deleted concurrently with a comment insert must never produce
 * a raw FK violation (500): the insert either wins or cleanly reports 404.
 * Runs outside a test transaction so worker threads see committed data.
 * Shares the cached Spring context.
 */
class ConcurrentCommentTest extends BaseSpringTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDeleteAndCommentInsert() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Race post " + System.nanoTime());
        request.setText("Text");
        request.setTags(List.of());
        PostDto post = postService.createPost(request);
        long postId = post.getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        try {
            Future<?> deleter = pool.submit(() -> {
                await(start);
                try {
                    postService.deletePost(postId);
                } catch (NotFoundException alreadyGone) {
                    // Insert won and cleanup deleted it: fine.
                }
            });
            Future<?> inserter = pool.submit(() -> {
                await(start);
                try {
                    com.example.blog.dto.CreateCommentRequest comment =
                            new com.example.blog.dto.CreateCommentRequest();
                    comment.setText("Race comment");
                    comment.setPostId(postId);
                    commentService.createComment(postId, comment);
                } catch (NotFoundException postGone) {
                    // Delete won: clean 404, the fixed behavior.
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            });
            start.countDown();
            deleter.get(15, TimeUnit.SECONDS);
            inserter.get(15, TimeUnit.SECONDS);
            assertThat(unexpected).isEmpty();
        } finally {
            try {
                postService.deletePost(postId);
            } catch (NotFoundException alreadyGone) {
                // Already deleted by the race: nothing to clean.
            }
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
