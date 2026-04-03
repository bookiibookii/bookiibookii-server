package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    void deleteAllByUser(User user);
    List<UserTag> findByUserId(Long userId);
    List<UserTag> findAllByUser(User user);
}
