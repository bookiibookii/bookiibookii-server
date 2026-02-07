package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.dto.res.MatchedMemberResponseDTO;
import com.example.bookiibookii.domain.group.service.MatchedMemberService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/together/members")
public class MatchedMemberController implements MatchedMemberControllerDocs {

    private final MatchedMemberService matchedMemberService;

    @Override // @Override 어노테이션 추가
    @PatchMapping("/me/complete")
    public ApiResponse<MatchedMemberResponseDTO.CompleteReadingResultDTO> completeTogetherReading(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        MatchedMemberResponseDTO.CompleteReadingResultDTO result =
                matchedMemberService.finishTogetherReading(user.getId(), groupId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }
}
