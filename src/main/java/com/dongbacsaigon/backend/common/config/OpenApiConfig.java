package com.dongbacsaigon.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI dongBacOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dong Bac Sai Gon API")
                        .description("Backend API for Dong Bac Sai Gon public website and management system.")
                        .version("v1"));
    }
}
