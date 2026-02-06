package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.DeletedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeletedCardRepository extends JpaRepository<DeletedCard, Long> {

    boolean existsByUser_IdAndCard_Id(Long userId, Long cardId);

    @Query("SELECT dc.card.id FROM DeletedCard dc WHERE dc.user.id = :userId")
    List<Long> findCardIdsByUser_Id(@Param("userId") Long userId);
}
