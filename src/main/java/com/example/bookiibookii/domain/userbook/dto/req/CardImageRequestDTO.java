package com.example.bookiibookii.domain.userbook.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardImageRequestDTO {
    @NotBlank(message = "S3 키는 필수입니다.")
    private String s3Key;
}
