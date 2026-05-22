package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.DeliveryAddressUpdateRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.DeliveryRegisterRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.DeliveryAddressResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.PartnerDeliveryResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Package Delivery", description = "택배 교환 배송지 및 운송장 API")
@RequestMapping("/api/groups/{groupId}/deliveries")
public interface PackageDeliveryControllerDocs {

    @GetMapping("/address")
    @Operation(
            summary = "배송 정보 확인",
            description = """
            현재 교환 라운드의 내 배송지와 파트너 배송지를 조회합니다.
            
            - 대표 배송지를 직접 조회하지 않고 현재 교환의 배송지 정보만 조회합니다.
            - 상대방이 이미 나에게 운송장을 등록하면 내 배송지 수정이 불가합니다.
            - 내가 이미 운송장을 등록해도 내 배송지 수정이 불가합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송 정보 조회 성공", content = @Content(schema = @Schema(implementation = DeliveryAddressResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "택배교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송지 정보를 찾을 수 없음")
    })
    ApiResponse<DeliveryAddressResponseDTO> getAddresses(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/address/me")
    @Operation(
            summary = "내 배송지 수정",
            description = """
            현재 교환의 내 배송지 정보만 수정합니다.
            
            - 대표 배송지를 수정하는 API가 아닙니다.
            - UserDelivery와 GroupPlace는 수정하지 않습니다.
            - 상대방이 이미 나에게 운송장을 등록하면 수정할 수 없습니다.
            - 내가 이미 운송장을 등록해도 수정할 수 없습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 배송지 수정 성공", content = @Content(schema = @Schema(implementation = DeliveryAddressResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "택배교환 그룹이 아니거나 수정 가능 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송지 정보를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 운송장에 사용된 배송지")
    })
    ApiResponse<DeliveryAddressResponseDTO> updateMyAddress(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid DeliveryAddressUpdateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PostMapping
    @Operation(
            summary = "운송장 등록",
            description = """
            내가 상대방에게 보낸 운송장을 등록합니다.
            
            - 운송장 등록 후 수정과 삭제는 불가합니다.
            - 등록 성공 시 내 교환 상태가 운송장 등록 완료로 변경됩니다.
            - 운송장 번호는 숫자만 입력할 수 있습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "운송장 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "택배교환 그룹이 아니거나 유효하지 않은 운송장 정보"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송지 정보를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 운송장을 등록함")
    })
    ApiResponse<Void> registerDelivery(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid DeliveryRegisterRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping("/partner")
    @Operation(summary = "상대방 운송장 확인", description = "상대방이 나에게 보낸 운송장 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상대방 운송장 조회 성공", content = @Content(schema = @Schema(implementation = PartnerDeliveryResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "택배교환 그룹이 아니거나 조회 가능 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상대방 운송장을 찾을 수 없음")
    })
    ApiResponse<PartnerDeliveryResponseDTO> getPartnerDelivery(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/partner/receive")
    @Operation(summary = "수령 인증", description = "상대방이 보낸 책을 받았다고 인증합니다. 두 명 모두 인증하면 다음 독서 단계로 전환됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수령 인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "택배교환 그룹이 아니거나 수령 인증 가능 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상대방 운송장을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 수령 인증됨")
    })
    ApiResponse<Void> confirmPartnerDeliveryReceived(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
