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

    public Slice<Groups> findGroupsByFilters(GroupRequestDTO.FilterDTO filter, List<CustomCategory> userInterests, Pageable pageable) {

        List<Groups> content = queryFactory
                .selectFrom(groups)
                // N+1 방지를 위해 fetchJoin 적용
                .join(groups.book, book).fetchJoin()
                .join(groups.host, user).fetchJoin()
                //.leftJoin(groups.groupTags, groupTag).fetchJoin()
                //.leftJoin(groupTag.tag, tag).fetchJoin()
                .where(
                        inGroupTypes(filter.groupTypes()),
                        inTradeTypes(filter.tradeTypes()),
                        containsRegions(filter.regions()),
                        inCategories(filter.categories()),
                        groups.groupStatus.ne(GroupStatus.DELETED) // 삭제된 그룹 제외
                )
                .orderBy(getSortOrder(filter.sort(), userInterests))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1) // Slice를 위해 +1 조회
                .fetch();

        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    // 정렬 조건 생성 (추천순/인기순/최신순)
    private OrderSpecifier<?>[] getSortOrder(String sort, List<CustomCategory> userInterests) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        // 추천순(usertag 기반 누적 추천?) 고도화 필요(현재 책 카테고리만 적용)
        if ("RECOMMEND".equals(sort) && userInterests != null && !userInterests.isEmpty()) {
            // 유저의 온보딩 태그 카테고리와 책의 카테고리가 같으면 1점, 아니면 0점
            NumberExpression<Integer> recommendationScore = new CaseBuilder()
                    .when(book.category.in(userInterests)).then(1)
                    .otherwise(0);

            // 점수 높은 순(DESC)으로 정렬하여 유저 취향 책을 상단으로!
            orders.add(new OrderSpecifier<>(Order.DESC, recommendationScore));
        }

        // 2순위: 인기순(신청자 수)
        if ("POPULAR".equals(sort)) {
            orders.add(new OrderSpecifier<>(Order.DESC, groups.applications.size()));
        }

        // 공통/기본: 최신순
        orders.add(new OrderSpecifier<>(Order.DESC, groups.createdAt));

        return orders.toArray(new OrderSpecifier[0]);
    }

   //지역 필터 (List<String>을 OR 조건으로 묶어 contains 처리)
    private BooleanExpression containsRegions(List<String> regions) {
        if (regions == null || regions.isEmpty()) return null;

        return regions.stream()
                .map(user.region::contains)
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