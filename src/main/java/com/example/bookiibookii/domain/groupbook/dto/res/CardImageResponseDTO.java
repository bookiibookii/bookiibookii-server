package com.example.bookiibookii.domain.groupbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardImageResponseDTO {
    private Long cardImageId;
    private String s3Key;
    private String presignedGetUrl;
}
