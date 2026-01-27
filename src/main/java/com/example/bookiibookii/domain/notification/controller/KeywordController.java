package com.example.bookiibookii.domain.notification.controller;

import com.example.bookiibookii.domain.notification.dto.KeywordReqDTO;
import com.example.bookiibookii.domain.notification.dto.KeywordResDTO;
import com.example.bookiibookii.domain.notification.enums.KeywordSort;
import com.example.bookiibookii.domain.notification.exception.code.KeywordSuccessCode;
import com.example.bookiibookii.domain.notification.service.KeywordService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class KeywordController implements KeywordControllerDocs{

    private final KeywordService keywordService;

    @GetMapping("/api/keywords")
    public ApiResponse<KeywordResDTO.KeywordList> getKeywordList(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam(defaultValue = "LATEST") KeywordSort sort
    ){
        KeywordResDTO.KeywordList result = keywordService.getMyKeywords(user, sort);
        return ApiResponse.onSuccess(KeywordSuccessCode.KEYWORD_LIST_OK, result);
    }

    @PostMapping("/api/keywords")
    public ApiResponse<KeywordResDTO.KeywordItem> saveKeyword(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid KeywordReqDTO.SaveKeyword saveReq
    ){
        KeywordResDTO.KeywordItem result = keywordService.saveKeyword(user, saveReq);
        return ApiResponse.onSuccess(KeywordSuccessCode.KEYWORD_CREATE_OK, result);
    }

    @DeleteMapping("/api/keywords/{keywordId}")
    public ApiResponse<Void> deleteKeyword(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long keywordId
    ){
        keywordService.deleteKeyword(user, keywordId);
        return ApiResponse.onSuccess(KeywordSuccessCode.KEYWORD_DELETE_OK, null);
    }
}
