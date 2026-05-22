package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberBookResponseDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.memberbook.entity.QMemberBook.memberBook;
import static com.example.bookiibookii.domain.review.entity.QBookReview.bookReview;

@Repository
@RequiredArgsConstructor
public class MemberBookQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MemberBookResponseDTO.MypageBookDto> findRecentBooksWithRating(Long userId, Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(
                        MemberBookResponseDTO.MypageBookDto.class,
                        memberBook.book.title,
                        bookReview.star
                ))
                .from(memberBook)
                .join(memberBook.group, groups)
                .leftJoin(bookReview).on(bookReview.memberBook.id.eq(memberBook.id))
                .where(
                        memberBook.matchedMember.user.id.eq(userId),
                        memberBook.removedAt.isNull(),
                        groups.groupStatus.eq(GroupStatus.COMPLETED)
                )
                .orderBy(memberBook.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
