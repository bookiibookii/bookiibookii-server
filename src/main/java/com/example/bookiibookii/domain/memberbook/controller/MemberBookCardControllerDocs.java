package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.memberbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardReactionToggleRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardUpdateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardBookmarkResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardReactionToggleResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardListResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "MemberBook Card", description = "멤버북 기준 독서카드 API")
public interface MemberBookCardControllerDocs {

    @Operation(
            summary = "그룹 독서카드 목록 조회",
            description = """
            그룹에 속한 전체 멤버의 memberBook 독서카드를 한 번에 조회합니다.

            - **엔드포인트**: `GET /api/member-books/group/{groupId}/cards`
            - 그룹 멤버만 조회 가능합니다.
            - 생성일 기준 오름차순으로 반환합니다.
            - 목록 조회 전 `member_card`에서 현재 사용자·그룹 기준 `hidden=true`인 카드를 먼저 조회하고 제외합니다(소프트 삭제).
            - 각 카드에 책 제목(`bookTitle`), 작성자(`creatorName`, `creatorProfileImageUrl`), 본인 책 여부(`isMine`), 북마크 여부(`isBookmarked`),
              리액션 집계(`reactionCounts`), 내 리액션(`myReactions`)이 포함됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 없음")
    })
    @GetMapping("/group/{groupId}/cards")
    ApiResponse<MemberCardListResponseDTO> getCardsByGroupId(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "그룹 식별자(ID)", example = "1")
            @PathVariable Long groupId
    );

    @Operation(
            summary = "멤버북 독서카드 상세 조회",
            description = """
            memberBook 도메인 독서카드 한 건의 상세 정보를 조회합니다.

            - **엔드포인트**: `GET /api/member-books/cards/detail/{cardId}`
            - 카드 소유자(MemberBook 소유자)이거나 같은 그룹 멤버만 조회 가능합니다.
            - 내가 숨긴 카드는 404로 처리됩니다.
            - 응답 형식은 목록 항목과 동일합니다 (`MemberCardResponseDTO`).
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음 또는 접근 권한 없음")
    })
    @GetMapping("/cards/detail/{cardId}")
    ApiResponse<MemberCardResponseDTO> getCardDetail(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "독서카드 식별자(ID)", example = "1")
            @PathVariable Long cardId
    );

    @Operation(
            summary = "독서카드 이미지 업로드용 Presigned URL 발급",
            description = """
            IMAGE 타입 독서카드의 S3 업로드용 presigned URL을 발급합니다. **생성·이미지 수정 모두 동일 API**를 사용합니다.

            - 본인 MatchedMember에 연결된 memberBookId만 사용 가능합니다.
            - s3Key 형식: image/cards/{uuid}
            - URL은 10분간 유효합니다.
            - 업로드 완료 후 s3Key를 독서카드 **생성** 또는 **수정**(IMAGE 타입, s3Key 변경) API body에 전달하세요.
            - 카드 수정 전용 presigned URL API는 별도로 두지 않습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "멤버북 없음 또는 소유권 없음")
    })
    @PostMapping("/{memberBookId}/cards/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "멤버북 식별자(ID)", example = "1")
            @PathVariable Long memberBookId
    );

    @Operation(
            summary = "멤버북 독서카드 생성",
            description = """
            memberBook 도메인 기준으로 독서카드를 생성합니다.

            - **엔드포인트**: `POST /api/member-books/{memberBookId}/cards`
              (기존 `POST /api/cards/{groupBookId}`와 경로 충돌을 피하기 위해 member-books 하위로 분리)
            - **TEXT**: quotation(필수, 최대 140자), memo(선택, 최대 110자), page(선택)
            - **IMAGE**: s3Key(필수, S3 업로드 완료), memo(선택), page(선택)
            - 본인 MatchedMember에 연결된 memberBookId만 생성 가능합니다.
            - 서재에서 제거된(removedAt 존재) 멤버북에는 생성할 수 없습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "독서카드 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수값 누락, 길이 초과, S3 검증 실패 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "멤버북 없음 또는 소유권 없음")
    })
    @PostMapping("/{memberBookId}/cards")
    ApiResponse<MemberCardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "멤버북 식별자(ID)", example = "1")
            @PathVariable Long memberBookId,
            @Valid @RequestBody MemberCardCreateRequestDTO request
    );

    @Operation(
            summary = "멤버북 독서카드 수정",
            description = """
            memberBook 도메인 독서카드를 부분 수정합니다. 전달한 필드만 변경됩니다.

            - **엔드포인트**: `PATCH /api/member-books/cards/{cardId}`
            - **TEXT**: page, memo, quotation (null이면 해당 필드 미변경, quotation은 빈 문자열 불가)
            - **IMAGE**: page, memo, s3Key (null이면 미변경). 이미지 변경 시 생성용 presigned URL API로 업로드 후 s3Key 전달
            - 본인이 소유한 MemberBook에 속한 카드만 수정 가능합니다.
            - 응답 형식은 생성 API와 동일합니다 (`MemberCardCreateResponseDTO`).
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "독서카드 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "독서카드 없음 또는 소유권 없음")
    })
    @PatchMapping("/cards/{cardId}")
    ApiResponse<MemberCardCreateResponseDTO> updateCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "독서카드 식별자(ID)", example = "1")
            @PathVariable Long cardId,
            @Valid @RequestBody MemberCardUpdateRequestDTO request
    );

    @Operation(
            summary = "내가 북마크한 멤버북 독서카드 목록 조회",
            description = """
            현재 로그인한 사용자가 북마크한 memberBook 독서카드 목록을 최신 북마크 순으로 반환합니다.

            - **엔드포인트**: `GET /api/member-books/cards/bookmarks`
            - 각 항목은 `MemberCardResponseDTO`이며 `isBookmarked`는 true입니다.
            - 내 화면에서 숨긴 카드(`hidden=true`)는 목록에서 제외됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/cards/bookmarks")
    ApiResponse<java.util.List<MemberCardResponseDTO>> getMyBookmarkedCards(
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "멤버북 독서카드 리액션 토글",
            description = """
            그룹 MatchedMember(본인 포함)가 독서카드에 리액션을 남기거나 취소합니다.

            - **엔드포인트**: `PATCH /api/member-books/cards/{cardId}/reactions`
            - **요청 body**: `{ "reaction": "LIKE" }` — `CardReactionType` (LIKE, SAD, CHEERUP, FEELYOU, AWESOME, FUN)
            - 동일 리액션을 다시 누르면 취소됩니다(토글). 응답 `active`: true = 적용, false = 취소.
            - 카드 소유자이거나 같은 그룹 멤버만 가능합니다.
            - 내 화면에서 숨긴 카드(`hidden=true`)는 404로 처리됩니다(상세 조회와 동일).
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음, 그룹 멤버 아님, 숨긴 카드"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동시 요청 충돌 (MB409_2)")
    })
    @PatchMapping("/cards/{cardId}/reactions")
    ApiResponse<MemberCardReactionToggleResponseDTO> toggleReaction(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "독서카드 식별자(ID)", example = "1")
            @PathVariable Long cardId,
            @RequestBody MemberCardReactionToggleRequestDTO request
    );

    @Operation(
            summary = "멤버북 독서카드 북마크 토글",
            description = """
            memberBook 독서카드에 대한 북마크를 토글합니다. 터치 시 북마크 ↔ 해제가 전환됩니다.

            - **엔드포인트**: `PATCH /api/member-books/cards/{cardId}/bookmark`
            - 카드 소유자이거나 같은 그룹 멤버만 북마크 가능합니다.
            - `MemberCard`가 없으면 생성한 뒤 `bookmarked`를 토글합니다.
            - 내 화면에서 숨긴 카드(`hidden=true`)는 북마크 토글 불가 (`MB400_9`).
            - 응답 `bookmarked`: true = 북마크됨, false = 북마크 해제됨.
            - 목록·상세 조회(`MemberCardResponseDTO`)의 `isBookmarked`에 반영됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "숨긴 카드 북마크 시도 (MB400_9)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음 또는 접근 권한 없음")
    })
    @PatchMapping("/cards/{cardId}/bookmark")
    ApiResponse<MemberCardBookmarkResponseDTO> toggleBookmark(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "독서카드 식별자(ID)", example = "1")
            @PathVariable Long cardId
    );

    @Operation(
            summary = "멤버북 독서카드 내 화면에서 제거",
            description = """
            그룹 내 공유된 독서카드를 내 화면에서만 숨깁니다. Cards 데이터는 삭제되지 않으며, 다른 멤버는 계속 조회할 수 있습니다.

            - **엔드포인트**: `DELETE /api/member-books/cards/{cardId}`
            - `MemberCard`가 없으면 생성하고 `hidden=true`로 설정합니다.
            - 카드 소유자 또는 그룹 멤버만 호출 가능합니다. 이미 숨긴 카드는 무시됩니다(멱등).
            - 북마크된 카드는 삭제할 수 없습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "북마크된 카드"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음 또는 접근 권한 없음")
    })
    @DeleteMapping("/cards/{cardId}")
    ApiResponse<Void> removeCardFromView(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "독서카드 식별자(ID)", example = "1")
            @PathVariable Long cardId
    );
}
