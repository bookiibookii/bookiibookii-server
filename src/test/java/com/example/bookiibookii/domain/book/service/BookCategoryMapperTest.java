package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCategoryMapperTest {

    @Mock
    private AladinCategoryTree aladinCategoryTree;

    @ParameterizedTest
    @MethodSource("literatureCategoryNames")
    void mapsLiteratureCategoryNameBeforeCid(
            String categoryName,
            CustomCategory expected
    ) {
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                999999L,
                categoryName,
                "9780000000001",
                "문학 책"
        );

        assertThat(result).contains(expected);
    }

    @Test
    void mapsKoreanScienceFictionChildCidWithoutCategoryName() {
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                89482L,
                null,
                "9780000000002",
                "한국 과학소설"
        );

        assertThat(result).contains(CustomCategory.KOREAN_NOVEL);
    }

    @Test
    void fallsBackOnlyWhenNameAndCidAreUnknown() {
        when(aladinCategoryTree.getParentCid(999999L)).thenReturn(Optional.empty());
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                999999L,
                "국내도서>알 수 없는 분류",
                "9780000000003",
                "미분류 책"
        );

        assertThat(result).contains(CustomCategory.NON_LITERATURE_ETC);
    }

    @Test
    void doesNotTreatNonLiteratureKeywordContainingClassicAsLiterature() {
        when(aladinCategoryTree.getParentCid(51024L)).thenReturn(Optional.of(987L));
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                51024L,
                "국내도서>과학>물리학>고전물리학",
                "9780000000006",
                "고전역학"
        );

        assertThat(result).contains(CustomCategory.SCIENCE_IT);
    }

    @Test
    void excludesBlockedCategoryBeforeLiteratureMapping() {
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                50917L,
                "국내도서>잡지>문학>한국소설",
                "9780000000004",
                "차단 문학 잡지"
        );

        assertThat(result).isEmpty();
    }

    @Test
    void excludesChildCidOfBlockedCategory() {
        when(aladinCategoryTree.getParentCid(34582L)).thenReturn(Optional.of(1383L));
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                34582L,
                "국내도서>분류명 누락",
                "9780000000005",
                "수험서"
        );

        assertThat(result).isEmpty();
    }

    @ParameterizedTest(name = "{0} -> {4}")
    @MethodSource("actualAladinSamples")
    void mapsActualAladinSamples(
            String title,
            String isbn13,
            Long categoryId,
            String categoryName,
            CustomCategory expected
    ) {
        BookCategoryMapper mapper = new BookCategoryMapper(aladinCategoryTree);

        Optional<CustomCategory> result = mapper.mapCategory(
                categoryId,
                categoryName,
                isbn13,
                title
        );

        assertThat(result).contains(expected);
    }

    private static Stream<Arguments> literatureCategoryNames() {
        return Stream.of(
                Arguments.of("국내도서>소설/시/희곡", CustomCategory.LITERATURE_ETC),
                Arguments.of("국내도서>소설/시/희곡>한국소설", CustomCategory.KOREAN_NOVEL),
                Arguments.of("국내도서>소설/시/희곡>외국소설", CustomCategory.WORLD_NOVEL),
                Arguments.of("국내도서>소설/시/희곡>시>한국시", CustomCategory.POETRY_ESSAY),
                Arguments.of("국내도서>소설/시/희곡>희곡>한국희곡", CustomCategory.PLAY_LITERATURE),
                Arguments.of("국내도서>에세이>독서에세이", CustomCategory.POETRY_ESSAY),
                Arguments.of("국내도서>장르소설>판타지", CustomCategory.GENRE_NOVEL),
                Arguments.of("국내도서>고전>서양고전문학", CustomCategory.LITERATURE_ETC)
        );
    }

    private static Stream<Arguments> actualAladinSamples() {
        return Stream.of(
                Arguments.of(
                        "위대한 개츠비",
                        "9788937460753",
                        50919L,
                        "국내도서>소설/시/희곡>영미소설",
                        CustomCategory.WORLD_NOVEL
                ),
                Arguments.of(
                        "죽은 자의 스토킹",
                        "9791124468197",
                        51067L,
                        "국내도서>소설/시/희곡>추리/미스터리소설>기타국가 추리/미스터리소설",
                        CustomCategory.WORLD_NOVEL
                ),
                Arguments.of(
                        "마션 - 스페셜 에디션",
                        "9788925588650",
                        89481L,
                        "국내도서>소설/시/희곡>과학소설(SF)>외국 과학소설",
                        CustomCategory.WORLD_NOVEL
                ),
                Arguments.of(
                        "소년이 온다 - 2024 노벨문학상 수상작가",
                        "9788936434120",
                        50993L,
                        "국내도서>소설/시/희곡>한국소설>2000년대 이후 한국소설",
                        CustomCategory.KOREAN_NOVEL
                ),
                Arguments.of(
                        "여행의 이유 (개정증보판)",
                        "9791191114591",
                        51371L,
                        "국내도서>에세이>한국에세이",
                        CustomCategory.POETRY_ESSAY
                ),
                // 고전으로 널리 분류되는 책이지만 알라딘의 실제 경로가 독일소설이므로 WORLD_NOVEL이다.
                Arguments.of(
                        "싯다르타",
                        "9788937460586",
                        50922L,
                        "국내도서>소설/시/희곡>독일소설",
                        CustomCategory.WORLD_NOVEL
                )
        );
    }
}
