package com.example.bookiibookii.domain.groupbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponseDTO {
    private String s3Key;
    private String presignedPutUrl;
}
