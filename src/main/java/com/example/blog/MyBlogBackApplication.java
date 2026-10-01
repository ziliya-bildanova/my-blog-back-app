package com.example.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Blog backend entry point. Runs as an executable jar
 * with an embedded servlet container (Tomcat by default).
 */
@SpringBootApplication
public class MyBlogBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyBlogBackApplication.class, args);
    }
}
