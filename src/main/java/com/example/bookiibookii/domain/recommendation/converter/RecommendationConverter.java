package com.example.bookiibookii.domain.recommendation.converter;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.recommendation.dto.res.RecommendationResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecommendationConverter {
    public RecommendationResponseDTO.BookmateDto toBookmateDto(User user, List<String> matchedTags, String recentBookTitle) {
        return RecommendationResponseDTO.BookmateDto.builder()
                .userId(user.getId())
                .nickname(user.getNickName())
                .userImage(user.getUserImage()) // UserImage 엔티티 혹은 URL 가공 로직 추가 가능
                .matchedTags(matchedTags)
                .recentBookTitle(recentBookTitle)
                .build();
    }

    public RecommendationResponseDTO.RecommendedGroupDto toRecommendedGroupDto(Groups group) {
        return RecommendationResponseDTO.RecommendedGroupDto.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .build();
    }
}
