package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GroupBookRepository extends JpaRepository<GroupBook, Long> {

    Optional<GroupBook> findByIdAndUser_Id(Long id, Long userId);

    // 완독한 책 개수 (서재에서 제거한 항목 제외)
    Long countByUser_IdAndRemovedAtIsNull(Long userId);

    @Query("SELECT ub FROM GroupBook ub JOIN FETCH ub.user JOIN FETCH ub.group WHERE ub.group.groupId IN :groupIds")
    List<GroupBook> findByGroup_GroupIdInWithUserAndGroup(@Param("groupIds") List<Long> groupIds);

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
}
