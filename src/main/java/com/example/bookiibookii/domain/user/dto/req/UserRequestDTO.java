package com.example.bookiibookii.domain.user.dto.req;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.user.enums.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserRequestDTO {
    public record OnboardingReqDTO(
            @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            String name,

            @NotEmpty
            @Valid
            List<Tag> tags,

            /** 이미지 업로드 후 받은 s3Key. 없으면 null (프로필 이미지 선택 안 함) */
            String s3Key,

            @Size(max = 7, message = "대표 도서는 최대 7권까지 설정 가능합니다.")
            @Valid
            List<BookReqDTO.UserPickISBN> userPickBooks,

            String introduction,

            // TODO : 집, 회사, 교환장소 등 상세주소(위치좌표)를 위치 테이블에 저장하여 좌표 기준 인접 km 계산가능하도록 로직 수정 필요
            // 임의로 시+군+구 합친 위치를 string으로 저장
            String region
    ){}

    public record MypageReqDTO (
            @NotBlank
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            String nickname,
            String introduction,

            @Schema(description = "프로필 이미지 S3 키. Presigned URL로 업로드 후 받은 값. 미전달 시 프로필 이미지 변경 안 함.", example = "image/users/1/550e8400-e29b-41d4-a716-446655440000")
            String s3Key,

            @NotBlank
            String receiverName,
            @NotBlank
            @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
            String phone,
            @NotBlank
            String zipCode,
            @NotBlank
            String address,
            @NotBlank
            String addressDetail,
            String meetPlace,
            String region,
            Boolean tagVisible,
            List<BookReqDTO.UserPickISBN> userPickBooks
    ){}
}
