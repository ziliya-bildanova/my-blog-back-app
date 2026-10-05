package com.example.blog;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all full-context Spring Boot tests: one shared
 * ApplicationContext (Spring context caching), one isolated H2 database.
 * Each test runs in a transaction that rolls back,
 * so tests never see each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseSpringTest {
}
