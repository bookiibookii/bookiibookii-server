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
    Optional<GroupBook> findByUser_IdAndGroup_GroupId(Long userId, Long groupId);

    @Query("""
        SELECT DISTINCT ub FROM GroupBook ub
        JOIN FETCH ub.group g
        JOIN FETCH g.book
        JOIN FETCH g.host h
        LEFT JOIN FETCH h.userImage
        WHERE ub.user.id = :userId
        AND ub.removedAt IS NULL
        ORDER BY ub.updatedAt DESC
        """)
    List<GroupBook> findAllByUser_IdWithGroupAndBookAndHost(@Param("userId") Long userId);

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



    //검색로직
    @Query("""
    SELECT DISTINCT ub FROM GroupBook ub
    JOIN FETCH ub.group g
    JOIN FETCH g.book b
    JOIN FETCH g.host h
    LEFT JOIN FETCH h.userImage
    WHERE ub.user.id = :userId
    AND ub.removedAt IS NULL
    AND (b.title LIKE %:keyword% 
         OR b.author LIKE %:keyword% 
         OR ub.comment LIKE %:keyword%)
    ORDER BY ub.updatedAt DESC
    """)
    List<GroupBook> searchMyLibrary(@Param("userId") Long userId, @Param("keyword") String keyword);


    @Query("SELECT ub FROM GroupBook ub JOIN FETCH ub.user JOIN FETCH ub.group WHERE ub.group.groupId IN :groupIds")
    List<GroupBook> findByGroup_GroupIdInWithUserAndGroup(@Param("groupIds") List<Long> groupIds);


    // 특정 그룹의 모든 멤버가 가진 GroupBook 목록 조회 (트래커 할당용)
    List<GroupBook> findAllByGroup_GroupId(Long groupId);

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
}
