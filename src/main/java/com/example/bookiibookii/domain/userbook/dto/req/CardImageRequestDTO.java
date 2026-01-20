package com.example.bookiibookii.domain.userbook.dto.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CardImageRequestDTO {
    @NotEmpty(message = "S3 키 목록은 필수입니다.")
    private List<String> s3Keys;
}
