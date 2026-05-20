package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.DeliveryAddressUpdateRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.DeliveryRegisterRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.DeliveryAddressResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.PartnerDeliveryResponseDTO;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerSuccessCode;
import com.example.bookiibookii.domain.tracker.service.PackageDeliveryService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/deliveries")
@RequiredArgsConstructor
public class PackageDeliveryController implements PackageDeliveryControllerDocs {

    private final PackageDeliveryService packageDeliveryService;

    @GetMapping("/address")
    public ApiResponse<DeliveryAddressResponseDTO> getAddresses(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.DELIVERY_ADDRESS_GET_OK,
                packageDeliveryService.getAddresses(groupId, user)
        );
    }

    @PatchMapping("/address/me")
    public ApiResponse<DeliveryAddressResponseDTO> updateMyAddress(
            @PathVariable Long groupId,
            @RequestBody @Valid DeliveryAddressUpdateRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.DELIVERY_ADDRESS_UPDATE_OK,
                packageDeliveryService.updateMyAddress(groupId, request, user)
        );
    }

    @PostMapping
    public ApiResponse<Void> registerDelivery(
            @PathVariable Long groupId,
            @RequestBody @Valid DeliveryRegisterRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        packageDeliveryService.registerDelivery(groupId, request, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.DELIVERY_REGISTER_OK, null);
    }

    @GetMapping("/partner")
    public ApiResponse<PartnerDeliveryResponseDTO> getPartnerDelivery(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.PARTNER_DELIVERY_GET_OK,
                packageDeliveryService.getPartnerDelivery(groupId, user)
        );
    }

    @PatchMapping("/partner/receive")
    public ApiResponse<Void> confirmPartnerDeliveryReceived(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        packageDeliveryService.confirmPartnerDeliveryReceived(groupId, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.DELIVERY_RECEIVE_OK, null);
    }
}
