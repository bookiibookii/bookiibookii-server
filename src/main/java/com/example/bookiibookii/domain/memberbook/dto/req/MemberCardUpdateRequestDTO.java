package com.example.bookiibookii.domain.memberbook.dto.req;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberCardUpdateRequestDTO {

    @Positive(message = "페이지는 양수여야 합니다.")
    private Integer page;

    @Size(max = 110, message = "메모는 최대 110자까지 입력 가능합니다.")
    private String memo;

    @Size(max = 140, message = "인용문은 최대 140자까지 입력 가능합니다.")
    private String quotation;

    /** IMAGE 타입 카드 이미지 변경 시 (null이면 미변경). 생성용 presigned URL로 업로드한 s3Key 사용 */
    private String s3Key;
}
