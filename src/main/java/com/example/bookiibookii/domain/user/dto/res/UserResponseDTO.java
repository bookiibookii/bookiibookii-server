package com.example.bookiibookii.domain.user.dto.res;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.user.entity.UserImage;
import com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO;
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
            // TODO : 유저 badge 리스트 추가,
            List<GroupResponseDTO.MypageGroupDto> groups,
            List<UserBookResponseDTO.MypageBookDto> books
    ){}
}
