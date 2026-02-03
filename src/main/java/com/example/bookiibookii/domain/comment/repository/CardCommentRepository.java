package com.example.bookiibookii.domain.comment.repository;

import com.example.bookiibookii.domain.comment.entity.CardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

    @Query("""
        select cc
        from CardComment cc
        join fetch cc.user u
        left join fetch u.userImage
        where cc.card.id = :cardId
        order by cc.createdAt asc
    """)
    List<CardComment> findAllByCardIdWithUserOrderByCreatedAtAsc(@Param("cardId") Long cardId);

    Optional<CardComment> findByIdAndCardId(Long commentId, Long cardId);

    long countByCardId(Long cardId);

}
