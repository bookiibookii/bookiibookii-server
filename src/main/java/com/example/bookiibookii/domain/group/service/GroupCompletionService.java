package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.review.entity.GroupReview;
import com.example.bookiibookii.domain.review.repository.GroupReviewRepository;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupCompletionService {

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupReviewRepository groupReviewRepository; // 리뷰 레포지토리 추가

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forceCompleteSingleGroup(Long groupId) {
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if(group.getGroupStatus()!=GroupStatus.MATCHED){
            log.info("MATCHED 상태가 아닌 그룹");
            return;
        }

        List<MatchedMember> lazyMembers = matchedMemberRepository
                .findAllByGroup_IdAndIsReviewWrittenFalse(groupId);

        for (MatchedMember mm : lazyMembers) {
            if (group.getGroupType() == GroupType.RELAY) {
                // 1. 파트너의 User ID가 아니라 MatchedMember 자체를 찾아야 합니다.
                matchedMemberRepository.findPartnerUserId(groupId, mm.getUser().getId())
                        .ifPresent(partnerId -> {
                            // 파트너의 MatchedMember 엔티티 조회
                            MatchedMember partnerMM = matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, partnerId)
                                    .orElseThrow(() -> new GroupException(GroupErrorCode.MATCHED_MEMBER_NOT_FOUND));

                            if (partnerMM != null) {
                                createDefaultReview(mm, partnerMM);
                            }
                        });
            }
            mm.markReviewAsWritten();
        }
        group.updateStatus(GroupStatus.COMPLETED);
    }

    private void createDefaultReview(MatchedMember reviewer, MatchedMember reviewed) {
        GroupReview defaultReview = GroupReview.builder()
                .reviewer(reviewer)   // MatchedMember 타입
                .reviewed(reviewed)   // MatchedMember 타입
                .rating(3.0)          // score -> rating
                .comment(null) // content -> comment
                .build();
        groupReviewRepository.save(defaultReview);
    }
}
