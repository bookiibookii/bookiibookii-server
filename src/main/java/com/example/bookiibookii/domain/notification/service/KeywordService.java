package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.dto.KeywordReqDTO;
import com.example.bookiibookii.domain.notification.dto.KeywordResDTO;
import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.entity.UserKeyword;
import com.example.bookiibookii.domain.notification.enums.KeywordSort;
import com.example.bookiibookii.domain.notification.exception.KeywordException;
import com.example.bookiibookii.domain.notification.exception.code.KeywordErrorCode;
import com.example.bookiibookii.domain.notification.repository.KeywordRepository;
import com.example.bookiibookii.domain.notification.repository.UserKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bookiibookii.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;

    @Transactional(readOnly = true)
    public KeywordResDTO.KeywordList getMyKeywords(User user, KeywordSort sort){

        List<UserKeyword> links = (sort == KeywordSort.ALPHABETICAL)
                ? userKeywordRepository.findAllByUserOrderByAlphabetical(user)
                : userKeywordRepository.findAllByUserOrderByLatest(user);

        List<KeywordResDTO.KeywordItem> items = links.stream()
                .map(uk -> KeywordResDTO.KeywordItem.builder()
                        .keywordId(uk.getKeyword().getId())
                        .content(uk.getKeyword().getContent())
                        .build())
                .toList();

        return KeywordResDTO.KeywordList.builder()
                .keywordSort(sort)
                .keywordNumber(items.size())
                .keywordList(items)
                .build();
    }

    @Transactional
    public KeywordResDTO.KeywordItem saveKeyword(User user, KeywordReqDTO.SaveKeyword req) {
        String content = req.content();

        if (userKeywordRepository.existsByUserAndKeyword_Content(user, content)) {
            throw new KeywordException(KeywordErrorCode.DUPLICATE_USER_KEYWORD);
        }

        long count = userKeywordRepository.countByUser(user);
        if (count >= 10) {
            throw new KeywordException(KeywordErrorCode.KEYWORD_LIMIT_EXCEEDED);
        }

        Keyword keyword = keywordRepository.findByContent(content)
                .orElseGet(() -> {
                    try {
                        return keywordRepository.save(Keyword.builder().content(content).build());
                    } catch (DataIntegrityViolationException e) {
                        if (isConstraint(e, "uk_keyword_content")) {
                            // 동시성으로 이미 insert 된 경우 재조회
                            return keywordRepository.findByContent(content).orElseThrow(() -> e);
                        }
                        throw e;
                    }
                });

        try {
            userKeywordRepository.save(UserKeyword.builder()
                    .user(user)
                    .keyword(keyword)
                    .build());
        } catch (DataIntegrityViolationException e) {
            if (isConstraint(e, "uk_user_keyword")) {
                throw new KeywordException(KeywordErrorCode.DUPLICATE_USER_KEYWORD);
            }
            throw e;
        }

        return KeywordResDTO.KeywordItem.builder()
                .keywordId(keyword.getId())
                .content(keyword.getContent())
                .build();
    }

    @Transactional
    public void deleteKeyword(User user, Long keywordId) {
        UserKeyword link = userKeywordRepository.findByUserAndKeyword_Id(user, keywordId)
                .orElseThrow(() -> new KeywordException(KeywordErrorCode.USER_KEYWORD_NOT_FOUND));

        userKeywordRepository.delete(link);
    }

    private boolean isConstraint(DataIntegrityViolationException e, String constraintName) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        String msg = t.getMessage();
        return msg != null && msg.contains(constraintName);
    }
}
