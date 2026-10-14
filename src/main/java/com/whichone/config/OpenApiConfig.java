package com.whichone.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

@Configuration
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    @Bean
    OpenAPI whichOneApi() {
        return new OpenAPI().info(new Info().title("WhichOne API").version("1.0.0")
                .description("Create drafts, publish immutable quizzes, submit answers, and view your own statistics. "
                        + "Educational scores and personality outcomes are calculated on the server."));
    }
}
