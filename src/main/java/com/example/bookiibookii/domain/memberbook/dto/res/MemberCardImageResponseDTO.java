package com.example.bookiibookii.domain.memberbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberCardImageResponseDTO {
    private Long cardImageId;
    private String s3Key;
    private String presignedGetUrl;
}
