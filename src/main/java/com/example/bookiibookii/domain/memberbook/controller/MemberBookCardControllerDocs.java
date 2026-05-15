package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "MemberBook Card", description = "멤버북 기준 독서카드 API")
public interface MemberBookCardControllerDocs {

    @Operation(
            summary = "독서카드 생성 전 이미지 업로드용 Presigned URL 발급",
            description = """
            IMAGE 타입 독서카드 생성 전 S3 업로드용 presigned URL을 발급합니다.

            - 본인 MatchedMember에 연결된 memberBookId만 사용 가능합니다.
            - s3Key 형식: image/cards/{uuid}
            - URL은 10분간 유효합니다.
            - 업로드 완료 후 s3Key를 독서카드 생성 API body에 전달하세요.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "멤버북 없음 또는 소유권 없음")
    })
    @PostMapping("/{memberBookId}/cards/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "멤버북 식별자(ID)", example = "1")
            @PathVariable Long memberBookId
    );

    @Operation(
            summary = "멤버북 독서카드 생성",
            description = """
            memberBook 도메인 기준으로 독서카드를 생성합니다.

            - **엔드포인트**: `POST /api/member-books/{memberBookId}/cards`
              (기존 `POST /api/cards/{groupBookId}`와 경로 충돌을 피하기 위해 member-books 하위로 분리)
            - **TEXT**: quotation(필수, 최대 140자), memo(선택, 최대 110자), page(선택)
            - **IMAGE**: s3Key(필수, S3 업로드 완료), memo(선택), page(선택)
            - 본인 MatchedMember에 연결된 memberBookId만 생성 가능합니다.
            - 서재에서 제거된(removedAt 존재) 멤버북에는 생성할 수 없습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "독서카드 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수값 누락, 길이 초과, S3 검증 실패 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "멤버북 없음 또는 소유권 없음")
    })
    @PostMapping("/{memberBookId}/cards")
    ApiResponse<MemberCardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "멤버북 식별자(ID)", example = "1")
            @PathVariable Long memberBookId,
            @Valid @RequestBody MemberCardCreateRequestDTO request
    );
}
