package com.example.blog;

import com.example.blog.config.TestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all Spring tests: same configuration, same cached
 * ApplicationContext, same isolated H2 database. Each test runs in a
 * transaction that rolls back, so tests never see each other's data.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
@Transactional
public abstract class BaseSpringTest {
}
