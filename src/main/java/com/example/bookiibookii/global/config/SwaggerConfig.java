package com.example.bookiibookii.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile("!v1")
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swagger() {
        Info info = new Info()
                .title("부키부키 V1 API 명세서")
                .description("부키부키 V1 백엔드 API 명세서입니다")
                .version("0.1.0");

        String securityScheme = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityScheme);

        Components components = new Components()
                .addSecuritySchemes(securityScheme, new SecurityScheme()
                        .name(securityScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .components(components)
                .info(info)
                .addSecurityItem(securityRequirement)
                .tags(orderedTags());
    }

    @Bean
    public GroupedOpenApi tempApi() {
        return GroupedOpenApi.builder()
                .group("temp API")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(openApi -> openApi.setTags(orderedTags()))
                .build();
    }

    private List<Tag> orderedTags() {
        return List.of(
                new Tag().name("Group").description("그룹 관련 API"),
                new Tag().name("Application").description("그룹 신청 관련 API"),
                new Tag().name("Tracker").description("도서 트래킹 관련 API"),
                new Tag().name("BookReview").description("책 리뷰 및 독서카드 관련 API"),
                new Tag().name("DirectExchange").description("직접 교환 관련 API"),
                new Tag().name("DeliveryExchange").description("택배 교환 관련 API"),
                new Tag().name("Inquiry").description("문의 관련 API"),
                new Tag().name("Comment").description("댓글 관련 API"),
                new Tag().name("Notification").description("알림 관련 API"),
                new Tag().name("Report").description("신고 관련 API"),
                new Tag().name("Location").description("주소지 관련 API"),
                new Tag().name("Notice").description("공지 관련 API"),
                new Tag().name("Terms").description("약관 관련 API"),
                new Tag().name("Admin").description("관리자용 API")
        );
    }
}
