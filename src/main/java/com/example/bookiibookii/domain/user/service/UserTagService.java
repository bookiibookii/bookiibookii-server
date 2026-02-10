package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.user.converter.UserConverter;
import com.example.bookiibookii.domain.user.entity.UserTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTagService {
    private final UserConverter userConverter;

    // 누적도가 높은 상위 UserTag를 limit개수만큼 추출 (동점일 시, 최신 순으로 추출)
    public List<Tag> extractTopTags(List<UserTag> userTags, int limit) {
        if (userTags == null || userTags.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        return userTags.stream()
                .sorted(
                        Comparator.comparing(UserTag::getScore).reversed() // 점수순
                                .thenComparing(UserTag::getUpdatedAt, Comparator.reverseOrder()) // 최신 날짜순
                                .thenComparing(ut -> ut.getTag().getId()) // ID순
                )
                .limit(limit)
                .map(UserTag::getTag)
                .toList();
    }

    /**
     * 상위 태그를 추출하여 코드(String) 리스트로 반환
     */
    public List<String> extractTopTagCodes(List<UserTag> userTags, int limit) {
        List<Tag> topTags = extractTopTags(userTags, limit);
        return userConverter.toTagCodeList(topTags);
    }
}
