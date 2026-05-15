package com.example.bookiibookii.domain.memberbook.dto.req;

import com.example.bookiibookii.domain.memberbook.enums.CardType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberCardCreateRequestDTO {

    @NotNull(message = "카드 타입은 필수입니다.")
    private CardType cardType;

    @Size(max = 140, message = "인용문은 최대 140자까지 입력 가능합니다.")
    private String quotation;

    private String s3Key;

    @Positive(message = "페이지는 양수여야 합니다.")
    private Integer page;

    @Size(max = 110, message = "메모는 최대 110자까지 입력 가능합니다.")
    private String memo;

    @AssertTrue(message = "TEXT 타입 독서카드는 quotation이 필수입니다.")
    public boolean isQuotationValidForText() {
        if (cardType != CardType.TEXT) {
            return true;
        }
        return quotation != null && !quotation.isBlank();
    }

    @AssertTrue(message = "IMAGE 타입 독서카드는 s3Key가 필수입니다.")
    public boolean isS3KeyValidForImage() {
        if (cardType != CardType.IMAGE) {
            return true;
        }
        return s3Key != null && !s3Key.isBlank();
    }
}
