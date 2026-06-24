package com.example.bookiibookii.domain.memberbook.dto.req;

import com.example.bookiibookii.domain.memberbook.enums.ShareLayout;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "독서카드 공유 토큰 발급 요청")
public class ShareTokenCreateRequestDTO {

    @NotNull(message = "공유 레이아웃은 필수입니다.")
    @Schema(description = "링크 공유 웹 렌더링 레이아웃", example = "OVERLAY", allowableValues = {"OVERLAY", "SPLIT"})
    private ShareLayout shareLayout;
}
