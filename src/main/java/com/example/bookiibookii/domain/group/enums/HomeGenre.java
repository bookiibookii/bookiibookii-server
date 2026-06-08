package com.example.bookiibookii.domain.group.enums;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum HomeGenre {
    KOREAN_NOVEL("한국소설", List.of(CustomCategory.KOREAN_NOVEL)),
    WORLD_NOVEL("세계소설", List.of(CustomCategory.WORLD_NOVEL)),
    GENRE_NOVEL("장르소설", List.of(CustomCategory.GENRE_NOVEL)),
    ROMANCE("로맨스", List.of(CustomCategory.ROMANCE)),
    HISTORICAL_NOVEL("역사소설", List.of(CustomCategory.HISTORICAL_NOVEL)),
    POETRY_ESSAY("시·에세이", List.of(CustomCategory.POETRY_ESSAY)),
    PLAY_LITERATURE("희곡·문학", List.of(CustomCategory.PLAY_LITERATURE)),
    ECONOMY_BUSINESS("경제·경영", List.of(CustomCategory.ECONOMY_BUSINESS)),
    SCIENCE_IT("과학·IT", List.of(CustomCategory.SCIENCE_IT)),
    HOME_HOBBY("가정·취미", List.of(CustomCategory.HOME_HOBBY)),
    ART_CULTURE("예술·문화", List.of(CustomCategory.ART_CULTURE)),
    HUMANITIES_HISTORY("인문·역사", List.of(CustomCategory.HUMANITIES_HISTORY)),
    SELF_DEVELOPMENT("자기계발", List.of(CustomCategory.SELF_DEVELOPMENT)),
    POLITICS_SOCIETY("정치·사회", List.of(CustomCategory.POLITICS_SOCIETY)),
    ETC("기타", List.of(
            CustomCategory.LITERATURE_ETC,
            CustomCategory.NON_LITERATURE_ETC
    ));

    private final String label;
    private final List<CustomCategory> categories;

    public static Optional<HomeGenre> from(CustomCategory category) {
        return Arrays.stream(values())
                .filter(genre -> genre.categories.contains(category))
                .findFirst();
    }
}
