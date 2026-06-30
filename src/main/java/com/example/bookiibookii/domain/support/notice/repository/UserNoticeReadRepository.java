package com.example.bookiibookii.domain.support.notice.repository;

import com.example.bookiibookii.domain.support.notice.entity.UserNoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserNoticeReadRepository extends JpaRepository<UserNoticeRead, Long> {

    @Query("SELECT ur.noticeId FROM UserNoticeRead ur WHERE ur.userId = :userId AND ur.noticeId IN :noticeIds")
    Set<Long> findReadNoticeIds(@Param("userId") Long userId, @Param("noticeIds") List<Long> noticeIds);
}
