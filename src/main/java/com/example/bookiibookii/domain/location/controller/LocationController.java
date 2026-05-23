package com.example.bookiibookii.domain.location.controller;

import com.example.bookiibookii.domain.location.dto.req.UserDeliveryReqDTO;
import com.example.bookiibookii.domain.location.dto.req.UserExchangeReqDTO;
import com.example.bookiibookii.domain.location.dto.res.UserDeliveryResDTO;
import com.example.bookiibookii.domain.location.dto.res.UserExchangeResDTO;
import com.example.bookiibookii.domain.location.exception.code.LocationSuccessCode;
import com.example.bookiibookii.domain.location.service.UserDeliveryService;
import com.example.bookiibookii.domain.location.service.UserExchangeService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController implements LocationControllerDocs {

    private final UserDeliveryService userDeliveryService;
    private final UserExchangeService userExchangeService;

    @Override
    @GetMapping("/api/mypage/addresses/deliveries")
    public ApiResponse<List<UserDeliveryResDTO.UserDeliveryDto>> getDeliveries(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(LocationSuccessCode.GET_DELIVERIES_SUCCESS,
                userDeliveryService.getMyDeliveries(user.getId()));
    }

    @Override
    @PostMapping("/api/mypage/addresses/deliveries")
    public ApiResponse<Void> addDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserDeliveryReqDTO.AddReqDTO request
    ) {
        userDeliveryService.addDelivery(user.getId(), request);
        return ApiResponse.onSuccess(LocationSuccessCode.ADD_DELIVERY_SUCCESS, null);
    }

    @Override
    @PutMapping("/api/mypage/addresses/deliveries/{userDeliveryId}")
    public ApiResponse<Void> updateDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userDeliveryId,
            @Valid @RequestBody UserDeliveryReqDTO.AddReqDTO request
    ) {
        userDeliveryService.updateDelivery(user.getId(), userDeliveryId, request);
        return ApiResponse.onSuccess(LocationSuccessCode.UPDATE_DELIVERY_SUCCESS, null);
    }

    @Override
    @DeleteMapping("/api/mypage/addresses/deliveries/{userDeliveryId}")
    public ApiResponse<Void> deleteDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userDeliveryId
    ) {
        userDeliveryService.deleteDelivery(user.getId(), userDeliveryId);
        return ApiResponse.onSuccess(LocationSuccessCode.DELETE_DELIVERY_SUCCESS, null);
    }

    @Override
    @GetMapping("/api/mypage/addresses/exchanges")
    public ApiResponse<List<UserExchangeResDTO.UserExchangeDto>> getExchanges(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(LocationSuccessCode.GET_EXCHANGES_SUCCESS,
                userExchangeService.getMyExchanges(user.getId()));
    }

    @Override
    @PostMapping("/api/mypage/addresses/exchanges")
    public ApiResponse<Void> addExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserExchangeReqDTO.AddReqDTO request
    ) {
        userExchangeService.addExchange(user.getId(), request);
        return ApiResponse.onSuccess(LocationSuccessCode.ADD_EXCHANGE_SUCCESS, null);
    }

    @Override
    @PutMapping("/api/mypage/addresses/exchanges/{userExchangeId}")
    public ApiResponse<Void> updateExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userExchangeId,
            @Valid @RequestBody UserExchangeReqDTO.AddReqDTO request
    ) {
        userExchangeService.updateExchange(user.getId(), userExchangeId, request);
        return ApiResponse.onSuccess(LocationSuccessCode.UPDATE_EXCHANGE_SUCCESS, null);
    }

    @Override
    @DeleteMapping("/api/mypage/addresses/exchanges/{userExchangeId}")
    public ApiResponse<Void> deleteExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userExchangeId
    ) {
        userExchangeService.deleteExchange(user.getId(), userExchangeId);
        return ApiResponse.onSuccess(LocationSuccessCode.DELETE_EXCHANGE_SUCCESS, null);
    }

    @Override
    @PatchMapping("/api/mypage/addresses/deliveries/{userDeliveryId}/default")
    public ApiResponse<Void> setDefaultDelivery(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userDeliveryId
    ) {
        userDeliveryService.setDefaultDelivery(user.getId(), userDeliveryId);
        return ApiResponse.onSuccess(LocationSuccessCode.SET_DEFAULT_DELIVERY_SUCCESS, null);
    }

    @Override
    @PatchMapping("/api/mypage/addresses/exchanges/{userExchangeId}/default")
    public ApiResponse<Void> setDefaultExchange(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userExchangeId
    ) {
        userExchangeService.setDefaultExchange(user.getId(), userExchangeId);
        return ApiResponse.onSuccess(LocationSuccessCode.SET_DEFAULT_EXCHANGE_SUCCESS, null);
    }
}
