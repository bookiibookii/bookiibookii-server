package com.example.bookiibookii.domain.aladin.scheduler;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.entity.BestsellerIsbn;
import com.example.bookiibookii.domain.aladin.repository.BestsellerIsbnRepository;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.service.BookCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BestsellerSchedulerTest {

    @Mock
    private AladinClient aladinClient;

    @Mock
    private BestsellerIsbnRepository bestsellerIsbnRepository;

    @Mock
    private BookCategoryMapper bookCategoryMapper;

    @InjectMocks
    private BestsellerScheduler bestsellerScheduler;

    @BeforeEach
    void allowCategoriesByDefault() {
        lenient().when(bookCategoryMapper.mapCategory(
                any(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(Optional.of(CustomCategory.LITERATURE_ETC));
    }

    @Test
    void savesDisplayCacheAndKeepsEarlierRankingForDuplicatedIsbn13() {
        when(aladinClient.fetchBestsellers(10)).thenReturn(
                new AladinClient.AladinItemSearchResponse(
                        3,
                        List.of(
                                item("9780000000001", "1위 책", "저자1", "image1"),
                                item("9780000000001", "중복 책", "저자중복", "image-dup"),
                                item("9780000000002", "3위 책", "저자3", "image3")
                        )
                )
        );

        int savedCount = bestsellerScheduler.refreshBestsellers();

        ArgumentCaptor<List<BestsellerIsbn>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(bestsellerIsbnRepository).deleteAllInBatch();
        verify(bestsellerIsbnRepository).saveAllAndFlush(captor.capture());
        List<BestsellerIsbn> saved = captor.getValue();

        assertThat(savedCount).isEqualTo(2);
        assertThat(saved).extracting(BestsellerIsbn::getIsbn13)
                .containsExactly("9780000000001", "9780000000002");
        assertThat(saved).extracting(BestsellerIsbn::getRank)
                .containsExactly(1, 3);
        assertThat(saved.get(0).getTitle()).isEqualTo("1위 책");
        assertThat(saved.get(0).getAuthor()).isEqualTo("저자1");
        assertThat(saved.get(0).getBookImage()).isEqualTo("image1");
    }

    @Test
    void keepsExistingDataWhenAladinRequestFails() {
        when(aladinClient.fetchBestsellers(10))
                .thenThrow(new IllegalStateException("aladin unavailable"));

        assertThrows(IllegalStateException.class, bestsellerScheduler::refreshBestsellers);

        verify(bestsellerIsbnRepository, never()).deleteAllInBatch();
        verify(bestsellerIsbnRepository, never()).saveAllAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void keepsExistingDataWhenEveryResponseItemIsInvalid() {
        when(aladinClient.fetchBestsellers(10)).thenReturn(
                new AladinClient.AladinItemSearchResponse(
                        2,
                        List.of(
                                item(null, "제목", "저자", "image"),
                                item("9780000000001", "", "저자", "image")
                        )
                )
        );

        int savedCount = bestsellerScheduler.refreshBestsellers();

        assertThat(savedCount).isZero();
        verify(bestsellerIsbnRepository, never()).deleteAllInBatch();
        verify(bestsellerIsbnRepository, never()).saveAllAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void excludesBlockedCategoryBeforeReplacingBestsellers() {
        AladinClient.AladinBookItem blocked = item(
                "9780000000001",
                "차단 도서",
                "저자1",
                "image1",
                50951L,
                "국내도서>잡지>문학 잡지"
        );
        AladinClient.AladinBookItem allowed = item(
                "9780000000002",
                "허용 도서",
                "저자2",
                "image2",
                50917L,
                "국내도서>소설/시/희곡>한국소설"
        );
        when(aladinClient.fetchBestsellers(10)).thenReturn(
                new AladinClient.AladinItemSearchResponse(2, List.of(blocked, allowed))
        );
        when(bookCategoryMapper.mapCategory(
                eq(blocked.categoryId()),
                eq(blocked.categoryName()),
                eq(blocked.isbn13()),
                eq(blocked.title())
        )).thenReturn(Optional.empty());

        int savedCount = bestsellerScheduler.refreshBestsellers();

        ArgumentCaptor<List<BestsellerIsbn>> captor = ArgumentCaptor.forClass(List.class);
        verify(bestsellerIsbnRepository).saveAllAndFlush(captor.capture());
        assertThat(savedCount).isOne();
        assertThat(captor.getValue())
                .extracting(BestsellerIsbn::getIsbn13)
                .containsExactly("9780000000002");
    }

    private AladinClient.AladinBookItem item(
            String isbn13,
            String title,
            String author,
            String cover
    ) {
        return item(
                isbn13,
                title,
                author,
                cover,
                1L,
                "국내도서>소설/시/희곡"
        );
    }

    private AladinClient.AladinBookItem item(
            String isbn13,
            String title,
            String author,
            String cover,
            Long categoryId,
            String categoryName
    ) {
        return new AladinClient.AladinBookItem(
                title,
                author,
                cover,
                "출판사",
                "2026-06-08",
                isbn13,
                "link",
                categoryId,
                categoryName,
                new AladinClient.AladinBookItem.SubInfo(100)
        );
    }
}
