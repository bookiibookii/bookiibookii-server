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
            List<TagSettingDTO> tags
    ){}

    public record TagSettingDTO (
            @NotNull
            TagType type,

            @NotEmpty
            List<String> value
    ){}
}
