package com.example.bookiibookii.domain.recommendation.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.recommendation.dto.res.RecommendationResponseDTO;
import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.repository.UserTagRepository;
import com.example.bookiibookii.domain.user.service.UserTagService;
import com.example.bookiibookii.domain.userbook.service.UserBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {
    private final UserTagService userTagService;
    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
    private final UserBookService userBookService;
    private final GroupsRepository groupsRepository;

    public List<RecommendationResponseDTO.BookmateDto> findRecommendBookmates(Long userId) {
        List<TagType> targetTypes = List.of(TagType.METHOD, TagType.VIBE);

        List<UserTag> currentUserTags = userTagRepository.findByUserIdAndTagTypeIn(userId, targetTypes);
        // 누적도 -> 최신 등록 -> ID 순으로 태그 정렬 후 상위태그 추출
        List<Tag> myTopTags = userTagService.extractTopTags(currentUserTags, 2);

        List<User> matchedUsers;
        List<String> displayTags;

        // 태그 개수에 따른 분기 처리
        if (myTopTags.isEmpty()) {                  // 태그 0개 : 태그 0개 유저 or 랜덤 매칭
            displayTags = Collections.emptyList();
            matchedUsers = findForNoTagUser(userId, targetTypes);
        } else if (myTopTags.size() == 1) {         // 태그 1개 (포함 관계 매칭)
            displayTags = List.of(myTopTags.get(0).getCode());
            matchedUsers = findForOneTagUser(userId, myTopTags.get(0), targetTypes);
        } else {                                    // 태그 2개 이상 (완전 일치 매칭)
            displayTags = myTopTags.stream().map(Tag::getCode).toList();
            matchedUsers = findForTwoTagUser(userId, myTopTags, targetTypes);
        }

        return matchedUsers.stream()
                .map(user -> RecommendationResponseDTO.BookmateDto.builder()
                        .userId(user.getId())
                        .nickname(user.getName())
                        .userImage(user.getUserImage())
                        .matchedTags(displayTags)
                        .recentBookTitle(userBookService.findRecentBookTitleByUserId(user.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<User> findForNoTagUser(Long userId, List<TagType> targetTypes) {
        // 태그가 없는 유저 조회
        List<User> candidates = userRepository.findHostsWithoutTargetTags(
                userId, targetTypes, GroupStatus.RECRUITING
        );

        if (!candidates.isEmpty()) {
            Collections.shuffle(candidates);
            return candidates.stream().limit(5).toList();
        }

        // 랜덤 1명 추천
        return userRepository.findOneRandomHost(userId, GroupStatus.RECRUITING.name())
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    // 현재 유저의 태그 1개를 상위 태그에 포함하는 유저 조회
    private List<User> findForOneTagUser(Long userId, Tag myTag, List<TagType> targetTypes) {
        List<User> candidates = userRepository.findHostsWithTargetTags(userId, targetTypes, GroupStatus.RECRUITING);

        List<User> result = new ArrayList<>();
        Long myTagId = myTag.getId();

        for (User candidate : candidates) {
            List<Tag> candidateTopTags = userTagService.extractTopTags(candidate.getUserTags(), 2);

            boolean includeMyTag = candidateTopTags.stream().anyMatch(t -> t.getId().equals(myTagId));

            if (includeMyTag) {
                result.add(candidate);
            }
        }

        Collections.shuffle(result);
        return result.stream().limit(5).toList();
    }

    // 상위 태그 2개와 완전히 일치하는 유저 조회
    private List<User> findForTwoTagUser(Long userId, List<Tag> myTopTags, List<TagType> targetTypes) {
        List<User> candidates = userRepository.findHostsWithTargetTags(userId, targetTypes, GroupStatus.RECRUITING);

        List<User> result = new ArrayList<>();
        Set<Long> myTagIds = myTopTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        for (User candidate : candidates) {
            List<Tag> candidateTopTags = userTagService.extractTopTags(candidate.getUserTags(), 2);
            if (candidateTopTags.size() < 2) continue;

            Set<Long> candidateTagIds = candidateTopTags.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet());

            if (myTagIds.equals(candidateTagIds)) {
                result.add(candidate);
            }
        }

        Collections.shuffle(result);
        return result.stream().limit(5).toList();
    }


    public List<RecommendationResponseDTO.RecommendedGroupDto> findRecommendGroups(Long userId, boolean isRefresh) {
        List<TagType> targetTypes = List.of(TagType.GENRE, TagType.METHOD, TagType.VIBE, TagType.SPEED);
        List<UserTag> userTags = userTagRepository.findByUserIdAndTagTypeIn(userId, targetTypes);
        List<Long> userTagIds = userTags.stream()
                .map(ut -> ut.getTag().getId())
                .toList();

        List<Groups> candidateGroups = new ArrayList<>();
        // 태그 일치 그룹 조회 (최대 6개)
        if (!userTagIds.isEmpty()) {
            List<Groups> matchedGroups = groupsRepository.findGroupsByTagMatching(
                    userTagIds,
                    GroupStatus.RECRUITING,
                    PageRequest.of(0, 6)
            );
            candidateGroups.addAll(matchedGroups);
        }

        // 6개를 충족하지 못한 경우, 부족한 수량 랜덤 채우기
        int candidateGroupsCount = 6 - candidateGroups.size();

        if (candidateGroupsCount > 0) {
            // 제외할 ID 리스트 추출
            List<Long> excludedIds = candidateGroups.stream()
                    .map(Groups::getGroupId)
                    .collect(Collectors.toList());

            // 빈 리스트일 경우 SQL 에러 방지를 위해 더미 값 추가 (-1)
            if (excludedIds.isEmpty()) {
                excludedIds.add(-1L);
            }

            List<Groups> randomGroups = groupsRepository.findRandomGroupsExcluding(
                    excludedIds,
                    GroupStatus.RECRUITING,
                    candidateGroupsCount
            );
            candidateGroups.addAll(randomGroups);
        }

        // 반환 로직 분기 : 처음 조회할 땐 가장 일치도가 높은 것 추출, 새로 고침 시엔 6개 중 랜덤 3개
        // TODO : Redis 적용하여 candidateGroups를 매번 계산하지 않도록 최적화할 예정
        if (isRefresh) {
            Collections.shuffle(candidateGroups); // 6개 풀 안에서 랜덤 섞기
        }

        // 앞에서 3개 잘라서 DTO 변환 및 반환
        return candidateGroups.stream()
                .limit(3)
                .map(this::toSuggestGroupDto)
                .collect(Collectors.toList());
    }

    private RecommendationResponseDTO.RecommendedGroupDto toSuggestGroupDto(Groups group) {
        return RecommendationResponseDTO.RecommendedGroupDto.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .build();
    }
}
