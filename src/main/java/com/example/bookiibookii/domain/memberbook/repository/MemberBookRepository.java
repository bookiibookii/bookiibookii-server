package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberBookRepository extends JpaRepository<MemberBook, Long> {

    Optional<MemberBook> findByIdAndMatchedMember_User_Id(Long id, Long userId);

    @Query("""
        SELECT mb FROM MemberBook mb
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mb.book
        WHERE mb.id = :id AND mm.user.id = :userId
        """)
    Optional<MemberBook> findByIdAndMatchedMember_User_IdWithBook(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT DISTINCT mb FROM MemberBook mb
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mb.group g
        JOIN FETCH mb.book b
        JOIN FETCH g.host h
        LEFT JOIN FETCH h.userImage
        WHERE mm.user.id = :userId
        AND mb.removedAt IS NULL
        ORDER BY mb.updatedAt DESC
        """)
    List<MemberBook> findAllByMatchedMember_User_IdWithGroupAndBookAndHost(@Param("userId") Long userId);

    Optional<MemberBook> findByMatchedMember_IdAndBook_IdAndIsMine(
            Long matchedMemberId,
            Long bookId,
            boolean isMine
    );
}
