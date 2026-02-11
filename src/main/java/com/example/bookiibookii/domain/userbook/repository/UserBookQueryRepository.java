package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.userbook.entity.QUserBook.userBook;

@Repository
@RequiredArgsConstructor
public class UserBookQueryRepository {

    private final JPAQueryFactory queryFactory;

    //최근 읽은 책과 평점 조회
    public List<UserBookResponseDTO.MypageBookDto> findRecentBooksWithRating(Long userId, Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(UserBookResponseDTO.MypageBookDto.class,
                        groups.book.title,
                        userBook.rating
                ))
                .from(userBook)
                .join(userBook.group, groups)
                .where(userBook.user.id.eq(userId), (userBook.group.groupStatus.eq(GroupStatus.COMPLETED)))
                .orderBy(userBook.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}