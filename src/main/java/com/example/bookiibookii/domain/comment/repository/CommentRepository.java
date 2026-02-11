package com.example.bookiibookii.domain.comment.repository;

import com.example.bookiibookii.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
    select c
    from Comment c
    join fetch c.user u
    where c.id = :commentId and c.group.groupId = :groupId
    """)
    Optional<Comment> findByIdAndGroupIdWithUser(@Param("commentId") Long commentId,
                                                 @Param("groupId") Long groupId);

    @Query("""
    select c from Comment c
    join fetch c.user u
    where c.group.groupId = :groupId
    and (
        c.secret = false
        or (c.secret = true and (c.user.id = :viewerId or c.secretTargetUserId = :viewerId))
    )
    order by c.createdAt asc
    """)
    List<Comment> findVisibleTree(@Param("groupId") Long groupId,
                                  @Param("viewerId") Long viewerId);
}
