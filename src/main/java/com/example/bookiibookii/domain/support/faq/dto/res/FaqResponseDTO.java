package com.example.bookiibookii.domain.support.faq.dto.res;

import java.time.Instant;

public class FaqResponseDTO {

    public record FaqListDTO(
            Long id,
            String question,
            String answer,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record FaqItemDTO(
            String question,
            String answer
    ) {}
}
