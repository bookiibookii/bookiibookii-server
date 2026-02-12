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
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.user.repository.UserTagRepository;
import com.example.bookiibookii.domain.user.service.UserTagService;
import com.example.bookiibookii.domain.userbook.service.UserBookService;
import com.example.bookiibookii.global.util.RedisUtil;
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
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final UserTagService userTagService;
    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
    private final UserBookService userBookService;
    private final GroupsRepository groupsRepository;
    private final RedisUtil redisUtil;
    private final UserImageS3Service userImageS3Service;

    // 캐시 키 접두사 상수
    private static final String REC_CACHE_KEY_PREFIX = "REC:GROUP:";

    public List<RecommendationResponseDTO.BookmateDto> findRecommendBookmates(Long userId) {
        List<TagType> targetTypes = List.of(TagType.METHOD, TagType.VIBE);

        List<UserTag> currentUserTags = userTagRepository.findByUserIdAndTagTypeIn(userId, targetTypes);
        // 누적도 -> 최신 등록 -> ID 순으로 태그 정렬 후 상위태그 추출
        List<Tag> myTopTags = userTagService.extractTopTags(currentUserTags, 2);
        List<User> matchedUsers;
        List<String> displayTags;

        // 태그 개수에 따른 분기 처리
        if (currentUserTags.size() < 1 || myTopTags.isEmpty() || myTopTags.size() < 1) { // 태그 0개 : 태그 0개 유저 or 랜덤 매칭
            displayTags = Collections.emptyList();
            matchedUsers = findForNoTagUser(userId, targetTypes);
        } else if (myTopTags.size() == 1) {         // 태그 1개 (포함 관계 매칭)
            displayTags = List.of(myTopTags.get(0).getCode());
            matchedUsers = findForOneTagUser(userId, myTopTags.get(0), targetTypes);
        } else {                                    // 태그 2개 이상 (완전 일치 매칭)
            displayTags = myTopTags.stream().map(Tag::getCode).toList();
            matchedUsers = findForTwoTagUser(userId, myTopTags, targetTypes);
        }

        if (matchedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        return matchedUsers.stream()
                .map(user -> {
                    String profileImageUrl = null;
                    if (user.getUserImage() != null) {
                        profileImageUrl = userImageS3Service.generatePresignedGetUrl(
                                user.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
                    }
                    return RecommendationResponseDTO.BookmateDto.builder()
                            .userId(user.getId())
                            .nickname(user.getNickName())
                            .profileImageUrl(profileImageUrl)
                            .matchedTags(displayTags)
                            .recentBookTitle(userBookService.findRecentBookTitleByUserId(user.getId()))
                            .build();
                })
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
        String cacheKey = REC_CACHE_KEY_PREFIX + userId; // Redis 키 설정
        List<RecommendationResponseDTO.RecommendedGroupDto> candidateDtos;

        // Redis에서 캐시 조회
        RecommendationResponseDTO.RecommendedGroupDto[] cachedArray =
                redisUtil.get(cacheKey, RecommendationResponseDTO.RecommendedGroupDto[].class);
        if (cachedArray != null && cachedArray.length > 0) {
            // [Cache Hit] 캐시가 있으면 리스트로 변환하여 사용
            candidateDtos = new ArrayList<>(Arrays.asList(cachedArray));
        } else {
            // [Cache Miss] 캐시가 없으면 DB에서 조회 (기존 로직 수행)
            candidateDtos = fetchRecommendationsFromDB(userId);

            // 조회된 결과가 있다면 Redis에 저장 (예: 1시간 유효)
            if (!candidateDtos.isEmpty()) {
                redisUtil.set(
                        cacheKey,
                        candidateDtos.toArray(new RecommendationResponseDTO.RecommendedGroupDto[0]), // List -> Array 변환
                        60
                );
            }
        }

        // 새로고침 시 캐시된 추천 풀을 셔플하여 다른 조합 반환
        if(isRefresh) {
            Collections.shuffle(candidateDtos);
        }

        // 앞에서 3개 잘라서 DTO 변환 및 반환
        return candidateDtos.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    private List<RecommendationResponseDTO.RecommendedGroupDto> fetchRecommendationsFromDB(Long userId) {
        List<TagType> targetTypes = List.of(TagType.GENRE, TagType.METHOD, TagType.VIBE, TagType.SPEED);
        List<UserTag> userTags = userTagRepository.findByUserIdAndTagTypeIn(userId, targetTypes);
        List<Long> userTagIds = userTags.stream()
                .map(ut -> ut.getTag().getId())
                .toList();

        List<Groups> candidateGroups = new ArrayList<>();

        // 태그 일치 그룹 조회 (최대 50개)
        if (!userTagIds.isEmpty()) {
            List<Groups> matchedGroups = groupsRepository.findGroupsByTagMatching(
                    userTagIds,
                    GroupStatus.RECRUITING,
                    PageRequest.of(0, 50)
            );
            candidateGroups.addAll(matchedGroups);
        }

        // 최소 6개를 충족하지 못한 경우, 부족한 수량 랜덤 채우기
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

            List<Groups> randomGroups =
                    groupsRepository.findRandomGroupsExcludingFetchBook(
                            excludedIds,
                            GroupStatus.RECRUITING,
                            PageRequest.of(0, candidateGroupsCount)
                    );
            candidateGroups.addAll(randomGroups);
        }

        return candidateGroups.stream()
                .map(this::toSuggestGroupDto)
                .collect(Collectors.toList());
    }

    private RecommendationResponseDTO.RecommendedGroupDto toSuggestGroupDto(Groups group) {
        return RecommendationResponseDTO.RecommendedGroupDto.builder()
                .groupId(group.getGroupId())
                .bookImageUrl(group.getBook().getImage())
                .bookTitle(group.getBook().getTitle())
                .build();
    }
}
