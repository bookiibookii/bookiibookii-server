package com.example.bookiibookii.domain.recommendation.service;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.recommendation.dto.res.RecommendationResponseDTO;
import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserTagRepository;
import com.example.bookiibookii.domain.user.service.UserTagService;
import com.example.bookiibookii.domain.userbook.service.UserBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {
    private final UserTagService userTagService;
    private final UserTagRepository userTagRepository;
    private final UserBookService userBookService;

    public List<RecommendationResponseDTO.BookmateDto> findRecommendBookmates(Long userId) {
        List<TagType> targetTagTypes = List.of(TagType.METHOD, TagType.VIBE);

        // 내 태그 조회
        List<UserTag> currentUserTags = userTagRepository.findByUserIdAndTagTypeIn(userId, targetTagTypes);

        if (currentUserTags.isEmpty()) {
            throw new UserException(UserErrorCode.USER_TAG_NOT_FOUND);
        }
        List<Tag> myTopTags = userTagService.extractTopTags(currentUserTags, 2);
        if(myTopTags.size() < 2) return null;
        // 기준 Tag Set
        Set<Long> baseTagIds = myTopTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());
        List<String> baseTagCodes = myTopTags.stream().map(Tag::getCode).toList(); // 결과 반환용

        // 후보 UserTag 전체 조회
        List<UserTag> candidateUserTags = userTagRepository.findRecommendUserTags(userId, targetTagTypes, GroupStatus.RECRUITING);
        if (candidateUserTags.isEmpty()) return null;

        // userId 기준으로 UserTag 묶기
        Map<Long, List<UserTag>> userTagMap = candidateUserTags.stream()
                .collect(Collectors.groupingBy(ut -> ut.getUser().getId()));

        List<User> matchedUsers = new ArrayList<>();

        // 각 유저별 TopTags 추출 후 기준 태그와 일치 여부 비교
        for (List<UserTag> userTags : userTagMap.values()) {

            List<Tag> candidateTopTags = userTagService.extractTopTags(userTags, 2);

            // 태그 개수가 부족하면 패스
            if (candidateTopTags.size() < 2) continue;

            Set<Long> candidateTagIds = candidateTopTags.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet());

            // 완전 일치 여부 확인 (Set.equals는 순서 무관하게 내용물 비교)
            if (baseTagIds.equals(candidateTagIds)) {
                matchedUsers.add(userTags.get(0).getUser());
            }
        }

        // 최대 5명 랜덤 선정
        Collections.shuffle(matchedUsers);
        List<User> result = matchedUsers.stream().limit(5).toList();

        return result.stream()
                .map(user -> {
                    String recentBook = userBookService.findRecentBookTitleByUserId(user.getId());
                    return RecommendationResponseDTO.BookmateDto.builder()
                            .userId(user.getId())
                            .nickname(user.getName())
                            .tagTypes(baseTagCodes)
                            .recentBookTitle(recentBook)
                            .build();
                }).toList();
    }
}
