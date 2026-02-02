package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.req.CardCreateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.req.CardUpdateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardListResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Card", description = "독서카드 관련 API")
public interface CardControllerDocs {

    @Operation(
            summary = "카드 생성 전 이미지 업로드용 Presigned URL 발급",
            description = """
            카드 생성 전에 이미지를 업로드하기 위한 presigned URL을 발급합니다.
            
            - UUID 기반 s3Key를 생성하여 presigned URL을 발급합니다.
            - 발급된 presignedPutUrl로 PUT 요청 시 클라이언트에서 직접 S3에 이미지를 업로드할 수 있습니다.
            - URL은 10분간 유효합니다.
            - 업로드 완료 후 받은 s3Key를 카드 생성 API에 전달해야 합니다.
            - s3Key 형식: image/cards/{uuid}
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "URL 발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 책을 찾을 수 없음"
            )
    })
    @PostMapping("/{userBookId}/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "사용자 책 식별자(ID)", example = "1")
            @PathVariable Long userBookId
    );

    @Operation(
            summary = "독서카드 생성",
            description = """
            독서카드를 생성합니다.
            
            - 이미지와 페이지 정보는 필수값입니다.
            - 메모는 선택값이며, 최대 500글자까지 입력 가능합니다.
            - 이미지는 S3에 이미 업로드된 상태여야 하며, s3Key를 전달해야 합니다.
            - 카드 생성 전 이미지 업로드를 위해 presigned URL을 발급받을 수 있습니다.
            - 카드 생성 시 S3에 이미지가 실제로 존재하는지 확인합니다 (HEAD 요청).
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "독서카드 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수값 누락, 메모 길이 초과, 중복된 S3 키, S3에 이미지 없음)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 책을 찾을 수 없음 (존재하지 않거나 소유권이 없음)"
            )
    })
    @PostMapping("/{userBookId}")
    ApiResponse<CardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "사용자 책 식별자(ID)", example = "1")
            @PathVariable Long userBookId,
            @Valid @RequestBody CardCreateRequestDTO request
    );

    @Operation(
            summary = "독서카드 목록 조회",
            description = """
            사용자 책(userBook)에 속한 독서카드 목록을 조회합니다.
            
            - UserBook 소유자이거나 같은 그룹의 멤버인 경우에만 조회 가능합니다.
            - 생성일 기준 오름차순으로 정렬된 카드 목록을 반환합니다.
            - 각 카드에는 카드 이미지(cardImageId, s3Key, presignedGetUrl), 카드 생성일(createdAt)이 포함됩니다.
            - 응답에 책 제목(title), 그룹 ID(groupId)가 포함됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "독서카드 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 책을 찾을 수 없음 (존재하지 않거나 접근 권한 없음)"
            )
    })
    @GetMapping("/{userBookId}")
    ApiResponse<CardListResponseDTO> getCards(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "사용자 책 식별자(ID)", example = "1")
            @PathVariable Long userBookId
    );

    @Operation(
            summary = "독서카드 수정",
            description = """
            독서카드를 수정합니다. 전달한 필드만 변경됩니다 (부분 수정).
            
            - page, memo, s3Key 모두 선택값입니다. 보내지 않은 필드는 변경되지 않습니다.
            - 카드 소유자(UserBook 소유자)만 수정 가능합니다.
            - 이미지(s3Key) 변경 시: `/api/cards/{cardId}/images/presigned-url`로 Presigned URL 발급 후 S3 업로드하고, 여기서 s3Key를 전달하세요.
            - s3Key 형식 검증 및 S3 존재 여부 확인이 수행됩니다.
            - 응답은 수정된 카드 정보(CardCreateResponseDTO)와 동일한 형태입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "독서카드 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (메모 길이 초과, 유효하지 않은 S3 키, 중복 S3 키 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카드를 찾을 수 없음 (존재하지 않거나 수정 권한 없음)"
            )
    })
    @PatchMapping("/{cardId}")
    ApiResponse<CardCreateResponseDTO> updateCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "카드 식별자(ID)", example = "1")
            @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateRequestDTO request
    );
}
