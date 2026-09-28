package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordMatchServiceTest {

    @Mock
    private KeywordRepository keywordRepository;

    private KeywordMatchService service;

    @BeforeEach
    void setUp() {
        service = new KeywordMatchService(keywordRepository);
        when(keywordRepository.findAllByNormalizedContentIn(any(Collection.class))).thenReturn(List.of());
    }

    @Test
    void matchesKeywordContainedInBookTitle() {
        Keyword keyword = keyword(1L, "클린 코드", "클린코드");
        when(keywordRepository.findAllByPrefix2In(any(Collection.class))).thenReturn(List.of(keyword));

        assertThat(service.matchForGroup("클린 코드 실전", "저자", "함께 읽기"))
                .containsExactly(keyword);
    }

    @Test
    void matchesKeywordContainedInAuthorName() {
        Keyword keyword = keyword(2L, "김영하", "김영하");
        when(keywordRepository.findAllByPrefix2In(any(Collection.class))).thenReturn(List.of(keyword));

        assertThat(service.matchForGroup("소설", "김영하 작가", "함께 읽기"))
                .containsExactly(keyword);
    }

    @Test
    void matchesKeywordContainedInGroupTitle() {
        Keyword keyword = keyword(3L, "경제", "경제");
        when(keywordRepository.findAllByPrefix2In(any(Collection.class))).thenReturn(List.of(keyword));

        assertThat(service.matchForGroup("도서", "저자", "퇴근 후 경제 읽기"))
                .containsExactly(keyword);
    }

    private Keyword keyword(Long id, String content, String normalizedContent) {
        return Keyword.builder()
                .id(id)
                .content(content)
                .normalizedContent(normalizedContent)
                .prefix2(normalizedContent.substring(0, Math.min(2, normalizedContent.length())))
                .build();
    }
}
