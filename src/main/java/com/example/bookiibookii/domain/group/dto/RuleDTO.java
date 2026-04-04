package com.example.bookiibookii.domain.group.dto;

import com.example.bookiibookii.domain.user.enums.Tag;
import jakarta.validation.constraints.NotNull;

public record RuleDTO(
        @NotNull
        Tag tag,
        String content  // CUSTOM 타입일 때만 필수, 프리셋은 null
) {}
