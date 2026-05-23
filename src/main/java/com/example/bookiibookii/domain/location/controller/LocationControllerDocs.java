package com.example.bookiibookii.domain.location.controller;

import com.example.bookiibookii.domain.location.dto.req.UserDeliveryReqDTO;
import com.example.bookiibookii.domain.location.dto.req.UserExchangeReqDTO;
import com.example.bookiibookii.domain.location.dto.res.UserDeliveryResDTO;
import com.example.bookiibookii.domain.location.dto.res.UserExchangeResDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Location", description = "주소지 관리 API")
public interface LocationControllerDocs {

    @Operation(summary = "배송지 목록 조회", description = "등록된 배송지 목록을 조회합니다. (최대 2개)")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송지 목록 조회 성공"))
    @GetMapping("/api/mypage/addresses/deliveries")
    ApiResponse<List<UserDeliveryResDTO.UserDeliveryDto>> getDeliveries(
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "배송지 추가", description = "배송지를 추가합니다. 최대 2개까지 등록 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송지 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "최대 2개 초과")
    })
    @PostMapping("/api/mypage/addresses/deliveries")
    ApiResponse<Void> addDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserDeliveryReqDTO.AddReqDTO request
    );

    @Operation(summary = "배송지 수정", description = "등록된 배송지 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송지 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음")
    })
    @PutMapping("/api/mypage/addresses/deliveries/{userDeliveryId}")
    ApiResponse<Void> updateDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userDeliveryId,
            @Valid @RequestBody UserDeliveryReqDTO.AddReqDTO request
    );

    @Operation(summary = "배송지 삭제", description = "등록된 배송지를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송지 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음")
    })
    @DeleteMapping("/api/mypage/addresses/deliveries/{userDeliveryId}")
    ApiResponse<Void> deleteDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userDeliveryId
    );

    @Operation(summary = "희망 교환 장소 목록 조회", description = "등록된 희망 교환 장소 목록을 조회합니다. (최대 2개)")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "희망 교환 장소 목록 조회 성공"))
    @GetMapping("/api/mypage/addresses/exchanges")
    ApiResponse<List<UserExchangeResDTO.UserExchangeDto>> getExchanges(
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "희망 교환 장소 추가", description = "희망 교환 장소를 추가합니다. 최대 2개까지 등록 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "희망 교환 장소 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "최대 2개 초과")
    })
    @PostMapping("/api/mypage/addresses/exchanges")
    ApiResponse<Void> addExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserExchangeReqDTO.AddReqDTO request
    );

    @Operation(summary = "희망 교환 장소 수정", description = "등록된 희망 교환 장소 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "희망 교환 장소 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "희망 교환 장소를 찾을 수 없음")
    })
    @PutMapping("/api/mypage/addresses/exchanges/{userExchangeId}")
    ApiResponse<Void> updateExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userExchangeId,
            @Valid @RequestBody UserExchangeReqDTO.AddReqDTO request
    );

    @Operation(summary = "희망 교환 장소 삭제", description = "등록된 희망 교환 장소를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "희망 교환 장소 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "희망 교환 장소를 찾을 수 없음")
    })
    @DeleteMapping("/api/mypage/addresses/exchanges/{userExchangeId}")
    ApiResponse<Void> deleteExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userExchangeId
    );

    @Operation(summary = "대표 배송지 설정", description = "선택한 배송지를 대표 배송지로 설정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대표 배송지 설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음")
    })
    @PatchMapping("/api/mypage/addresses/deliveries/{userDeliveryId}/default")
    ApiResponse<Void> setDefaultDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userDeliveryId
    );

    @Operation(summary = "대표 희망 교환 장소 설정", description = "선택한 희망 교환 장소를 대표로 설정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대표 희망 교환 장소 설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "희망 교환 장소를 찾을 수 없음")
    })
    @PatchMapping("/api/mypage/addresses/exchanges/{userExchangeId}/default")
    ApiResponse<Void> setDefaultExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userExchangeId
    );
}
