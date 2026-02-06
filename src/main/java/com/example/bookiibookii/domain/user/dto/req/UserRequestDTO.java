package com.example.bookiibookii.domain.user.dto.req;

import com.example.bookiibookii.domain.tag.enums.TagType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public class UserRequestDTO {
    public record OnboardingReqDTO(
            @NotNull
            String name,

            @NotEmpty
            @Valid
            List<TagSettingDTO> tags,

            /** 이미지 업로드 후 받은 s3Key. 없으면 null (프로필 이미지 선택 안 함) */
            String s3Key
    ){}

    public record TagSettingDTO (
            @NotNull
            TagType type,

            @NotEmpty
            List<String> value
    ){}

    public record MypageReqDTO (
            @NotBlank
            String nickname,
            // TODO : 프로필 이미지 업데이트
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
            String region
    ){}
}
