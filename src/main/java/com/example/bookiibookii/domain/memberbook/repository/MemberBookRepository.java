package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import org.springframework.data.domain.Pageable;
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

    @Query("""
        SELECT DISTINCT mb FROM MemberBook mb
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mb.group g
        JOIN FETCH mb.book b
        JOIN FETCH g.host h
        LEFT JOIN FETCH h.userImage
        WHERE mm.user.id = :userId
        AND mb.removedAt IS NULL
        AND (g.groupName LIKE %:keyword%
             OR b.title LIKE %:keyword%
             OR b.author LIKE %:keyword%)
        ORDER BY mb.updatedAt DESC
        """)
    List<MemberBook> searchMyLibrary(@Param("userId") Long userId, @Param("keyword") String keyword);

    Optional<MemberBook> findByMatchedMember_IdAndBook_IdAndIsMine(
            Long matchedMemberId,
            Long bookId,
            boolean isMine
    );

  /** GroupBook.findByUser_IdAndGroup_Id 대체 — 그룹 대표 책(group.book)에 해당하는 MemberBook */
  @Query("""
      SELECT mb FROM MemberBook mb
      JOIN FETCH mb.matchedMember mm
      JOIN FETCH mb.group g
      JOIN FETCH mb.book
      WHERE mm.user.id = :userId
      AND g.id = :groupId
      AND mb.book.id = g.book.id
      """)
  Optional<MemberBook> findByUser_IdAndGroup_Id(
      @Param("userId") Long userId,
      @Param("groupId") Long groupId
  );

  /** GroupBook.findCompletedBooksByUserId 대체 — 그룹 대표 책(group.book) 1건/그룹 */
  @Query("""
      SELECT mb FROM MemberBook mb
      JOIN FETCH mb.group g
      JOIN FETCH g.book
      JOIN FETCH mb.matchedMember mm
      WHERE mm.user.id = :userId
      AND mb.removedAt IS NULL
      AND mb.book.id = g.book.id
      AND g.groupStatus = com.example.bookiibookii.domain.group.enums.GroupStatus.COMPLETED
      ORDER BY mb.updatedAt DESC
      """)
  List<MemberBook> findCompletedBooksByUserId(@Param("userId") Long userId);

  /** GroupBook.findAllByGroup_Id 대체 (트래커 할당 등) */
  List<MemberBook> findAllByGroup_Id(Long groupId);

  /** GroupBook.findRecentBookTitle 대체 */
  @Query("""
      SELECT mb.book.title
      FROM MemberBook mb
      JOIN mb.matchedMember mm
      WHERE mm.user.id = :userId
      AND mb.removedAt IS NULL
      ORDER BY mb.updatedAt DESC
      """)
  List<String> findRecentBookTitle(@Param("userId") Long userId, Pageable pageable);

  /** GroupBook.countByUser_IdAndRemovedAtIsNull 대체 */
  @Query("""
      SELECT COUNT(mb) FROM MemberBook mb
      JOIN mb.matchedMember mm
      WHERE mm.user.id = :userId
      AND mb.removedAt IS NULL
      """)
  long countByUser_IdAndRemovedAtIsNull(@Param("userId") Long userId);

  /** GroupBook.findByGroup_IdInWithUserAndGroup 대체 — 그룹별 MemberBook + MatchedMember + User */
  @Query("""
      SELECT mb FROM MemberBook mb
      JOIN FETCH mb.matchedMember mm
      JOIN FETCH mm.user
      JOIN FETCH mb.group
      WHERE mb.group.id IN :groupIds
      """)
  List<MemberBook> findByGroup_IdInWithMatchedMemberAndUser(@Param("groupIds") List<Long> groupIds);
}

