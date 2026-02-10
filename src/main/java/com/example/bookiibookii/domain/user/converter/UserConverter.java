package com.example.bookiibookii.domain.user.converter;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.entity.Address;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBadge;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserConverter {

    private final UserImageS3Service userImageS3Service;

    /**
     * 유저 프로필 응답 DTO 변환
     */
    public UserResponseDTO.UserProfileResDTO toUserProfileResDTO(
            User user,
            List<String> topTagCodes,
            List<UserBadge> userBadges,
            Long completeBookCount,
            Long relayCount,
            Long togetherCount,
            List<Groups> targetGroups,
            List<UserBookResponseDTO.MypageBookDto> recentBooks,
            Address address,
            int expirationMinutes
    ) {
        // 프로필 이미지 URL 생성
        String profileImageUrl = null;
        if (user.getUserImage() != null) {
            profileImageUrl = userImageS3Service.generatePresignedGetUrl(
                    user.getUserImage().getS3Key(), expirationMinutes);
        }

        // 배지 리스트 변환
        List<UserResponseDTO.UserBadgeDTO> badgeList = (userBadges == null ? List.<UserBadge>of() : userBadges).stream()
                .map(ub -> UserResponseDTO.UserBadgeDTO.builder()
                        .userBadge(ub.getBadge().name())
                        .count(ub.getCount())
                        .build())
                .toList();

        // 그룹 리스트 변환
        List<GroupResponseDTO.MypageGroupDto> groupList = (targetGroups == null ? List.<Groups>of() : targetGroups).stream()
                .map(this::toMypageGroupDto)
                .collect(Collectors.toList());

        return UserResponseDTO.UserProfileResDTO.builder()
                .userId(user.getId())
                .profileImageUrl(profileImageUrl)
                .nickname(user.getNickName())
                .manner(user.getManner())
                .topTags(topTagCodes)
                .completeBook(completeBookCount.intValue())
                .relayGroup(relayCount.intValue())
                .togetherGroup(togetherCount.intValue())
                .userBadges(badgeList)
                .groups(groupList)
                .books(recentBooks)
                .receiverName(address != null ? address.getReceiverName() : null)
                .phone(address != null ? address.getPhone() : null)
                .zipCode(address != null ? address.getZipCode() : null)
                .address(address != null ? address.getAddress() : null)
                .addressDetail(address != null ? address.getAddressDetail() : null)
                .region(user.getRegion())
                .meetPlace(user.getMeetPlace())
                .build();
    }

    /**
     * 마이페이지용 그룹 리스트 아이템 변환
     */
    public GroupResponseDTO.MypageGroupDto toMypageGroupDto(Groups group) {
        List<String> displayTags = group.getGroupTags().stream()
                .map(gt -> gt.getTag().getCode())
                .toList();

        return GroupResponseDTO.MypageGroupDto.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .auth(group.getBook().getAuthor())
                .genre(group.getBook().getCategory().name())
                .groupStatus(group.getGroupStatus())
                .groupTags(displayTags)
                .build();
    }

    public List<String> toTagCodeList(List<Tag> tags) {
        if (tags == null) return List.of();
        return tags.stream()
                .map(Tag::getCode)
                .toList();
    }
}