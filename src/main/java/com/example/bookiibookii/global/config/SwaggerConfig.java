package com.example.bookiibookii.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swagger() {
        Info info = new Info()
                .title("부키부키 API 명세서")
                .description("부키부키 임시 백엔드 API 명세서입니다.")
                .version("0.0.1");

        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
    @Bean
    public GroupedOpenApi tempApi() {
        return GroupedOpenApi.builder()
                .group("temp API")
                .pathsToMatch("/api/**")
                .build();
    }
}
