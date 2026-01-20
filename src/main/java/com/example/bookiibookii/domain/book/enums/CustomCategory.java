package com.example.bookiibookii.domain.book.enums;

public enum CustomCategory {
    ECON_BIZ("경제/경영"),
    SCI_IT("과학/IT"),
    NOVEL_GENRE("소설/장르"),
    POEM_ESSAY("시/에세이"),
    HOME_HOBBY("가정/취미"),
    ART_CULTURE("예술/문화"),
    HUMAN_HISTORY("인문/역사"),
    SELF_DEV("자기계발"),
    POL_SOC("정치/사회"),
    ETC("기타");

    private final String label;
    CustomCategory(String label) { this.label = label; }
    public String label() { return label; }
}
