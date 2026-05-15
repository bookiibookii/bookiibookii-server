package com.example.bookiibookii.domain.user.dto.req;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.location.dto.req.UserDeliveryReqDTO;
import com.example.bookiibookii.domain.location.dto.req.UserExchangeReqDTO;
import com.example.bookiibookii.domain.user.enums.Gender;
import com.example.bookiibookii.domain.user.enums.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public class UserRequestDTO {
    public record OnboardingReqDTO(
            @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            String name,

            @NotNull(message = "성별은 필수 입력 사항입니다.")
            Gender gender,

            @NotNull(message = "생년월일은 필수 입력 사항입니다.")
            LocalDate birth,

            @NotEmpty
            @Valid
            List<Tag> tags,

            /** 이미지 업로드 후 받은 s3Key. 없으면 null (프로필 이미지 선택 안 함) */
            String s3Key,

            @Size(max = 7, message = "대표 도서는 최대 7권까지 설정 가능합니다.")
            @Valid
            List<BookReqDTO.UserPickISBN> userBooks,

            String introduction
    ){}

    public record MypageReqDTO (
            @NotBlank
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            String nickname,

            @Schema(description = "프로필 이미지 S3 키. Presigned URL로 업로드 후 받은 값. 미전달 시 프로필 이미지 변경 안 함.", example = "image/users/1/550e8400-e29b-41d4-a716-446655440000")
            String s3Key,

            String introduction,

            @Schema(description = "추가할 배송지 목록. null이면 변경 없음.")
            @Valid
            List<UserDeliveryReqDTO.AddReqDTO> deliveriesToAdd,

            @Schema(description = "삭제할 배송지 ID 목록. null이면 변경 없음.")
            List<Long> deliveryIdsToDelete,

            @Schema(description = "추가할 교환 장소 목록. null이면 변경 없음.")
            @Valid
            List<UserExchangeReqDTO.AddReqDTO> exchangesToAdd,

            @Schema(description = "삭제할 교환 장소 ID 목록. null이면 변경 없음.")
            List<Long> exchangeIdsToDelete
    ){}
}
