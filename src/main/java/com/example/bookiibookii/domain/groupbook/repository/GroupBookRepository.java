package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GroupBookRepository extends JpaRepository<GroupBook, Long> {
    Optional<GroupBook> findById(Long id);

    @Query("SELECT ub FROM GroupBook ub JOIN FETCH ub.group g JOIN FETCH g.book JOIN FETCH ub.user WHERE ub.id = :id")
    Optional<GroupBook> findByIdWithGroupAndUser(@Param("id") Long id);

    Optional<GroupBook> findByIdAndUser_Id(Long id, Long userId);

    /** 그룹 내 해당 사용자의 GroupBook (한줄평 조회용) */
    Optional<GroupBook> findByUser_IdAndGroup_Id(Long userId, Long groupId);

    @Query("""
        SELECT g.book.title
        FROM GroupBook ub
        JOIN ub.group g
        WHERE ub.user.id = :userId
        AND ub.removedAt IS NULL
        ORDER BY ub.updatedAt DESC
    """)
    List<String> findRecentBookTitle(@Param("userId") Long userId, Pageable pageable);

    // 완독한 책 개수 (서재에서 제거한 항목 제외)
    Long countByUser_IdAndRemovedAtIsNull(Long userId);

    @Query("SELECT ub FROM GroupBook ub JOIN FETCH ub.user JOIN FETCH ub.group WHERE ub.group.id IN :groupIds")
    List<GroupBook> findByGroup_IdInWithUserAndGroup(@Param("groupIds") List<Long> groupIds);


    // 특정 그룹의 모든 멤버가 가진 GroupBook 목록 조회 (트래커 할당용)
    List<GroupBook> findAllByGroup_Id(Long groupId);

    @Query("""
        SELECT gb FROM GroupBook gb
        JOIN FETCH gb.group g
        JOIN FETCH g.book
        WHERE gb.user.id = :userId
        AND gb.removedAt IS NULL
        AND g.groupStatus = com.example.bookiibookii.domain.group.enums.GroupStatus.COMPLETED
        ORDER BY gb.updatedAt DESC
    """)
    List<GroupBook> findCompletedBooksByUserId(@Param("userId") Long userId);

    boolean existsByUser_IdAndBook_IdAndRatingIsNotNull(Long userId, Long bookId);

    @Query("""
        SELECT gb FROM GroupBook gb
        JOIN FETCH gb.book
        JOIN FETCH gb.group
        WHERE gb.user.id = :userId
          AND gb.rating IS NOT NULL
          AND gb.removedAt IS NULL
        ORDER BY gb.updatedAt DESC
    """)
    List<GroupBook> findReviewedBooksByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(gb) FROM GroupBook gb
        WHERE gb.user.id = :userId
          AND gb.rating IS NOT NULL
          AND gb.removedAt IS NULL
    """)
    long countReviewedBooksByUserId(@Param("userId") Long userId);
}
