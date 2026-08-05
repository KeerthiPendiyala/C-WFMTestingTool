package com.ukgqtm.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI ukgQaTestManagementOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("entraBearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("UKG QA Test Management API")
                        .version("0.2.0")
                        .description("Versioned /api/v1 REST foundation. Future workflow groups are contract-planned until implemented."));
    }
}
