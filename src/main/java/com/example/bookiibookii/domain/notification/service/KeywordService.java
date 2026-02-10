package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.converter.NotificationConverter;
import com.example.bookiibookii.domain.notification.dto.KeywordReqDTO;
import com.example.bookiibookii.domain.notification.dto.KeywordResDTO;
import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.entity.UserKeyword;
import com.example.bookiibookii.domain.notification.enums.KeywordSort;
import com.example.bookiibookii.domain.notification.exception.KeywordException;
import com.example.bookiibookii.domain.notification.exception.code.KeywordErrorCode;
import com.example.bookiibookii.domain.notification.repository.KeywordRepository;
import com.example.bookiibookii.domain.notification.repository.UserKeywordRepository;
import com.example.bookiibookii.domain.notification.util.KeywordNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bookiibookii.domain.user.entity.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final NotificationConverter notificationConverter;

    @Transactional(readOnly = true)
    public KeywordResDTO.KeywordList getMyKeywords(User user, KeywordSort sort){
        List<UserKeyword> links = (sort == KeywordSort.ALPHABETICAL)
                ? userKeywordRepository.findAllByUserOrderByAlphabetical(user)
                : userKeywordRepository.findAllByUserOrderByLatest(user);

        return notificationConverter.toKeywordListRes(links, sort); // 컨버터 사용
    }

    @Transactional
    public KeywordResDTO.KeywordItem saveKeyword(User user, KeywordReqDTO.SaveKeyword req) {
        String display = KeywordNormalizer.display(req.content());
        String normalized = KeywordNormalizer.normalize(req.content());
        String prefix2 = KeywordNormalizer.prefix2(normalized);

        if (prefix2 == null || prefix2.isBlank()) {
            throw new KeywordException(KeywordErrorCode.INVALID_KEYWORD);
        }

        if (userKeywordRepository.existsByUserAndKeyword_NormalizedContent(user, normalized)) {
            throw new KeywordException(KeywordErrorCode.DUPLICATE_USER_KEYWORD);
        }

        long count = userKeywordRepository.countByUser(user);
        if (count >= 10) {
            throw new KeywordException(KeywordErrorCode.KEYWORD_LIMIT_EXCEEDED);
        }

        Keyword keyword = keywordRepository.findByNormalizedContent(normalized)
                .orElseGet(() -> {
                    try {
                        return keywordRepository.save(
                                Keyword.builder()
                                        .content(display)
                                        .normalizedContent(normalized)
                                        .prefix2(prefix2)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        // 동시성으로 이미 insert된 경우 재조회
                        return keywordRepository.findByNormalizedContent(normalized)
                                .orElseThrow(() -> e);
                    }
                });

        try {
            userKeywordRepository.save(UserKeyword.builder()
                    .user(user)
                    .keyword(keyword)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new KeywordException(KeywordErrorCode.DUPLICATE_USER_KEYWORD);
        }

        return notificationConverter.toKeywordItemRes(keyword);
    }

    @Transactional
    public void deleteKeyword(User user, Long keywordId) {
        UserKeyword link = userKeywordRepository.findByUserAndKeyword_Id(user, keywordId)
                .orElseThrow(() -> new KeywordException(KeywordErrorCode.USER_KEYWORD_NOT_FOUND));

        userKeywordRepository.delete(link);
    }
}
