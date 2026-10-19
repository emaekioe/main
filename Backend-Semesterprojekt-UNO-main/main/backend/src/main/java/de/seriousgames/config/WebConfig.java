package de.seriousgames.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig {

    @Value("${app.deck-image-upload-dir}")
    private String uploadDir;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Error-Message");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String resourcePath = Path.of(uploadDir)
                        .toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString();

                registry.addResourceHandler("/deck-images/**")
                        .addResourceLocations(resourcePath);
            }
        };
    }
}