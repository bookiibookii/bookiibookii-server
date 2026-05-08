package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.user.enums.Tag;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
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

    public Slice<Groups> findGroupsByFilters(GroupRequestDTO.FilterDTO filter, List<Tag> userTags, Pageable pageable) {

        List<Groups> content = queryFactory
                .selectFrom(groups)
                .join(groups.book, book).fetchJoin() // 도서 정보 페치 조인
                .join(groups.host, user).fetchJoin() // 호스트 정보 페치 조인
                .where(
                        inTradeTypes(filter.tradeTypes()),
                        containsMeetPlaces(filter.meetPlace()),
                        inCategories(filter.categories()),
                        groups.groupStatus.eq(GroupStatus.RECRUITING)
                )
                .groupBy(groups.groupId)
                .orderBy(getSortOrder(filter.sort(), userTags))
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
    private OrderSpecifier<?>[] getSortOrder(GroupSortType sort, List<Tag> userTags) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        GroupSortType sortType = (sort != null) ? sort : GroupSortType.LATEST;

        // 추천순(usertag 기반 누적 추천?) 고도화 필요(현재 책 카테고리만 적용)
//        if (GroupSortType.RECOMMEND == sortType && userTags != null && !userTags.isEmpty()) {
//            NumberExpression<Long> matchCount = new CaseBuilder()
//                    .when(groups.groupRules.in(userTags)).then(1L)
//                    .otherwise(0L)
//                    .sum();
//            orders.add(new OrderSpecifier<>(Order.DESC, matchCount));
//        }

        // 2순위: 인기순(신청자 수)
        if (GroupSortType.POPULAR == sortType) {
            orders.add(new OrderSpecifier<>(Order.DESC, groups.applications.size()));
        }

        //최신순
        orders.add(new OrderSpecifier<>(Order.DESC, groups.createdAt));

        return orders.toArray(new OrderSpecifier[0]);
    }

   //지역 필터
   private BooleanExpression containsMeetPlaces(List<String> meetPlaces) {
       if (meetPlaces == null || meetPlaces.isEmpty()) return null;

       return meetPlaces.stream()
               .map(place -> {
                   if (place.contains("전체")) {
                       String clean = place.replace("전체", "").trim();
                       return clean.length() >= 2 ? clean.substring(0, 2) : clean;
                   }
                   return place;
               })
               .filter(keyword -> keyword != null && !keyword.isBlank())
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
        GroupSortType sortType = (sort != null) ? sort : GroupSortType.LATEST;

        return switch (sortType) {
            case POPULAR -> groups.applications.size().desc(); // 인기순

            // RECOMMEND가 들어와도 의도적으로 LATEST(최신순)를 반환(검색결과에는 추천순 필터 없음)
            case LATEST, RECOMMEND -> groups.createdAt.desc();

            default -> groups.createdAt.desc();
        };
    }
}