package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.groupbook.dto.res.GroupBookResponseDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.groupbook.entity.QGroupBook.groupBook;

@Repository
@RequiredArgsConstructor
public class GroupBookQueryRepository {

    private final JPAQueryFactory queryFactory;

    //최근 읽은 책과 평점 조회
    public List<GroupBookResponseDTO.MypageBookDto> findRecentBooksWithRating(Long userId, Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(GroupBookResponseDTO.MypageBookDto.class,
                        groups.book.title,
                        groupBook.rating
                ))
                .from(groupBook)
                .join(groupBook.group, groups)
                .where(groupBook.user.id.eq(userId), (groupBook.group.groupStatus.eq(GroupStatus.COMPLETED)))
                .orderBy(groupBook.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}