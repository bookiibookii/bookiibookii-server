package com.example.bookiibookii.domain.notification.repository;

import com.example.bookiibookii.domain.notification.entity.UserKeyword;
import com.example.bookiibookii.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {

    // 키워드 리스트 조회
    long countByUser(User user);

    @Query("""
        select uk
        from UserKeyword uk
        join fetch uk.keyword k
        where uk.user = :user
        order by uk.id desc
    """)
    List<UserKeyword> findAllByUserOrderByLatest(@Param("user") User user);

    @Query("""
        select uk
        from UserKeyword uk
        join fetch uk.keyword k
        where uk.user = :user
        order by k.content asc
    """)
    List<UserKeyword> findAllByUserOrderByAlphabetical(@Param("user") User user);

    // 키워드 등록
    Optional<UserKeyword> findByUserAndKeyword_Id(User user, Long keywordId);

    boolean existsByUserAndKeyword_Content(User user, String content);

    @Query("""
        select uk
        from UserKeyword uk
        join fetch uk.user u
        join fetch uk.keyword k
        where k.id in :keywordIds
    """)
    List<UserKeyword> findAllWithUserAndKeywordByKeywordIds(@Param("keywordIds") List<Long> keywordIds);

    boolean existsByUserAndKeyword_NormalizedContent(User user, String normalizedContent);
}
