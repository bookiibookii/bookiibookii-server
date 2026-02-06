package com.example.bookiibookii.domain.user.dto.res;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.user.entity.UserImage;
import com.example.bookiibookii.domain.user.enums.NicknameStatus;
import com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.util.List;

public class UserResponseDTO {
    @Builder
    public record UserProfileResDTO  (
            Long userId,
            UserImage userImage,
            String nickname,
            Double manner,
            List<String> topTags,
            Integer completeBook,
            Integer relayGroup,
            Integer togetherGroup,
            List<UserBadgeDTO>  userBadges,
            List<GroupResponseDTO.MypageGroupDto> groups,
            List<UserBookResponseDTO.MypageBookDto> books,
            String receiverName,
            String phone,
            String zipCode,
            String address,
            String addressDetail,
            String meetPlace
    ){}

    @Builder
    public record UserBadgeDTO  (
            String userBadge,
            Integer count
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
