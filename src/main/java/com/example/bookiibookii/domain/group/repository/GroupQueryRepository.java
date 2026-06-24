package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.book.entity.QBook;
import com.example.bookiibookii.domain.book.enums.CategoryGroup;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.QGroups;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.HomeCandidateSectionType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.example.bookiibookii.domain.book.entity.QBook.book;
import static com.example.bookiibookii.domain.aladin.entity.QBestsellerIsbn.bestsellerIsbn;
import static com.example.bookiibookii.domain.group.entity.QGroupPlace.groupPlace;
import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.group.entity.QHomeSectionBookCandidate.homeSectionBookCandidate;
import static com.example.bookiibookii.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class GroupQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record HomeBookProjection(
            String isbn13,
            String title,
            String author,
            String image
    ) {}

    public record HomeBestsellerBookProjection(
            String isbn13,
            String title,
            String author,
            String image,
            Integer ranking
    ) {}

    public Slice<Groups> findGroupsByFilters(GroupRequestDTO.FilterDTO filter, Pageable pageable) {

        List<Groups> content = queryFactory
                .selectFrom(groups)
                .join(groups.book, book).fetchJoin() // 도서 정보 페치 조인
                .join(groups.host, user).fetchJoin() // 호스트 정보 페치 조인
                .leftJoin(groups.groupPlace, groupPlace)
                .where(
                        inTradeTypes(filter.tradeTypes()),
                        containsRegions(filter.regions()),
                        inCategories(filter.categories()),
                        groups.groupStatus.eq(GroupStatus.RECRUITING)
                )
                .groupBy(groups.id)
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
        return new OrderSpecifier[]{
                new OrderSpecifier<>(Order.DESC, groups.createdAt),
                new OrderSpecifier<>(Order.DESC, groups.id)
        };
    }

    private BooleanExpression containsRegions(List<String> regions) {
        if (regions == null || regions.isEmpty()) return null;
        return regions.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> Arrays.stream(r.trim().split("\\s+"))
                        .map(groupPlace.address::contains)
                        .reduce(BooleanExpression::and)
                        .orElse(null))
                .filter(expr -> expr != null)
                .reduce(BooleanExpression::or)
                .orElse(null);
    }

    private BooleanExpression inTradeTypes(List<TradeType> types) {
        return (types == null || types.isEmpty()) ? null : groups.tradeType.in(types);
    }

    private BooleanExpression inCategories(List<CustomCategory> categories) {
        if (categories == null || categories.isEmpty()) return null;
        if (categories.contains(CustomCategory.ALL)) return null;

        List<CustomCategory> expanded = categories.stream()
                .flatMap(cat -> switch (cat) {
                    case LITERATURE_ALL -> Arrays.stream(CustomCategory.values())
                            .filter(c -> c.getGroup() == CategoryGroup.LITERATURE && c != CustomCategory.LITERATURE_ALL);
                    case NON_LITERATURE_ALL -> Arrays.stream(CustomCategory.values())
                            .filter(c -> c.getGroup() == CategoryGroup.NON_LITERATURE && c != CustomCategory.NON_LITERATURE_ALL);
                    default -> Stream.of(cat);
                })
                .distinct()
                .toList();

        return expanded.isEmpty() ? null : book.category.in(expanded);
    }

    public long countGroupsByFilters(GroupRequestDTO.FilterDTO filter) {
        Long count = queryFactory
                .select(groups.countDistinct())
                .from(groups)
                .join(groups.book, book)
                .leftJoin(groups.groupPlace, groupPlace)
                .where(
                        inTradeTypes(filter.tradeTypes()),
                        containsRegions(filter.regions()),
                        inCategories(filter.categories()),
                        groups.groupStatus.eq(GroupStatus.RECRUITING)
                )
                .fetchOne();
        return count != null ? count : 0L;
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
                .groupBy(groups.id) // 태그 조인으로 인한 중복 제거
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

    private OrderSpecifier<?>[] getSearchSortOrder(GroupSortType sort) {
        return new OrderSpecifier[]{
                new OrderSpecifier<>(Order.DESC, groups.createdAt),
                new OrderSpecifier<>(Order.DESC, groups.id)
        };
    }

    // ===== 그룹 홈 화면 (GET /api/groups/home) =====

    public List<Groups> findRecentGroups(
            Long userId,
            Instant createdAfter,
            int limit
    ) {
        return homeGroupQuery(userId)
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        groups.createdAt.goe(createdAfter)
                )
                .orderBy(groups.createdAt.desc(), groups.id.desc())
                .limit(limit)
                .fetch();
    }

    public List<HomeBookProjection> findPopularBooks(Long userId, int limit) {
        BooleanExpression visibleRecruitingCandidate =
                hasVisibleRecruitingGroups(userId)
                        ? isbn13WithVisibleRecruitingGroups(userId)
                        : null;
        NumberExpression<Long> groupCount = groups.id.count();
        DateTimeExpression<Instant> latestGroupCreatedAt = groups.createdAt.max();

        return queryFactory
                .select(Projections.constructor(
                        HomeBookProjection.class,
                        book.isbn13,
                        book.title,
                        book.author,
                        book.image
                ))
                .from(groups)
                .join(groups.book, book)
                .where(
                        groups.groupStatus.ne(GroupStatus.DELETED),
                        visibleRecruitingCandidate
                )
                .groupBy(book.id, book.isbn13, book.title, book.author, book.image)
                .orderBy(
                        groupCount.desc(),
                        latestGroupCreatedAt.desc(),
                        book.id.desc()
                )
                .limit(limit)
                .fetch();
    }

    private boolean hasVisibleRecruitingGroups(Long userId) {
        QGroups recruitingGroups = new QGroups("visibleRecruitingGroups");

        Integer found = queryFactory
                .selectOne()
                .from(recruitingGroups)
                .where(
                        recruitingGroups.groupStatus.eq(GroupStatus.RECRUITING),
                        recruitingGroups.host.id.ne(userId)
                )
                .fetchFirst();
        return found != null;
    }

    private BooleanExpression isbn13WithVisibleRecruitingGroups(Long userId) {
        QGroups candidateGroups = new QGroups("popularCandidateGroups");
        QBook candidateBook = new QBook("popularCandidateBook");

        return book.isbn13.in(
                JPAExpressions
                        .select(candidateBook.isbn13)
                        .distinct()
                        .from(candidateGroups)
                        .join(candidateGroups.book, candidateBook)
                        .where(
                                candidateGroups.groupStatus.eq(GroupStatus.RECRUITING),
                                candidateGroups.host.id.ne(userId)
                        )
        );
    }

    public List<HomeBestsellerBookProjection> findBestsellerBooks(int limit) {
        return queryFactory
                .select(Projections.constructor(
                        HomeBestsellerBookProjection.class,
                        bestsellerIsbn.isbn13,
                        bestsellerIsbn.title,
                        bestsellerIsbn.author,
                        bestsellerIsbn.bookImage,
                        bestsellerIsbn.rank
                ))
                .from(bestsellerIsbn)
                .orderBy(bestsellerIsbn.rank.asc(), bestsellerIsbn.id.asc())
                .limit(limit)
                .fetch();
    }

    public List<CustomCategory> findCategoriesWithRecruitingGroups(Long userId) {
        return queryFactory
                .select(book.category).distinct()
                .from(groups)
                .join(groups.book, book)
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        groups.host.id.ne(userId),
                        book.category.notIn(
                                CustomCategory.ALL,
                                CustomCategory.LITERATURE_ALL,
                                CustomCategory.NON_LITERATURE_ALL
                        )
                )
                .orderBy(book.category.asc())
                .fetch();
    }

    public List<Groups> findGroupsByCategories(
            Long userId,
            List<CustomCategory> categories,
            int limit
    ) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return homeGroupQuery(userId)
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        book.category.in(categories)
                )
                .orderBy(groups.createdAt.desc(), groups.id.desc())
                .limit(limit)
                .fetch();
    }

    public List<Groups> findClassicGroups(
            Long userId,
            HomeCandidateSectionType candidateSectionType,
            int limit
    ) {
        return homeGroupQuery(userId)
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        book.isbn13.in(
                                JPAExpressions
                                        .select(homeSectionBookCandidate.isbn13)
                                        .from(homeSectionBookCandidate)
                                        .where(
                                                homeSectionBookCandidate.sectionType
                                                        .eq(candidateSectionType),
                                                homeSectionBookCandidate.active.isTrue()
                                        )
                        )
                )
                .orderBy(groups.createdAt.desc(), groups.id.desc())
                .limit(limit)
                .fetch();
    }

    public List<Groups> findGroupsByTradeType(
            Long userId,
            TradeType tradeType,
            int limit
    ) {
        return homeGroupQuery(userId)
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        groups.tradeType.eq(tradeType)
                )
                .orderBy(groups.createdAt.desc(), groups.id.desc())
                .limit(limit)
                .fetch();
    }

    public boolean existsDirectGroupsAtCoordinate(
            Long userId,
            BigDecimal x,
            BigDecimal y
    ) {
        if (x == null || y == null) {
            return false;
        }
        Integer found = queryFactory
                .selectOne()
                .from(groups)
                .join(groups.groupPlace, groupPlace)
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        groups.host.id.ne(userId),
                        groups.tradeType.eq(TradeType.DIRECT),
                        groupPlace.x.eq(x),
                        groupPlace.y.eq(y)
                )
                .fetchFirst();
        return found != null;
    }

    public List<Groups> findDirectGroupsAtCoordinate(
            Long userId,
            BigDecimal x,
            BigDecimal y,
            int limit
    ) {
        if (x == null || y == null) {
            return List.of();
        }
        return homeGroupQuery(userId)
                .join(groups.groupPlace, groupPlace).fetchJoin()
                .where(
                        groups.groupStatus.eq(GroupStatus.RECRUITING),
                        groups.tradeType.eq(TradeType.DIRECT),
                        groupPlace.x.eq(x),
                        groupPlace.y.eq(y)
                )
                .orderBy(groups.createdAt.desc(), groups.id.desc())
                .limit(limit)
                .fetch();
    }

    private com.querydsl.jpa.impl.JPAQuery<Groups> homeGroupQuery(Long userId) {
        return queryFactory
                .selectFrom(groups)
                .join(groups.book, book).fetchJoin()
                .join(groups.host, user).fetchJoin()
                .leftJoin(user.userImage).fetchJoin()
                .where(
                        groups.host.id.ne(userId)
                )
                .distinct();
    }
}
