package com.example.bookiibookii.domain.user.dto.res;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.user.enums.NicknameStatus;
import com.example.bookiibookii.domain.groupbook.dto.res.GroupBookResponseDTO;
import lombok.Builder;

import java.util.List;

public class UserResponseDTO {
    @Builder
    public record UserProfileResDTO  (
            Long userId,
            /** 프로필 이미지 표시용 Presigned GET URL. 없으면 null */
            String profileImageUrl,
            String nickname,
            String introduction,
            Integer completeBook,
            Integer relayGroup,
            List<GroupResponseDTO.MypageGroupDto> groups,
            List<GroupBookResponseDTO.MypageBookDto> books,
            List<UserPickBookDto> userPickBooks,
            String receiverName,
            String phone,
            String zipCode,
            String address,
            String addressDetail,
            String region,
            String meetPlace
    ){}

    public record UserPickBookDto (
            String title,
            String auth,
            String image
    ){}

    @Builder
    public record NicknameValidationDTO  (
            boolean isAvailable,
            String code,
            String message
    ){
        public static NicknameValidationDTO from(NicknameStatus status) {
            return switch (status) {
                case AVAILABLE -> NicknameValidationDTO.builder()
                        .isAvailable(true)
                        .code("SUCCESS")
                        .message("사용 가능한 닉네임입니다.")
                        .build();
                case DUPLICATE -> NicknameValidationDTO.builder()
                        .isAvailable(false)
                        .code("DUPLICATE")
                        .message("이미 사용 중인 닉네임입니다.")
                        .build();
                case BAD_WORD -> NicknameValidationDTO.builder()
                        .isAvailable(false)
                        .code("BAD_WORD")
                        .message("금칙어가 포함되어 있습니다.")
                        .build();
            };
        }
    }
}
