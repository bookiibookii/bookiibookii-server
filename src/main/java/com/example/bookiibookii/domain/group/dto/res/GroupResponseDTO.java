package com.example.bookiibookii.domain.group.dto.res;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


public class GroupResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResultDTO{
        private Long groupId;
        private GroupStatus groupStatus;
        private String createdAt;
    }
}
