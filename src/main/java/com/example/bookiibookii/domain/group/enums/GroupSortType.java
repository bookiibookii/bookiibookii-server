package com.example.bookiibookii.domain.group.enums;

public enum GroupSortType {

    LATEST("최신순"),
    POPULAR("인기순"),
    RECOMMEND("추천순");

    private final String description;

    GroupSortType(String description) {
        this.description = description;
    }
}
