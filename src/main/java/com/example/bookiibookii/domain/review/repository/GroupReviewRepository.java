package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.review.entity.GroupReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupReviewRepository extends JpaRepository<GroupReview, Long> {

    /**
     * 특정 Group에서 특정 유저가 reviewer인 리뷰가 이미 존재하는지 확인
     * 리뷰 생성 시 중복 방지용
     * 
     * @param groupId Group ID
     * @param reviewerUserId 리뷰를 작성하는 유저 ID (현재 로그인한 유저)
     * @return 리뷰 존재 여부
     */
    @Query("""
        SELECT CASE WHEN COUNT(gr) > 0 THEN true ELSE false END
        FROM GroupReview gr
        JOIN gr.reviewer mm
        WHERE mm.group.groupId = :groupId
          AND mm.user.id = :reviewerUserId
    """)
    boolean existsByGroupIdAndReviewerUserId(@Param("groupId") Long groupId,
                                              @Param("reviewerUserId") Long reviewerUserId);

    /**
     * 특정 유저가 reviewed인 리뷰들을 조회 (마이페이지용)
     *
     * @param reviewedUserId 리뷰를 받은 유저 ID (현재 로그인한 유저)
     * @return 리뷰 목록
     */
    @Query("""
        SELECT gr
        FROM GroupReview gr
        JOIN gr.reviewed mm
        WHERE mm.user.id = :reviewedUserId
    """)
    List<GroupReview> findByReviewedUserId(@Param("reviewedUserId") Long reviewedUserId);

    /**
     * 특정 유저가 reviewed인 리뷰 중, 지정한 그룹 타입만 조회 (이어읽기 리뷰 히스토리용)
     *
     * @param reviewedUserId 리뷰를 받은 유저 ID
     * @param groupType     그룹 타입 (RELAY 등)
     * @return 리뷰 목록 (최신순)
     */
    @Query("""
        SELECT gr
        FROM GroupReview gr
        JOIN gr.reviewed mm
        JOIN mm.group g
        WHERE mm.user.id = :reviewedUserId
          AND g.groupType = :groupType
        ORDER BY gr.id DESC
    """)
    List<GroupReview> findByReviewedUserIdAndGroupType(@Param("reviewedUserId") Long reviewedUserId,
                                                       @Param("groupType") GroupType groupType);

    /**
     * 특정 reviewer MatchedMember에 대한 리뷰 조회
     * 
     * @param reviewerMatchedMember reviewer의 MatchedMember 엔티티
     * @return 리뷰 (존재하지 않으면 Optional.empty())
     */
    Optional<GroupReview> findByReviewer(MatchedMember reviewerMatchedMember);
}
