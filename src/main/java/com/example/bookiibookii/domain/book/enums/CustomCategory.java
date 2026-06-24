package com.example.bookiibookii.domain.book.enums;

public enum CustomCategory {

    // 조회용
    ALL(null, "전체"),
    LITERATURE_ALL(CategoryGroup.LITERATURE, "문학"),
    NON_LITERATURE_ALL(CategoryGroup.NON_LITERATURE, "비문학"),

    // 문학
    KOREAN_NOVEL(CategoryGroup.LITERATURE, "한국소설"),
    WORLD_NOVEL(CategoryGroup.LITERATURE, "세계소설"),
    GENRE_NOVEL(CategoryGroup.LITERATURE, "장르소설"),
    ROMANCE(CategoryGroup.LITERATURE, "로맨스"),
    HISTORICAL_NOVEL(CategoryGroup.LITERATURE, "역사소설"),
    POETRY_ESSAY(CategoryGroup.LITERATURE, "시/에세이"),
    PLAY_LITERATURE(CategoryGroup.LITERATURE, "희곡/문학"),
    LITERATURE_ETC(CategoryGroup.LITERATURE, "기타"),

    // 비문학
    ECONOMY_BUSINESS(CategoryGroup.NON_LITERATURE, "경제/경영"),
    SCIENCE_IT(CategoryGroup.NON_LITERATURE, "과학/IT"),
    HUMANITIES_HISTORY(CategoryGroup.NON_LITERATURE, "인문/역사"),
    HOME_HOBBY(CategoryGroup.NON_LITERATURE, "가정/취미"),
    ART_CULTURE(CategoryGroup.NON_LITERATURE, "예술/문화"),
    SELF_DEVELOPMENT(CategoryGroup.NON_LITERATURE, "자기계발"),
    POLITICS_SOCIETY(CategoryGroup.NON_LITERATURE, "정치/사회"),
    NON_LITERATURE_ETC(CategoryGroup.NON_LITERATURE, "기타");

    private final CategoryGroup group;
    private final String label;

    CustomCategory(CategoryGroup group, String label) {
        this.group = group;
        this.label = label;
    }

    public CategoryGroup getGroup() {
        return group;
    }

    public String getLabel() {
        return label;
    }
}