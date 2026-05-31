package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewsResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MyGroupReviewsResponseDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "BookReview", description = "책 리뷰 및 독서카드 관련 API")
@RequestMapping("/api/groups/{groupId}/reviews")
public interface BookReviewControllerDocs {

    @GetMapping
    @Operation(
            summary = "그룹 리뷰 조회",
            description = """
            그룹에 작성된 책 리뷰와 파트너 리뷰를 함께 조회합니다.

            - 그룹 멤버만 조회할 수 있습니다.
            - groupStatus가 COMPLETED인 그룹만 조회할 수 있습니다.
            - bookReviews: 그룹 내 모든 책 리뷰(책 정보, 작성자 정보, 별점, 내용, 작성 일자)
            - memberReviews: 서로가 서로에게 남긴 파트너 리뷰(그룹명, 독서 기간, 작성자 정보, reaction, 코멘트)
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "그룹 리뷰 조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupReviewsResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "그룹이 종료(COMPLETED) 상태가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    ApiResponse<GroupReviewsResponseDTO> getGroupReviews(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PostMapping
    @Operation(
            summary = "책 리뷰 등록",
            description = """
            현재 로그인 사용자가 교환독서 중 읽고 있는 책에 대한 리뷰를 등록합니다.

            - 그룹 멤버만 작성할 수 있습니다.
            - 별점은 필수이며 0.0~5.0 범위에서 0.5 단위만 허용됩니다.
            - 코멘트는 선택값이며 최대 500자입니다.
            - MY_BOOK_REVIEWING 상태에서는 등록 후 EXCHANGING으로 전환됩니다.
            - PARTNER_BOOK_REVIEWING 상태에서는 등록 후 RETURNING으로 전환됩니다.
            - 같은 memberBook에 대한 중복 리뷰는 허용되지 않습니다.
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ReviewRequestDTO.BookReviewUpsertDTO.class),
                    examples = @ExampleObject(value = """
                    {
                      "star": 4.5,
                      "comment": "문장이 좋아서 오래 기억에 남았어요"
                    }
                    """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "책 리뷰 등록 성공",
                    content = @Content(schema = @Schema(implementation = BookReviewResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "별점 형식 오류, 코멘트 길이 초과, 중복 리뷰, 작성 가능 상태가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹, 매칭 멤버 또는 현재 책을 찾을 수 없음")
    })
    ApiResponse<BookReviewResponseDTO> createBookReview(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewUpsertDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/me")
    @Operation(
            summary = "내 책 리뷰 수정",
            description = """
            현재 로그인 사용자가 작성한 현재 책 리뷰를 수정합니다.

            - 그룹 멤버만 수정할 수 있습니다.
            - 별점은 필수이며 0.0~5.0 범위에서 0.5 단위만 허용됩니다.
            - 코멘트는 선택값이며 최대 500자입니다.
            - 현재 사용자의 MatchedMember와 현재 memberBook 기준으로 리뷰를 찾습니다.
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ReviewRequestDTO.BookReviewUpsertDTO.class),
                    examples = @ExampleObject(value = """
                    {
                      "star": 5.0,
                      "comment": "다시 읽어도 좋았어요"
                    }
                    """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "책 리뷰 수정 성공",
                    content = @Content(schema = @Schema(implementation = BookReviewResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "별점 형식 오류 또는 코멘트 길이 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰, 매칭 멤버 또는 현재 책을 찾을 수 없음")
    })
    ApiResponse<BookReviewResponseDTO> updateMyBookReview(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewUpsertDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/my-group")
    @Operation(
            summary = "종료 그룹 내 리뷰 일괄 수정",
            description = """
            종료된 그룹에서 내가 작성한 책 리뷰 2건과 파트너 리뷰를 수정합니다.

            - 그룹 멤버만 수정할 수 있습니다.
            - groupStatus가 COMPLETED인 그룹만 수정할 수 있습니다.
            - bookReviews: memberBookId 기준으로 별점/코멘트를 부분 수정할 수 있습니다.
            - memberReview: reaction/comment를 부분 수정할 수 있습니다.
            - 수정하지 않을 필드는 요청에서 생략하면 기존 값을 유지합니다.
            - bookReviews, memberReview 중 최소 1개 이상의 수정 항목이 필요합니다.
            """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ReviewRequestDTO.MyGroupReviewsUpdateDTO.class),
                    examples = @ExampleObject(value = """
                    {
                      "bookReviews": [
                        {
                          "memberBookId": 100,
                          "star": 5.0,
                          "comment": "다시 읽어도 좋았어요"
                        },
                        {
                          "memberBookId": 101,
                          "star": 4.0
                        }
                      ],
                      "memberReview": {
                        "reaction": "BOOM_UP",
                        "comment": "좋았어요"
                      }
                    }
                    """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 그룹 리뷰 수정 성공",
                    content = @Content(schema = @Schema(implementation = MyGroupReviewsResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "수정할 항목 없음, 별점/코멘트 형식 오류, 그룹이 종료(COMPLETED) 상태가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 또는 리뷰를 찾을 수 없음")
    })
    ApiResponse<MyGroupReviewsResponseDTO> updateMyGroupReviews(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.MyGroupReviewsUpdateDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
