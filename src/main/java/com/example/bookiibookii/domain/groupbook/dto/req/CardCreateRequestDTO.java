package com.example.bookiibookii.domain.groupbook.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardCreateRequestDTO {
    @NotBlank(message = "S3 키는 필수입니다.")
    private String s3Key;

    @NotNull(message = "페이지 정보는 필수입니다.")
    @Positive(message = "페이지는 양수여야 합니다.")
    private Integer page;

    @Size(max = 500, message = "메모는 최대 500글자까지 입력 가능합니다.")
    private String memo;
}
