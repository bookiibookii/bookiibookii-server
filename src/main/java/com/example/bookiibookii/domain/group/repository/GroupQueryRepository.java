package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.example.bookiibookii.domain.book.entity.QBook.book;
import static com.example.bookiibookii.domain.group.entity.QGroupTag.groupTag;
import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.tag.entity.QTag.tag;
import static com.example.bookiibookii.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class GroupQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Slice<Groups> findGroupsByFilters(GroupRequestDTO.FilterDTO filter, List<Long> userTagIds, Pageable pageable) {

        List<Groups> content = queryFactory
                .selectFrom(groups)
                .join(groups.book, book).fetchJoin() // 도서 정보 페치 조인
                .join(groups.host, user).fetchJoin() // 호스트 정보 페치 조인
                .leftJoin(groups.groupTags, groupTag) // 추천 점수 계산을 위해 조인 (fetchJoin 아님)
                .where(
                        inGroupTypes(filter.groupTypes()),
                        inTradeTypes(filter.tradeTypes()),
                        containsRegions(filter.regions()),
                        inCategories(filter.categories()),
                        groups.groupStatus.eq(GroupStatus.RECRUITING)
                )
                .groupBy(groups.groupId)
                .orderBy(getSortOrder(filter.sort(), userTagIds))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 정렬 조건 생성 (추천순/인기순/최신순)
    private OrderSpecifier<?>[] getSortOrder(String sort, List<Long> userTagIds) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        // 추천순(usertag 기반 누적 추천?) 고도화 필요(현재 책 카테고리만 적용)
        if ("RECOMMEND".equals(sort) && userTagIds != null && !userTagIds.isEmpty()) {
            // 💡 석진님이 보여주신 JPA Repo의 COUNT(gt) 로직을 QueryDSL로 구현한 부분입니다.
            NumberExpression<Long> matchCount = new CaseBuilder()
                    .when(groupTag.tag.id.in(userTagIds)).then(1L)
                    .otherwise(0L)
                    .sum(); // 일치할 때마다 1점씩 더해서 총점을 계산

            orders.add(new OrderSpecifier<>(Order.DESC, matchCount));
        }

        // 2순위: 인기순(신청자 수)
        if ("POPULAR".equals(sort)) {
            orders.add(new OrderSpecifier<>(Order.DESC, groups.applications.size()));
        }

        //최신순
        orders.add(new OrderSpecifier<>(Order.DESC, groups.createdAt));

        return orders.toArray(new OrderSpecifier[0]);
    }

   //지역 필터 (List<String>을 OR 조건으로 묶어 contains 처리)
    private BooleanExpression containsRegions(List<String> regions) {
        if (regions == null || regions.isEmpty()) return null;

        return regions.stream()
                .map(region -> {
                    // "서울시 전체" 버튼 대응 로직
                    if (region.contains("전체")) {
                        // "서울시 전체" -> "서울", "경기도 전체" -> "경기"
                        String cleanRegion = region.replace("전체", "").trim();
                        return cleanRegion.length() >= 2 ? cleanRegion.substring(0, 2) : cleanRegion;
                    }
                    return region;
                })
                .map(user.region::contains) // 정제된 키워드("서울")가 포함된 모든 주소 검색
                .reduce(BooleanExpression::or)
                .orElse(null);
    }

    private BooleanExpression inGroupTypes(List<GroupType> types) {
        return (types == null || types.isEmpty()) ? null : groups.groupType.in(types);
    }

    private BooleanExpression inTradeTypes(List<TradeType> types) {
        return (types == null || types.isEmpty()) ? null : groups.tradeType.in(types);
    }

    private BooleanExpression inCategories(List<CustomCategory> categories) {
        return (categories == null || categories.isEmpty()) ? null : book.category.in(categories);
    }
}