package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class BookCategoryMapper {

    private static final Set<String> BLOCK_CATEGORIES = Set.of(
            "수험서/자격증",
            "대학교재",
            "초/중/고 참고서",
            "어린이/유아",
            "전집/중고전집",
            "잡지",
            "달력/기타 굿즈"
    );

    // category가 "어린이"처럼 더 쪼개져 오는 상황 대비
    private static boolean isBlocked(String category) {
        if (category == null || category.isBlank()) return false;
        String c = category.trim();

        // 완전 일치(루트가 그대로 오는 케이스)
        if (BLOCK_CATEGORIES.contains(c)) return true;

        // 한 단어로 오는 하위/동의어 케이스
        return containsAny(c, "수험서", "자격증", "대학교재", "참고서", "어린이", "유아", "전집", "중고전집", "잡지", "달력", "굿즈");
    }

    private static boolean containsAny(String s, String... keys) {
        for (String k : keys) if (s.contains(k)) return true;
        return false;
    }

    public Optional<CustomCategory> mapCategory(String category) {
        if (category == null || category.isBlank()) return Optional.of(CustomCategory.ETC);

        // 특정 분야 검색 차단
        if (isBlocked(category)) return Optional.empty();

        if (containsAny(category, "경제", "경영")) return Optional.of(CustomCategory.ECON_BIZ);
        if (containsAny(category, "과학", "컴퓨터", "모바일", "IT")) return Optional.of(CustomCategory.SCI_IT);
        if (containsAny(category, "소설", "희곡", "장르소설", "소설/시/희곡")) return Optional.of(CustomCategory.NOVEL_GENRE);
        if (containsAny(category, "시", "에세이")) return Optional.of(CustomCategory.POEM_ESSAY);
        if (containsAny(category, "가정", "취미", "여행", "요리", "살림", "건강")) return Optional.of(CustomCategory.HOME_HOBBY);
        if (containsAny(category, "예술", "대중문화", "음악", "미술", "영화")) return Optional.of(CustomCategory.ART_CULTURE);
        if (containsAny(category, "인문학", "역사")) return Optional.of(CustomCategory.HUMAN_HISTORY);
        if (containsAny(category, "자기계발")) return Optional.of(CustomCategory.SELF_DEV);
        if (containsAny(category, "사회과학", "정치", "사회")) return Optional.of(CustomCategory.POL_SOC);

        return Optional.of(CustomCategory.ETC);
    }
}
