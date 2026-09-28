package com.example.blog.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Bootstraps the Spring application in any Servlet 5+/6 container
 * (Tomcat 10.1, Jetty 12). DispatcherServlet is mapped to "/".
 * Multipart limits mirror {@link com.example.blog.service.PostServiceImpl#MAX_IMAGE_SIZE}.
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** Max uploaded file: 5 MB. */
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    /** Max whole multipart request: 6 MB (file + overhead). */
    public static final long MAX_REQUEST_SIZE = 6L * 1024 * 1024;
    /** Files larger than 1 MB are spooled to disk instead of memory. */
    public static final int FILE_SIZE_THRESHOLD = 1024 * 1024;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{AppConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"),
                MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD));
    }
}
