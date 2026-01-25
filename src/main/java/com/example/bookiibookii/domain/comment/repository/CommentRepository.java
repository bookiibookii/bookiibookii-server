package com.example.bookiibookii.domain.comment.repository;

import com.example.bookiibookii.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
        select c
        from Comment c
        join fetch c.user u
        where c.group.groupId = :groupId
        order by c.createdAt asc
    """)
    List<Comment> findAllByGroupIdWithUserOrderByCreatedAtAsc(@Param("groupId") Long groupId);
}
