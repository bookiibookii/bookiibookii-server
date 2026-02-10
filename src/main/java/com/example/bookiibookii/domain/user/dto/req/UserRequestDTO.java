package com.example.bookiibookii.domain.user.dto.req;

import com.example.bookiibookii.domain.tag.enums.TagType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
            List<TagSettingDTO> tags,

            /** 이미지 업로드 후 받은 s3Key. 없으면 null (프로필 이미지 선택 안 함) */
            String s3Key
    ){}

    public record TagSettingDTO (
            @NotNull
            TagType type,

            @NotEmpty
            List<@NotBlank(message = "태그 값은 비어 있을 수 없습니다.") String> value
    ){}

    public record MypageReqDTO (
            @NotBlank
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
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
