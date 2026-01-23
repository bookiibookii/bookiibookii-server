package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.user.entity.UserTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserTagService {

    // 누적도가 높은 상위 UserTag를 limit개수만큼 추출
    public List<Tag> extractTopTags(List<UserTag> userTags, int limit)
    {
        if (userTags == null || userTags.isEmpty() || limit <= 0) {
            return null;
        }

        // score 기준 내림차순 정렬
        List<UserTag> sortedUserTags = new ArrayList<>(userTags);
        sortedUserTags.sort(Comparator.comparing(UserTag::getScore).reversed());

        // 모두 0점인 경우 랜덤 반환
        if (sortedUserTags.get(0).getScore() == 0) {
            Collections.shuffle(sortedUserTags);
            return sortedUserTags.stream().limit(limit).map(UserTag::getTag).toList();
        }

        // 컷라인(limit번째) 동점자 처리 로직을 포함한 상위 UserTag 리스트 추출
        if (sortedUserTags.size() <= limit) {
            return sortedUserTags.stream().map(UserTag::getTag).toList();
        }
        int cutoffScore = sortedUserTags.get(limit - 1).getScore(); // limit 경계 점수 (컷라인)

        // 컷라인 이상 후보 추출
        List<UserTag> guaranteed = sortedUserTags.stream()
                .filter(ut -> ut.getScore() > cutoffScore)
                .toList();

        // 컷라인의 동점자 그룹 처리
        List<UserTag> tied = sortedUserTags.stream()
                .filter(ut -> ut.getScore() == cutoffScore)
                .collect(Collectors.toList());
        Collections.shuffle(tied);

        List<UserTag> result = new ArrayList<>(guaranteed);
        result.addAll(tied);

        return result.stream()
                .limit(limit)
                .map(UserTag::getTag)
                .toList();
    }

}
