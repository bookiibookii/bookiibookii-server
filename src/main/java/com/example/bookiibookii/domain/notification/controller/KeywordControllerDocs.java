package com.example.bookiibookii.domain.notification.controller;

import com.example.bookiibookii.domain.notification.dto.KeywordReqDTO;
import com.example.bookiibookii.domain.notification.dto.KeywordResDTO;
import com.example.bookiibookii.domain.notification.enums.KeywordSort;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Keyword", description = "키워드 관련 API")
public interface KeywordControllerDocs {

    @Operation(
            summary = "내가 등록한 키워드 리스트 조회 API"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 조회 성공")
    })
    @GetMapping("/api/keywords")
    public ApiResponse<KeywordResDTO.KeywordList> getKeywordList(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam(defaultValue = "LATEST") KeywordSort sort
    );

    @Operation(
            summary = "키워드 등록 API"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 등록 성공")
    })
    @PostMapping("/api/keywords")
    public ApiResponse<KeywordResDTO.KeywordItem> saveKeyword(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid KeywordReqDTO.SaveKeyword saveReq
    );

    @Operation(
            summary = "키워드 삭제 API"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 삭제 성공")
    })
    @DeleteMapping("/api/keywords/{keywordId}")
    public ApiResponse<Void> deleteKeyword(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long keywordId
    );
}
