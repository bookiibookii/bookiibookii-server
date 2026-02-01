package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    void deleteAllByUser(User user);
    List<UserTag> findByUserIdAndTagTypeIn(Long userId, List<TagType> targetTypes);

    List<UserTag> findAllByUser(User user);
}
