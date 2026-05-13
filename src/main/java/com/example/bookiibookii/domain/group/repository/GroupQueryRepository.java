package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.example.bookiibookii.domain.book.entity.QBook.book;
import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class GroupQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Slice<Groups> findGroupsByFilters(GroupRequestDTO.FilterDTO filter, Pageable pageable) {

        List<Groups> content = queryFactory
                .selectFrom(groups)
                .join(groups.book, book).fetchJoin() // 도서 정보 페치 조인
                .join(groups.host, user).fetchJoin() // 호스트 정보 페치 조인
                .where(
                        inTradeTypes(filter.tradeTypes()),
                        containsRegions(filter.regions()),
                        inCategories(filter.categories()),
                        groups.groupStatus.eq(GroupStatus.RECRUITING)
                )
                .groupBy(groups.groupId)
                .orderBy(getSortOrder(filter.sort()))
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

    private OrderSpecifier<?>[] getSortOrder(GroupSortType sort) {
        return new OrderSpecifier[]{new OrderSpecifier<>(Order.DESC, groups.createdAt)};
    }

    private BooleanExpression containsRegions(List<String> regions) {
        if (regions == null || regions.isEmpty()) return null;
        return regions.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(groups.preferRegion::contains)
                .reduce(BooleanExpression::or)
                .orElse(null);
    }

    private BooleanExpression inTradeTypes(List<TradeType> types) {
        return (types == null || types.isEmpty()) ? null : groups.tradeType.in(types);
    }

    private BooleanExpression inCategories(List<CustomCategory> categories) {
        return (categories == null || categories.isEmpty()) ? null : book.category.in(categories);
    }

    //검색
    public Page<Groups> searchGroupsByKeyword(String searchword, GroupSortType sort, Pageable pageable) {
        // 1. 데이터 조회 (JOIN FETCH로 N+1 방지)
        List<Groups> content = queryFactory
                .selectFrom(groups)
                .join(groups.book, book).fetchJoin()
                .join(groups.host, user).fetchJoin()
                .where(
                        searchwordContains(searchword),
                        groups.groupStatus.eq(GroupStatus.RECRUITING)
                )
                .groupBy(groups.groupId) // 태그 조인으로 인한 중복 제거
                .orderBy(getSearchSortOrder(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 검색 결과 총 개수 (SearchResultDTO 전용)
        Long totalCount = queryFactory
                .select(groups.countDistinct())
                .from(groups)
                .join(groups.book, book)
                .where(searchwordContains(searchword), groups.groupStatus.eq(GroupStatus.RECRUITING))
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount != null ? totalCount : 0L);
    }

    private BooleanExpression searchwordContains(String searchword) {
        if (searchword == null || searchword.isBlank()) return null;

        return book.title.containsIgnoreCase(searchword)
                .or(book.author.containsIgnoreCase(searchword));

    }

    private OrderSpecifier<?> getSearchSortOrder(GroupSortType sort) {
        return groups.createdAt.desc();
    }
}