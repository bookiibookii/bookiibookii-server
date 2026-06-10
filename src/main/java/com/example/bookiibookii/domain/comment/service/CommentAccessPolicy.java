package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.enums.CommentContext;
import com.example.bookiibookii.domain.comment.exception.CommentException;
import com.example.bookiibookii.domain.comment.exception.code.CommentErrorCode;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentAccessPolicy {

    private final MatchedMemberRepository matchedMemberRepository;

    public CommentContext resolveContext(Groups group) {
        return switch (group.getGroupStatus()) {
            case RECRUITING -> CommentContext.GROUP_DETAIL;
            case MATCHED, COMPLETED -> CommentContext.TRACKER;
            case DELETED -> throw new GroupException(GroupErrorCode.GROUP_NOT_FOUND);
        };
    }

    public void validateGroupDetailAccess(User user) {
        if (user == null) {
            throw new CommentException(CommentErrorCode.NO_PERMISSION);
        }
    }

    public void validateTrackerAccess(Long groupId, User user) {
        if (user == null || !matchedMemberRepository.existsByGroup_IdAndUser_IdAndStatus(
                groupId,
                user.getId(),
                MemberStatus.JOINED
        )) {
            throw new CommentException(CommentErrorCode.NO_PERMISSION);
        }
    }

    public void validateAccess(CommentContext context, Long groupId, User user) {
        switch (context) {
            case GROUP_DETAIL -> validateGroupDetailAccess(user);
            case TRACKER -> validateTrackerAccess(groupId, user);
        }
    }
}
