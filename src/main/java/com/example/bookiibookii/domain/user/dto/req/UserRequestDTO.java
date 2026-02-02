package com.example.bookiibookii.domain.user.dto.req;

import com.example.bookiibookii.domain.tag.enums.TagType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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
}
