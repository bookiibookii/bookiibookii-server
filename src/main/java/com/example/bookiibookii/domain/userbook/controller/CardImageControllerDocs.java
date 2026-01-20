package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.userbook.dto.req.CardImageRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "CardImage", description = "카드 이미지 관련 API (S3 업로드 및 조회)")
public interface CardImageControllerDocs {

    @Operation(
            summary = "Presigned PUT URL 발급",
            description = """
            S3에 이미지를 업로드하기 위한 presigned URL을 발급합니다.
            
            - 카드당 이미지는 하나만 저장됩니다.
            - 발급된 presigned URL을 사용하여 클라이언트에서 직접 S3에 이미지를 업로드할 수 있습니다.
            - URL은 10분간 유효합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "URL 발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카드를 찾을 수 없음"
            )
    })
    @PostMapping("/{cardId}/images/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrl(
            @Parameter(description = "카드 식별자(ID)", example = "1")
            @PathVariable Long cardId
    );

    @Operation(
            summary = "카드 이미지 DB 저장 또는 업데이트",
            description = """
            S3 업로드가 완료된 이미지를 DB에 저장합니다.
            
            - 카드에 이미 이미지가 있으면 업데이트됩니다.
            - s3Key는 presigned URL 발급 시 받은 값을 사용해야 합니다.
            - s3Key 형식 검증 및 cardId 일치 확인이 수행됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "이미지 저장/업데이트 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효하지 않은 S3 키 형식 또는 중복된 S3 키)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카드를 찾을 수 없음"
            )
    })
    @PostMapping("/{cardId}/images")
    ApiResponse<CardImageResponseDTO> saveCardImage(
            @Parameter(description = "카드 식별자(ID)", example = "1")
            @PathVariable Long cardId,
            @Valid @RequestBody CardImageRequestDTO request
    );

    @Operation(
            summary = "카드 이미지 조회",
            description = """
            특정 카드에 속한 이미지를 조회합니다.
            
            - presigned GET URL이 포함되어 있습니다.
            - 이미지가 없으면 null을 반환합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (이미지가 없으면 null 반환)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카드를 찾을 수 없음"
            )
    })
    @GetMapping("/{cardId}/images")
    ApiResponse<CardImageResponseDTO> getCardImage(
            @Parameter(description = "카드 식별자(ID)", example = "1")
            @PathVariable Long cardId
    );
}
