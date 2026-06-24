package com.example.bookiibookii.domain.memberbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponseDTO {
    private String s3Key;
    private String presignedPutUrl;
}
