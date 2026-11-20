package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.book.catalog.CatalogBook;
import com.example.bookiibookii.domain.book.enums.CategoryGroup;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LibraryCategoryMapper {
    private static final Pattern DECIMAL_CODE = Pattern.compile("[0-9]{3}(?:\\.[0-9]+)?");
    private static final Pattern RESTRICTED_AUDIENCE = Pattern.compile("[5679](?:[0-9]{4})?");
    private static final Set<String> RESTRICTED_LABELS = Set.of(
            "대학교재", "전문서적", "수험서", "자격증", "학습참고서", "참고서", "아동", "전집");

    public boolean blockedInCatalog(CatalogBook book) {
        if (RESTRICTED_AUDIENCE.matcher(book.additionSymbol()).matches()
                || BookCategoryMapper.isBlockedCategoryName(book.classification())) return true;
        for (String label : book.classification().split(">")) {
            if (RESTRICTED_LABELS.contains(label.strip())) return true;
        }
        return false;
    }

    // KDC는 주제 분류다. 813.7만으로 로맨스나 역사소설 등의 세부 장르를 추정하지 않는다.
    public CustomCategory catalogCategory(String kdc) {
        if (!DECIMAL_CODE.matcher(kdc).matches()) return CustomCategory.NON_LITERATURE_ETC;
        if (kdc.startsWith("813")) return CustomCategory.KOREAN_NOVEL;
        // 892.8은 러시아 문학 전반이며 892.83에서 소설로 좁혀진다.
        if (kdc.equals("892.83")) return CustomCategory.WORLD_NOVEL;
        if (kdc.equals("001.3")) return CustomCategory.HUMANITIES_HISTORY;
        int code = Integer.parseInt(kdc.substring(0, 3));
        if (code >= 810 && code < 890) {
            return switch (code % 10) {
                case 3 -> CustomCategory.WORLD_NOVEL;
                case 1, 4 -> CustomCategory.POETRY_ESSAY;
                case 2 -> CustomCategory.PLAY_LITERATURE;
                default -> CustomCategory.LITERATURE_ETC;
            };
        }
        if (kdc.startsWith("325.211")) return CustomCategory.SELF_DEVELOPMENT;
        if (kdc.startsWith("32")) return CustomCategory.ECONOMY_BUSINESS;
        if (kdc.startsWith("59") || kdc.startsWith("980.2")) return CustomCategory.HOME_HOBBY;
        if (code == 4 || code == 5) return CustomCategory.SCIENCE_IT;
        return switch (kdc.charAt(0)) {
            case '1', '2', '7', '9' -> CustomCategory.HUMANITIES_HISTORY;
            case '3' -> CustomCategory.POLITICS_SOCIETY;
            case '4', '5' -> CustomCategory.SCIENCE_IT;
            case '6' -> CustomCategory.ART_CULTURE;
            case '8' -> CustomCategory.LITERATURE_ETC;
            default -> CustomCategory.NON_LITERATURE_ETC;
        };
    }

    public CustomCategory registrationCategory(String kdc, List<CatalogBook> national) {
        if (!kdc.isBlank()) return catalogCategory(kdc);
        var candidates = national.stream().map(CatalogBook::ddc)
                .filter(ddc -> !ddc.isBlank()).map(this::fromDdc).distinct().toList();
        if (candidates.size() == 1) return candidates.get(0);
        if (!candidates.isEmpty() && candidates.stream().allMatch(category ->
                category.getGroup() == CategoryGroup.LITERATURE)) return CustomCategory.LITERATURE_ETC;
        return CustomCategory.NON_LITERATURE_ETC;
    }

    private CustomCategory fromDdc(String ddc) {
        if (!DECIMAL_CODE.matcher(ddc).matches()) return CustomCategory.NON_LITERATURE_ETC;
        // DDC 813은 미국소설이고 KDC 813은 한국소설이다.
        if (ddc.startsWith("909")) return CustomCategory.HUMANITIES_HISTORY;
        return switch (ddc) {
            case "895.73" -> CustomCategory.KOREAN_NOVEL;
            case "813", "823", "891.73" -> CustomCategory.WORLD_NOVEL;
            default -> ddc.startsWith("8") ? CustomCategory.LITERATURE_ETC : CustomCategory.NON_LITERATURE_ETC;
        };
    }
}
