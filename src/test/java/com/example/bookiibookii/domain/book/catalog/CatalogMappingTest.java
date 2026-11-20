package com.example.bookiibookii.domain.book.catalog;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.domain.book.service.LibraryCategoryMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogMappingTest {
    private final LibraryCategoryMapper categories = new LibraryCategoryMapper();

    @ParameterizedTest
    @CsvSource({"813.7,KOREAN_NOVEL", "843,WORLD_NOVEL", "811.6,POETRY_ESSAY", "814,POETRY_ESSAY",
            "842,PLAY_LITERATURE", "004.73,SCIENCE_IT", "420,SCIENCE_IT", "598.1,HOME_HOBBY",
            "325.04,ECONOMY_BUSINESS", "325.211,SELF_DEVELOPMENT", "340,POLITICS_SOCIETY", "651,ART_CULTURE",
            "892.83,WORLD_NOVEL", "892.81,LITERATURE_ETC", "001.3,HUMANITIES_HISTORY",
            "001,NON_LITERATURE_ETC", "911,HUMANITIES_HISTORY", "800,LITERATURE_ETC", "892.8,LITERATURE_ETC", "invalid,NON_LITERATURE_ETC"})
    void mapsSubjectCodesIncludingUncertainGenres(String kdc, CustomCategory expected) {
        assertThat(categories.catalogCategory(kdc)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"대학교재", "유아", "참고서", "전집", "수험서", "잡지", "전문서적", "아동", "학습참고서"})
    void retainsExplicitBlockedClasses(String classification) {
        var book = new CatalogBook("", "", "", "", "", "", null, "", "", "", classification, "");
        assertThat(categories.blockedInCatalog(book)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"홍길동 (지은이), 김철수", "홍길동 지음", "저자 : 홍길동 ; 이작가", "글：홍길동"})
    void normalizesFirstAuthor(String input) {
        assertThat(new BookAuthorMapper().mapFirstWriterOnly(input)).isEqualTo("홍길동");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "홍길동，김철수 | 홍길동",
            "홍길동；김철수 | 홍길동",
            "Jane Austen, Virginia Woolf | Jane Austen",
            "지은이: Austen, Jane (지은이), 김철수 | Austen, Jane",
            "Tolkien, J. R. R.; 다른 저자 | Tolkien, J. R. R."
    })
    void distinguishesAuthorListsFromInvertedNames(String input, String expected) {
        assertThat(new BookAuthorMapper().mapFirstWriterOnly(input)).isEqualTo(expected);
    }
}
