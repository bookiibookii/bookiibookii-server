package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.aladin.entity.BestsellerIsbn;
import com.example.bookiibookii.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({GroupQueryRepository.class, QueryDslConfig.class})
@ActiveProfiles("test")
class GroupQueryRepositoryBestsellerBooksTest {

    @Autowired
    private GroupQueryRepository groupQueryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void exposesBestsellerWithoutBookRowAndKeepsActualRanking() {
        bestseller("9780000000001", 1, "1위 책");
        bestseller("9780000000002", 2, "2위 책");
        bestseller("9780000000003", 3, "3위 책");
        flushAndClear();

        List<GroupQueryRepository.HomeBestsellerBookProjection> result =
                groupQueryRepository.findBestsellerBooks(3);

        assertThat(result).extracting(GroupQueryRepository.HomeBestsellerBookProjection::isbn13)
                .containsExactly("9780000000001", "9780000000002", "9780000000003");
        assertThat(result).extracting(GroupQueryRepository.HomeBestsellerBookProjection::ranking)
                .containsExactly(1, 2, 3);
    }

    @Test
    void sortsByRankingThenId() {
        bestseller("9780000000012", 2, "2위 책");
        bestseller("9780000000011", 1, "1위 책");
        bestseller("9780000000013", 2, "같은 2위 늦은 책");
        flushAndClear();

        List<GroupQueryRepository.HomeBestsellerBookProjection> result =
                groupQueryRepository.findBestsellerBooks(3);

        assertThat(result).extracting(GroupQueryRepository.HomeBestsellerBookProjection::isbn13)
                .containsExactly("9780000000011", "9780000000012", "9780000000013");
    }

    @Test
    void uniqueConstraintPreventsDuplicatedIsbnRows() {
        bestseller("9780000000021", 1, "1위 책");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> bestseller("9780000000021", 2, "중복 책")
                )
                .isInstanceOf(RuntimeException.class);
    }

    private void bestseller(String isbn13, int ranking, String title) {
        entityManager.persist(BestsellerIsbn.builder()
                .isbn13(isbn13)
                .rank(ranking)
                .title(title)
                .author("저자")
                .bookImage("image")
                .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
