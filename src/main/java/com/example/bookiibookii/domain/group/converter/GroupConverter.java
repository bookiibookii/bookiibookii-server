package com.example.bookiibookii.domain.group.converter;

import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.dto.res.MatchedMemberResponseDTO;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GroupConverter {
    private final UserImageS3Service userImageS3Service;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm");
    private static final int EXPIRATION = 60;

    /**
     * 신청자 상세 정보 DTO 변환 (태그 상위 3개 추출 포함)
     */
    public ApplicationResponseDTO.ApplicationDetailDTO toApplicationDetailDTO(Application application) {
        User guest = application.getGuest();

        List<String> top3Tags = (guest.getUserTags() == null) ? new ArrayList<>() :
                guest.getUserTags().stream()
                        .sorted(Comparator.comparingInt(UserTag::getScore).reversed())
                        .limit(3)
                        .map(ut -> ut.getTag().getCode())
                        .toList();

        return ApplicationResponseDTO.ApplicationDetailDTO.builder()
                .applicationId(application.getApplicationId())
                .user(guest.getId())
                .name(guest.getNickName())
                .createdAt(application.getCreatedAt().format(DATE_FMT))
                .tags(top3Tags)
                .applyMsg(application.getApplyMsg())
                .build();
    }

    /**
     * 신청자 목록 응답 DTO 변환
     */
    public ApplicationResponseDTO.ApplicationListDTO toApplicationListDTO(List<ApplicationResponseDTO.ApplicationDetailDTO> detailDTOs) {
        return ApplicationResponseDTO.ApplicationListDTO.builder()
                .applicationList(detailDTOs)
                .totalCount(detailDTOs.size())
                .build();
    }

    /**
     * 신청 상태 변경 결과 DTO 변환
     */
    public ApplicationResponseDTO.UpdateResultDTO toUpdateResultDTO(Application application, Groups group) {
        return ApplicationResponseDTO.UpdateResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus())
                .groupStatus(group.getGroupStatus())
                .updatedAt(LocalDateTime.now().format(DATE_TIME_FMT))
                .build();
    }

    /**
     * 그룹 참가 신청 결과 DTO 변환
     */
    public ApplicationResponseDTO.JoinResultDTO toJoinResultDTO(Application application) {
        return ApplicationResponseDTO.JoinResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus().name())
                .createdAt(LocalDateTime.now().format(DATE_FMT))
                .build();
    }

    /**
     * 참가 취소 결과 DTO 변환
     */
    public ApplicationResponseDTO.CancelResultDTO toCancelResultDTO(Long groupId) {
        return ApplicationResponseDTO.CancelResultDTO.builder()
                .groupId(groupId)
                .canceledAt(LocalDateTime.now().format(DATE_TIME_FMT))
                .build();
    }

    /**
     * 그룹 생성 결과 변환
     */
    public GroupResponseDTO.CreateResultDTO toCreateResultDTO(Groups group) {
        return GroupResponseDTO.CreateResultDTO.builder()
                .groupId(group.getGroupId())
                .groupStatus(group.getGroupStatus())
                .createdAt(LocalDateTime.now().format(DATE_FMT))
                .build();
    }

    /**
     * 그룹 상세 정보 변환
     */
    public GroupResponseDTO.GroupDetailDTO toGroupDetailDTO(
            Groups group,
            List<MatchedMember> members,
            int waitingCount,
            boolean isHot,
            List<GroupResponseDTO.ParticipantSlotDTO> slots,
            String buttonStatus,
            String meetPlace,
            Long userId) {

        return GroupResponseDTO.GroupDetailDTO.builder()
                .groupId(group.getGroupId())
                .title(group.getBook().getTitle())
                .groupComment(group.getGroupComment())
                .groupStatus(group.getGroupStatus().name())
                .isHost(group.getHost().getId().equals(userId))
                .preferRegion(group.getPreferRegion())
                .meetPlace(meetPlace)
                .bookTitle(group.getBook().getTitle())
                .bookImage(group.getBook().getImage())
                .author(group.getBook().getAuthor())
                .category(group.getBook().getCategory().label())
                .readingPeriod(group.getReadingPeriod())
                .matchedCount(members.size())
                .maxCapacity(group.getMaxCapacity())
                .waitingCount(waitingCount)
                .isHot(isHot)
                .hostNickname(group.getHost().getNickName())
                .hostProfileImage(getProfileUrl(group.getHost()))
                .createdAt(group.getCreatedAt().format(DATE_FMT))
                .startDate(group.getStartDate() != null ? group.getStartDate().toString() : null)
                .groupTags(group.getGroupTags().stream().map(gt -> gt.getTag().getCode()).toList())
                .customTag(group.getCustomTag())
                .participantSlots(slots)
                .buttonStatus(buttonStatus)
                .build();
    }

    /**
     * 그룹 요약 DTO 변환 (목록/검색용 공통)
     */
    public GroupResponseDTO.GroupSummaryDTO toGroupSummaryDTO(
            Groups group, int waitingCount, List<String> tags, boolean isHot, String pictureBadge) {

        return GroupResponseDTO.GroupSummaryDTO.builder()
                .groupId(group.getGroupId())
                .title(group.getBook().getTitle())
                .author(group.getBook().getAuthor())
                .genre(group.getBook().getCategory().label())
                .hostProfileImage(getProfileUrl(group.getHost()))
                .bookImage(group.getBook().getImage())
                .hostNickname(group.getHost().getNickName())
                .groupStatus(group.getGroupStatus().name())
                .currentCount(group.getMatchedMember().size())
                .maxCapacity(group.getMaxCapacity())
                .waitingCount(waitingCount)
                .isHot(isHot)
                .groupType(group.getGroupType().name())
                .tradeType(group.getTradeType().name())
                .pictureBadge(pictureBadge)
                .readingPeriod(group.getReadingPeriod())
                .startDate(group.getStartDate() != null ? group.getStartDate().toString() : null)
                .tags(tags)
                .customTag(group.getCustomTag())
                .build();
    }

    /**
     * 완독 결과 변환
     */
    public MatchedMemberResponseDTO.CompleteReadingResultDTO toCompleteReadingResultDTO(MatchedMember mm) {
        return MatchedMemberResponseDTO.CompleteReadingResultDTO.builder()
                .matchedMemberId(mm.getId())
                .currentReadingRate(mm.getCurrentReadingRate())
                .completedAt(mm.getCompletedAt())
                .build();
    }

    private String getProfileUrl(User user) {
        if (user == null || user.getUserImage() == null) return null;
        return userImageS3Service.generatePresignedGetUrl(user.getUserImage().getS3Key(), EXPIRATION);
    }
}