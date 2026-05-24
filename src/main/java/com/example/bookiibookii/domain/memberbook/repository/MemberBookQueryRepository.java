package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberBookMypageDTO;
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

    /** GroupBookQueryRepository.findRecentBooksWithRating 대체 */
    public List<MemberBookMypageDTO> findRecentBooksWithRating(Long userId, Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(MemberBookMypageDTO.class,
                        memberBook.book.title,
                        bookReview.star
                ))
                .from(bookReview)
                .join(bookReview.memberBook, memberBook)
                .join(memberBook.group, groups)
                .join(bookReview.matchedMember)
                .where(
                        bookReview.matchedMember.user.id.eq(userId),
                        groups.groupStatus.eq(GroupStatus.COMPLETED)
                )
                .orderBy(bookReview.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
