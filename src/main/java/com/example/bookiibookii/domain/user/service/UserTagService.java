package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.enums.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserTagService {

    // 누적도가 높은 상위 UserTag를 추출 (동점일 시, 최신 순으로 추출)
    public Tag extractTopTags(List<UserTag> userTags) {
        if (userTags == null || userTags.isEmpty()) {
            return null;
        }

        return userTags.stream()
                .sorted(
                        Comparator.comparing(UserTag::getScore).reversed() // 점수순
                                .thenComparing(UserTag::getUpdatedAt, Comparator.reverseOrder()) // 최신 날짜순
                )
                .map(UserTag::getTag).findFirst().orElse(null);
    }

}
