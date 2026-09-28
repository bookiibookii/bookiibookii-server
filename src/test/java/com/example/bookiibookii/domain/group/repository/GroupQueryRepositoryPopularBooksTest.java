package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({GroupQueryRepository.class, QueryDslConfig.class})
@ActiveProfiles("test")
class GroupQueryRepositoryPopularBooksTest {

    @Autowired
    private GroupQueryRepository groupQueryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void usesOnlyVisibleRecruitingIsbnAsCandidatesButSortsByTotalGroupCount() {
        User viewer = user("viewer");
        User other = user("other");
        entityManager.persist(viewer);
        entityManager.persist(other);

        Book highTotal = book("9780000000001", "모집 1개 전체 11개");
        Book lowTotal = book("9780000000002", "모집 2개 전체 2개");
        Book completedOnly = book("9780000000003", "완료만 20개");
        Book myRecruitingOnly = book("9780000000004", "내 모집만 1개");
        entityManager.persist(highTotal);
        entityManager.persist(lowTotal);
        entityManager.persist(completedOnly);
        entityManager.persist(myRecruitingOnly);

        group(highTotal, other, GroupStatus.RECRUITING, at(10));
        for (int i = 0; i < 10; i++) {
            group(highTotal, other, GroupStatus.COMPLETED, at(i));
        }
        group(lowTotal, other, GroupStatus.RECRUITING, at(9));
        group(lowTotal, other, GroupStatus.RECRUITING, at(8));
        for (int i = 0; i < 20; i++) {
            group(completedOnly, other, GroupStatus.COMPLETED, at(20 + i));
        }
        group(myRecruitingOnly, viewer, GroupStatus.RECRUITING, at(99));

        flushAndClear();

        List<GroupQueryRepository.HomeBookProjection> result =
                groupQueryRepository.findPopularBooks(viewer.getId(), 5);

        assertThat(result).extracting(GroupQueryRepository.HomeBookProjection::isbn13)
                .containsExactly("9780000000001", "9780000000002");
    }

    @Test
    void fallsBackToAllRegisteredBooksWhenNoVisibleRecruitingGroupExists() {
        User viewer = user("viewer");
        User other = user("other");
        entityManager.persist(viewer);
        entityManager.persist(other);

        Book first = book("9780000000011", "전체 3개");
        Book second = book("9780000000012", "전체 2개");
        entityManager.persist(first);
        entityManager.persist(second);

        group(first, other, GroupStatus.COMPLETED, at(1));
        group(first, other, GroupStatus.COMPLETED, at(2));
        group(first, other, GroupStatus.COMPLETED, at(3));
        group(second, other, GroupStatus.COMPLETED, at(99));
        group(second, other, GroupStatus.COMPLETED, at(100));

        flushAndClear();

        List<GroupQueryRepository.HomeBookProjection> result =
                groupQueryRepository.findPopularBooks(viewer.getId(), 5);

        assertThat(result).extracting(GroupQueryRepository.HomeBookProjection::isbn13)
                .containsExactly("9780000000011", "9780000000012");
    }

    @Test
    void sortsByTotalGroupCountThenLatestGroupCreatedAt() {
        User viewer = user("viewer");
        User other = user("other");
        entityManager.persist(viewer);
        entityManager.persist(other);

        Book frequent = book("9780000000021", "빈도 높음");
        Book olderTie = book("9780000000022", "동률 오래됨");
        Book newerTie = book("9780000000023", "동률 최신");
        entityManager.persist(frequent);
        entityManager.persist(olderTie);
        entityManager.persist(newerTie);

        group(frequent, other, GroupStatus.RECRUITING, at(1));
        group(frequent, other, GroupStatus.COMPLETED, at(2));
        group(frequent, other, GroupStatus.COMPLETED, at(3));
        group(olderTie, other, GroupStatus.RECRUITING, at(10));
        group(newerTie, other, GroupStatus.RECRUITING, at(20));

        flushAndClear();

        List<GroupQueryRepository.HomeBookProjection> result =
                groupQueryRepository.findPopularBooks(viewer.getId(), 5);

        assertThat(result).extracting(GroupQueryRepository.HomeBookProjection::isbn13)
                .containsExactly(
                        "9780000000021",
                        "9780000000023",
                        "9780000000022"
                );
    }

    private User user(String suffix) {
        User user = User.builder()
                .nickName(suffix)
                .socialType(SocialType.KAKAO)
                .socialId("social-" + suffix)
                .build();
        setAudit(user, at(0));
        return user;
    }

    private Book book(String isbn13, String title) {
        Book book = Book.builder()
                .isbn13(isbn13)
                .title(title)
                .author("저자")
                .publisher("출판사")
                .image("image")
                .totalPages(100)
                .link("link")
                .category(CustomCategory.KOREAN_NOVEL)
                .build();
        setAudit(book, at(0));
        return book;
    }

    private void group(
            Book book,
            User host,
            GroupStatus status,
            LocalDateTime createdAt
    ) {
        Groups group = Groups.builder()
                .book(book)
                .host(host)
                .maxCapacity(2)
                .startDate(LocalDate.now())
                .readingPeriod(14)
                .groupStatus(status)
                .tradeType(TradeType.DIRECT)
                .groupName(book.getTitle() + " 그룹")
                .build();
        setAudit(group, createdAt);
        entityManager.persist(group);
    }

    private void setAudit(Object entity, LocalDateTime createdAt) {
        Instant instant = createdAt.atZone(ZoneId.of("Asia/Seoul")).toInstant();
        ReflectionTestUtils.setField(entity, "createdAt", instant);
        ReflectionTestUtils.setField(entity, "updatedAt", instant);
    }

    private LocalDateTime at(int minute) {
        return LocalDateTime.of(2026, 6, 8, 0, 0).plusMinutes(minute);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
