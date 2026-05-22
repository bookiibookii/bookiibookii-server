package com.example.bookiibookii.domain.user.dto.req;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.user.enums.Gender;
import com.example.bookiibookii.domain.user.enums.Tag;
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

    public record UpdateIntroductionReqDTO(
            @Size(max = 255, message = "한줄 소개는 255자 이하로 입력해주세요.")
            String introduction
    ) {}

    public record MypageReqDTO(
            @NotBlank
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            String nickname,

            Gender gender,

            LocalDate birth,

            /** Presigned URL로 업로드 후 받은 s3Key. null이면 프로필 이미지 변경 안 함. */
            String s3Key
    ){}
}
