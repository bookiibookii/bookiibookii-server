package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.repository.BookRepository;
import com.example.bookiibookii.domain.group.entity.PopularBook;
import com.example.bookiibookii.domain.group.repository.GroupQueryRepository;
import com.example.bookiibookii.domain.group.repository.PopularBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularBookService {

    // 홈 노출은 5권이지만, 조회 시 '본인 외 모집중' 필터로 빠지는 책을 고려해 넉넉히 저장
    private static final int AGGREGATE_LIMIT = 50;

    private final GroupQueryRepository groupQueryRepository;
    private final PopularBookRepository popularBookRepository;
    private final BookRepository bookRepository;
    private final Clock clock;

    // 한 트랜잭션에서 전체 교체 → 커밋 전까지 조회는 이전 집계 결과를 본다
    @Transactional
    public int refresh() {
        List<GroupQueryRepository.PopularBookAggregate> aggregates =
                groupQueryRepository.aggregatePopularBooks(AGGREGATE_LIMIT);
        Instant aggregatedAt = clock.instant();

        List<PopularBook> popularBooks = new ArrayList<>();
        for (int i = 0; i < aggregates.size(); i++) {
            GroupQueryRepository.PopularBookAggregate aggregate = aggregates.get(i);
            popularBooks.add(PopularBook.builder()
                    .book(bookRepository.getReferenceById(aggregate.bookId()))
                    .groupCount(aggregate.groupCount())
                    .lastGroupCreatedAt(aggregate.lastGroupCreatedAt())
                    .ranking(i + 1)
                    .aggregatedAt(aggregatedAt)
                    .build());
        }

        popularBookRepository.deleteAllInBatch();
        popularBookRepository.saveAll(popularBooks);
        return popularBooks.size();
    }

    @Transactional(readOnly = true)
    public boolean isEmpty() {
        return popularBookRepository.count() == 0;
    }
}
