package com.ukgqtm.app.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class ApiWebConfiguration implements WebMvcConfigurer {
    private final ApiProperties apiProperties;

    public ApiWebConfiguration(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (apiProperties.getCors().getAllowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/api/v1/**")
                .allowedOrigins(apiProperties.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Correlation-Id", "Idempotency-Key", "If-Match")
                .exposedHeaders("X-Correlation-Id", "ETag", "Location", "Content-Disposition")
                .allowCredentials(false);
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonReadLimits() {
        return builder -> builder.postConfigurer(objectMapper -> objectMapper.getFactory()
                .setStreamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(64)
                        .maxNumberLength(128)
                        .maxStringLength(1_000_000)
                        .build()));
    }
}
