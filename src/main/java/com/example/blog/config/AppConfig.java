package com.example.blog.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Root application configuration, assembled from focused configs:
 * web (MVC/CORS), persistence (DataSource/transactions) and DB bootstrap.
 * Bootstrapped by {@link WebAppInitializer}.
 */
@Configuration
@Import({WebMvcConfig.class, PersistenceConfig.class, DbInitConfig.class})
@ComponentScan(
        basePackages = "com.example.blog",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.example\\.blog\\.config\\..*")
)
public class AppConfig {
}
