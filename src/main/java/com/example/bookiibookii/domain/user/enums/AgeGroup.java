package com.example.bookiibookii.domain.user.enums;

import java.time.LocalDate;
import java.time.Period;

public enum AgeGroup {
    TEENS,            // 10대
    TWENTIES,         // 20대
    THIRTIES,         // 30대
    FORTIES_AND_ABOVE // 40대 이상
    ;

    public static AgeGroup from(LocalDate birth) {
        int age = Period.between(birth, LocalDate.now()).getYears();
        if (age < 20) return TEENS;
        if (age < 30) return TWENTIES;
        if (age < 40) return THIRTIES;
        return FORTIES_AND_ABOVE;
    }
}
