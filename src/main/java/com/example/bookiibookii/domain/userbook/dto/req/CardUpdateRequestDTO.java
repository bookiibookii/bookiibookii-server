package com.example.bookiibookii.domain.userbook.dto.req;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardUpdateRequestDTO {
    /** 변경할 페이지 (null이면 미변경, 양수만 허용) */
    @Positive(message = "페이지는 양수여야 합니다.")
    private Integer page;

    /** 변경할 메모 (null이면 미변경, 최대 500자) */
    @Size(max = 500, message = "메모는 최대 500글자까지 입력 가능합니다.")
    private String memo;

    /** 변경할 카드 이미지 S3 키 (null이면 미변경, presigned URL로 업로드 후 사용) */
    private String s3Key;
}
