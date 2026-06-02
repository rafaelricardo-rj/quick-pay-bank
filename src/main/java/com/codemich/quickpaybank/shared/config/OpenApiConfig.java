package com.codemich.quickpaybank.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
            .title("Quick Pay Bank API")
            .description("REST API for simulating digital bank operations. It allows account management, transfers between customers, and statement inquiries.")
            .version("1.0.0"));
    }
}
