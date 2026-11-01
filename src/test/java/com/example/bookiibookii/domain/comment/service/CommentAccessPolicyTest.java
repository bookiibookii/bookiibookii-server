package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.enums.CommentContext;
import com.example.bookiibookii.domain.comment.exception.CommentException;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentAccessPolicyTest {

    @Mock
    private MatchedMemberRepository matchedMemberRepository;

    @Test
    void recruitingGroupUsesGroupDetailContextAndAllowsAuthenticatedUser() {
        CommentAccessPolicy policy = new CommentAccessPolicy(matchedMemberRepository);
        Groups group = Groups.builder().id(1L).groupStatus(GroupStatus.RECRUITING).build();
        User user = User.builder().id(10L).build();

        assertThat(policy.resolveContext(group)).isEqualTo(CommentContext.GROUP_DETAIL);
        assertThatCode(() -> policy.validateAccess(CommentContext.GROUP_DETAIL, group.getId(), user))
                .doesNotThrowAnyException();
    }

    @Test
    void matchedAndCompletedGroupsUseTrackerContext() {
        CommentAccessPolicy policy = new CommentAccessPolicy(matchedMemberRepository);

        assertThat(policy.resolveContext(
                Groups.builder().groupStatus(GroupStatus.MATCHED).build()
        )).isEqualTo(CommentContext.TRACKER);
        assertThat(policy.resolveContext(
                Groups.builder().groupStatus(GroupStatus.COMPLETED).build()
        )).isEqualTo(CommentContext.TRACKER);
    }

    @Test
    void deletedGroupCannotResolveCommentContext() {
        CommentAccessPolicy policy = new CommentAccessPolicy(matchedMemberRepository);

        assertThatThrownBy(() -> policy.resolveContext(
                Groups.builder().groupStatus(GroupStatus.DELETED).build()
        )).isInstanceOf(GroupException.class);
    }

    @Test
    void trackerContextAllowsOnlyJoinedMatchedMember() {
        CommentAccessPolicy policy = new CommentAccessPolicy(matchedMemberRepository);
        User member = User.builder().id(10L).build();
        User outsider = User.builder().id(20L).build();
        when(matchedMemberRepository.existsByGroup_IdAndUser_IdAndStatus(
                1L, member.getId(), MemberStatus.JOINED
        )).thenReturn(true);

        assertThatCode(() -> policy.validateTrackerAccess(1L, member))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateTrackerAccess(1L, outsider))
                .isInstanceOf(CommentException.class);
    }
}
