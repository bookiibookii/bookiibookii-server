package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookCategoryMapper {

    private final AladinCategoryTree aladinCategoryTree;

    private static final Set<Long> BLOCK_ROOT_CIDS = Set.of(
            // 수험서/자격증
            1383L,

            // 대학교재/전문서적
            8257L,

            // 초/중/고 참고서
            50246L, // 초등학교참고서
            76000L, // 중학교참고서
            76001L, // 고등학교참고서

            // 어린이/유아
            1108L,  // 어린이
            13789L, // 유아

            // 전집
            17195L, // 전집/중고전집

            // 잡지
            2913L,  // 잡지
            50951L  // 문학 잡지
    );

    private static final Set<String> BLOCKED_CATEGORY_NAMES = Set.of(
            "수험서/자격증",
            "대학교재/전문서적",
            "초등학교참고서",
            "중학교참고서",
            "고등학교참고서",
            "어린이",
            "유아",
            "전집/중고전집",
            "잡지"
    );

    private static final Map<Long, CustomCategory> CID_CATEGORY_MAP = Map.<Long, CustomCategory>ofEntries(
            // 문학
            Map.entry(1L, CustomCategory.LITERATURE_ETC),         // 소설/시/희곡
            Map.entry(50917L, CustomCategory.KOREAN_NOVEL),      // 한국소설
            Map.entry(89482L, CustomCategory.KOREAN_NOVEL),      // 한국 과학소설
            Map.entry(51065L, CustomCategory.KOREAN_NOVEL),      // 한국 추리/미스터리소설
            Map.entry(50925L, CustomCategory.WORLD_NOVEL),       // 세계의 소설
            Map.entry(50919L, CustomCategory.WORLD_NOVEL),       // 영미소설
            Map.entry(50922L, CustomCategory.WORLD_NOVEL),       // 독일소설
            Map.entry(52650L, CustomCategory.WORLD_NOVEL),       // 러시아소설
            Map.entry(50918L, CustomCategory.WORLD_NOVEL),       // 일본소설
            Map.entry(50923L, CustomCategory.WORLD_NOVEL),       // 중국소설
            Map.entry(50921L, CustomCategory.WORLD_NOVEL),       // 프랑스소설
            Map.entry(50920L, CustomCategory.WORLD_NOVEL),       // 스페인/중남미소설
            Map.entry(89481L, CustomCategory.WORLD_NOVEL),       // 외국 과학소설
            Map.entry(51067L, CustomCategory.WORLD_NOVEL),       // 기타국가 추리/미스터리소설
            Map.entry(51062L, CustomCategory.WORLD_NOVEL),       // 영미 추리/미스터리소설
            Map.entry(51058L, CustomCategory.WORLD_NOVEL),       // 일본 추리/미스터리소설
            Map.entry(112011L, CustomCategory.GENRE_NOVEL),      // 장르소설
            Map.entry(50935L, CustomCategory.ROMANCE),           // 로맨스소설
            Map.entry(50929L, CustomCategory.HISTORICAL_NOVEL),  // 역사소설
            Map.entry(50940L, CustomCategory.POETRY_ESSAY),      // 시
            Map.entry(55889L, CustomCategory.POETRY_ESSAY),      // 에세이
            Map.entry(50948L, CustomCategory.PLAY_LITERATURE),   // 희곡
            Map.entry(2105L, CustomCategory.LITERATURE_ETC),     // 고전
            Map.entry(90842L, CustomCategory.LITERATURE_ETC),    // 외국도서 소설/시/희곡
            Map.entry(90845L, CustomCategory.POETRY_ESSAY),      // 외국도서 에세이
            Map.entry(38396L, CustomCategory.LITERATURE_ETC),    // 전자책 소설/시/희곡
            Map.entry(38414L, CustomCategory.LITERATURE_ETC),    // 전자책 고전
            Map.entry(56387L, CustomCategory.POETRY_ESSAY),      // 전자책 에세이
            Map.entry(112013L, CustomCategory.GENRE_NOVEL),      // 전자책 장르소설

            // 비문학
            Map.entry(170L, CustomCategory.ECONOMY_BUSINESS),    // 경제경영

            Map.entry(987L, CustomCategory.SCIENCE_IT),          // 과학
            Map.entry(351L, CustomCategory.SCIENCE_IT),          // 컴퓨터/모바일

            Map.entry(656L, CustomCategory.HUMANITIES_HISTORY),  // 인문학
            Map.entry(74L, CustomCategory.HUMANITIES_HISTORY),   // 역사

            Map.entry(1230L, CustomCategory.HOME_HOBBY),         // 가정/요리/뷰티
            Map.entry(55890L, CustomCategory.HOME_HOBBY),        // 건강/취미/레저
            Map.entry(1196L, CustomCategory.HOME_HOBBY),         // 여행

            Map.entry(517L, CustomCategory.ART_CULTURE),         // 예술/대중문화

            Map.entry(336L, CustomCategory.SELF_DEVELOPMENT),    // 자기계발

            Map.entry(798L, CustomCategory.POLITICS_SOCIETY),    // 사회과학
            Map.entry(51016L, CustomCategory.POLITICS_SOCIETY),  // 정치학/외교학/행정학
            Map.entry(51046L, CustomCategory.POLITICS_SOCIETY)   // 법과 생활
    );

    public Optional<CustomCategory> mapCategory(
            Long aladinCategoryId,
            String categoryName,
            String isbn13,
            String title
    ) {
        if (isBlockedCategoryName(categoryName) || isBlockedCategoryId(aladinCategoryId)) {
            return Optional.empty();
        }

        Optional<CustomCategory> nameMapped = mapByCategoryName(categoryName);
        if (nameMapped.isPresent()) {
            return nameMapped;
        }

        Optional<CustomCategory> cidMapped = mapByCategoryId(aladinCategoryId);
        if (cidMapped.isPresent()) {
            return cidMapped;
        }

        log.debug(
                "알라딘 카테고리 매핑 실패로 기타 분류 categoryId={}, categoryName={}, isbn={}, title={}",
                aladinCategoryId,
                categoryName,
                isbn13,
                title
        );
        return Optional.of(CustomCategory.NON_LITERATURE_ETC);
    }

    private Optional<CustomCategory> mapByCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return Optional.empty();
        }

        String[] path = Arrays.stream(categoryName.split(">"))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .toArray(String[]::new);

        if (contains(path, "한국소설")) {
            return Optional.of(CustomCategory.KOREAN_NOVEL);
        }
        if (containsAny(path, "세계의 소설", "세계소설", "외국소설")) {
            return Optional.of(CustomCategory.WORLD_NOVEL);
        }
        if (contains(path, "로맨스")) {
            return Optional.of(CustomCategory.ROMANCE);
        }
        if (contains(path, "역사소설")) {
            return Optional.of(CustomCategory.HISTORICAL_NOVEL);
        }
        if (contains(path, "장르소설")) {
            return Optional.of(CustomCategory.GENRE_NOVEL);
        }
        if (isKoreanNovelPath(path)) {
            return Optional.of(CustomCategory.KOREAN_NOVEL);
        }
        if (isWorldNovelPath(path)) {
            return Optional.of(CustomCategory.WORLD_NOVEL);
        }
        if (containsOutsideLiteratureRoot(path, "희곡")) {
            return Optional.of(CustomCategory.PLAY_LITERATURE);
        }
        if (contains(path, "에세이") || hasExactSegment(path, "시")) {
            return Optional.of(CustomCategory.POETRY_ESSAY);
        }
        if (containsAny(path, "소설", "문학")
                || hasExactSegment(path, "고전")
                || contains(path, "고전문학")) {
            return Optional.of(CustomCategory.LITERATURE_ETC);
        }

        return Optional.empty();
    }

    private Optional<CustomCategory> mapByCategoryId(Long aladinCategoryId) {
        if (aladinCategoryId == null) {
            return Optional.empty();
        }

        Long currentCid = aladinCategoryId;
        Set<Long> visited = new HashSet<>();

        while (currentCid != null && visited.add(currentCid)) {
            CustomCategory mapped = CID_CATEGORY_MAP.get(currentCid);
            if (mapped != null) {
                return Optional.of(mapped);
            }

            currentCid = aladinCategoryTree.getParentCid(currentCid).orElse(null);
        }

        return Optional.empty();
    }

    private boolean isBlockedCategoryId(Long aladinCategoryId) {
        if (aladinCategoryId == null) {
            return false;
        }

        Long currentCid = aladinCategoryId;
        Set<Long> visited = new HashSet<>();
        while (currentCid != null && visited.add(currentCid)) {
            if (BLOCK_ROOT_CIDS.contains(currentCid)) {
                return true;
            }
            currentCid = aladinCategoryTree.getParentCid(currentCid).orElse(null);
        }
        return false;
    }

    private boolean isBlockedCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return false;
        }
        return Arrays.stream(categoryName.split(">"))
                .map(String::trim)
                .anyMatch(segment -> BLOCKED_CATEGORY_NAMES.stream().anyMatch(segment::contains));
    }

    private boolean contains(String[] path, String keyword) {
        return Arrays.stream(path).anyMatch(segment -> segment.contains(keyword));
    }

    private boolean containsAny(String[] path, String... keywords) {
        return Arrays.stream(keywords).anyMatch(keyword -> contains(path, keyword));
    }

    private boolean hasExactSegment(String[] path, String expected) {
        return Arrays.stream(path).anyMatch(expected::equals);
    }

    private boolean containsOutsideLiteratureRoot(String[] path, String keyword) {
        return Arrays.stream(path)
                .filter(segment -> !"소설/시/희곡".equals(segment))
                .anyMatch(segment -> segment.contains(keyword));
    }

    private boolean isKoreanNovelPath(String[] path) {
        return Arrays.stream(path)
                .anyMatch(segment -> segment.contains("한국") && segment.contains("소설"));
    }

    private boolean isWorldNovelPath(String[] path) {
        return Arrays.stream(path)
                .anyMatch(segment ->
                        segment.contains("소설")
                                && containsAnyKeyword(
                                segment,
                                "세계",
                                "외국",
                                "기타국가",
                                "기타 국가",
                                "영미",
                                "독일",
                                "러시아",
                                "일본",
                                "중국",
                                "프랑스",
                                "스페인",
                                "중남미"
                        )
                );
    }

    private boolean containsAnyKeyword(String value, String... keywords) {
        return Arrays.stream(keywords).anyMatch(value::contains);
    }
}
