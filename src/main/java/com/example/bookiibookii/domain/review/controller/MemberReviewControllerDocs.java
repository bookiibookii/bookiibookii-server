package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.MemberReviewResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "BookReview", description = "책 리뷰 및 독서카드 관련 API")
@RequestMapping("/api/groups/{groupId}/member-reviews")
public interface MemberReviewControllerDocs {

    @PostMapping
    @Operation(
            summary = "파트너 후기 등록",
            description = """
            1:1 교환독서의 상대 멤버에게 파트너 후기를 남깁니다.

            - 그룹 멤버만 작성할 수 있습니다.
            - 코멘트는 필수이며 최대 20자입니다.
            - reaction은 선택값입니다. 전달하지 않거나 null로 보내도 등록할 수 있습니다.
            - reaction 값은 BOOM_UP, BOOM_DOWN 중 하나입니다.
            - 두 번째 교환 플로우가 양쪽 모두 완료된 후에만 작성할 수 있습니다.
            - 같은 groupId에서 같은 reviewer는 한 번만 작성할 수 있습니다.
            - 양쪽 멤버가 모두 파트너 후기를 작성하면 그룹이 최종 종료됩니다.
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ReviewRequestDTO.MemberReviewCreateDTO.class),
                    examples = {
                            @ExampleObject(
                                    name = "reaction 포함",
                                    value = """
                                    {
                                      "reaction": "BOOM_UP",
                                      "comment": "좋았어요"
                                    }
                                    """
                            ),
                            @ExampleObject(
                                    name = "reaction 없이 등록",
                                    value = """
                                    {
                                      "comment": "좋았어요"
                                    }
                                    """
                            ),
                            @ExampleObject(
                                    name = "reaction null",
                                    value = """
                                    {
                                      "reaction": null,
                                      "comment": "좋았어요"
                                    }
                                    """
                            )
                    }
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "파트너 후기 등록 성공",
                    content = @Content(schema = @Schema(implementation = MemberReviewResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "코멘트 누락, 길이 초과, 중복 후기, 작성 가능 상태가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹, 매칭 멤버 또는 상대 멤버를 찾을 수 없음")
    })
    ApiResponse<MemberReviewResponseDTO> createMemberReview(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.MemberReviewCreateDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
