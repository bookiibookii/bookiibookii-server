package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.UserBook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    Optional<UserBook> findById(Long id);

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.group g JOIN FETCH g.book JOIN FETCH ub.user WHERE ub.id = :id")
    Optional<UserBook> findByIdWithGroupAndUser(@Param("id") Long id);

    Optional<UserBook> findByIdAndUser_Id(Long id, Long userId);

    /** 그룹 내 해당 사용자의 UserBook (한줄평 조회용) */
    Optional<UserBook> findByUser_IdAndGroup_GroupId(Long userId, Long groupId);

    @Query("""
        SELECT DISTINCT ub FROM UserBook ub
        JOIN FETCH ub.group g
        JOIN FETCH g.book
        JOIN FETCH g.host h
        LEFT JOIN FETCH h.userImage
        WHERE ub.user.id = :userId
        AND ub.removedAt IS NULL
        ORDER BY ub.updatedAt DESC
        """)
    List<UserBook> findAllByUser_IdWithGroupAndBookAndHost(@Param("userId") Long userId);

    @Query("""
        SELECT g.book.title
        FROM UserBook ub
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
    SELECT DISTINCT ub FROM UserBook ub
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
    List<UserBook> searchMyLibrary(@Param("userId") Long userId, @Param("keyword") String keyword);


    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.user JOIN FETCH ub.group WHERE ub.group.groupId IN :groupIds")
    List<UserBook> findByGroup_GroupIdInWithUserAndGroup(@Param("groupIds") List<Long> groupIds);


    // 특정 그룹의 모든 멤버가 가진 UserBook 목록 조회 (트래커 할당용)
    List<UserBook> findAllByGroup_GroupId(Long groupId);
}
