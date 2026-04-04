package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.enums.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserTagService {

    // 누적도가 높은 상위 UserTag를 limit개수만큼 추출 (동점일 시, 최신 순으로 추출)
    public List<Tag> extractTopTags(List<UserTag> userTags, int limit) {
        if (userTags == null || userTags.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        return userTags.stream()
                .sorted(
                        Comparator.comparing(UserTag::getScore).reversed() // 점수순
                                .thenComparing(UserTag::getUpdatedAt, Comparator.reverseOrder()) // 최신 날짜순
                )
                .limit(limit)
                .map(UserTag::getTag)
                .toList();
    }

}
