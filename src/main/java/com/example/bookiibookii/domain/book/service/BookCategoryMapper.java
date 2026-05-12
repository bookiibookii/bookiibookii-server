package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private static final Map<Long, CustomCategory> CID_CATEGORY_MAP = Map.<Long, CustomCategory>ofEntries(
            // 문학
            Map.entry(50917L, CustomCategory.KOREAN_NOVEL),      // 한국소설
            Map.entry(50925L, CustomCategory.WORLD_NOVEL),       // 세계의 소설
            Map.entry(112011L, CustomCategory.GENRE_NOVEL),      // 장르소설
            Map.entry(50935L, CustomCategory.ROMANCE),           // 로맨스소설
            Map.entry(50929L, CustomCategory.HISTORICAL_NOVEL),  // 역사소설
            Map.entry(50940L, CustomCategory.POETRY_ESSAY),      // 시
            Map.entry(55889L, CustomCategory.POETRY_ESSAY),      // 에세이
            Map.entry(50948L, CustomCategory.PLAY_LITERATURE),   // 희곡

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

    public Optional<CustomCategory> mapCategory(Long aladinCategoryId) {
        if (aladinCategoryId == null) {
            return Optional.of(CustomCategory.NON_LITERATURE_ETC);
        }

        Long currentCid = aladinCategoryId;
        Set<Long> visited = new HashSet<>();

        while (currentCid != null && visited.add(currentCid)) {
            if (BLOCK_ROOT_CIDS.contains(currentCid)) {
                return Optional.empty();
            }

            CustomCategory mapped = CID_CATEGORY_MAP.get(currentCid);
            if (mapped != null) {
                return Optional.of(mapped);
            }

            currentCid = aladinCategoryTree.getParentCid(currentCid).orElse(null);
        }

        return Optional.of(CustomCategory.NON_LITERATURE_ETC);
    }
}