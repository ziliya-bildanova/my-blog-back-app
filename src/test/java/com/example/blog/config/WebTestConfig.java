package com.example.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.context.annotation.Bean;

/**
 * Full web test configuration: {@link TestConfig} plus Spring MVC
 * infrastructure. Used only by MockMvc integration tests
 * (with {@code @WebAppConfiguration}, so a ServletContext exists).
 */
@Configuration
@EnableWebMvc
@Import(TestConfig.class)
public class WebTestConfig {

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
