package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.comment.dto.req.CommentCreateReqDTO;
import com.example.bookiibookii.domain.comment.entity.Comment;
import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.comment.exception.CommentException;
import com.example.bookiibookii.domain.comment.repository.CommentRepository;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private GroupsRepository groupsRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private UserImageS3Service userImageS3Service;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository,
                groupsRepository,
                matchedMemberRepository,
                new CommentAccessPolicy(matchedMemberRepository),
                eventPublisher,
                userImageS3Service
        );
    }

    @Test
    void authenticatedUserCanReadRecruitingGroupCommentsWithoutMembership() {
        Groups group = group(GroupStatus.RECRUITING);
        User user = user(30L);
        stubGroup(group);
        when(commentRepository.findVisibleTree(group.getId(), user.getId(), user.getLastResetAt())).thenReturn(List.of());
        when(matchedMemberRepository.findWriterRowsByGroupId(group.getId())).thenReturn(List.of());

        assertThat(commentService.getTree(group.getId(), user)).isEmpty();

        verify(matchedMemberRepository, never()).existsByGroup_IdAndUser_IdAndStatus(
                group.getId(), user.getId(), MemberStatus.JOINED);
    }

    @Test
    void authenticatedUserCanCreateRecruitingGroupCommentWithoutMembership() {
        Groups group = group(GroupStatus.RECRUITING);
        User user = user(30L);
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), user.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(group.getId(), user, createRequest("모집 전 댓글"));

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void secretRootCommentIsSavedWithHostAsTarget() {
        Groups group = group(GroupStatus.RECRUITING);
        User author = user(30L);
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), author.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentCreateReqDTO request = createRequest("비밀 댓글");
        ReflectionTestUtils.setField(request, "secret", true);

        var response = commentService.create(group.getId(), author, request);

        ArgumentCaptor<Comment> commentCaptor = forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        Comment saved = commentCaptor.getValue();
        assertThat(saved.isSecret()).isTrue();
        assertThat(saved.getParent()).isNull();
        assertThat(saved.getSecretTargetUserId()).isEqualTo(group.getHost().getId());
        assertThat(response.isSecret()).isTrue();
    }

    @Test
    void secretReplyIsSavedWithParentWriterAsTarget() {
        Groups group = group(GroupStatus.RECRUITING);
        User parentWriter = user(20L);
        User replyWriter = user(30L);
        Comment parent = comment(group, parentWriter);
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), replyWriter.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentCreateReqDTO request = createRequest("비밀 답글");
        ReflectionTestUtils.setField(request, "parentId", parent.getId());
        ReflectionTestUtils.setField(request, "secret", true);

        var response = commentService.create(group.getId(), replyWriter, request);

        ArgumentCaptor<Comment> commentCaptor = forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        Comment saved = commentCaptor.getValue();
        assertThat(saved.isSecret()).isTrue();
        assertThat(saved.getParent()).isEqualTo(parent);
        assertThat(saved.getSecretTargetUserId()).isEqualTo(parentWriter.getId());
        assertThat(response.isSecret()).isTrue();
    }

    @Test
    void nonHostCommentPublishesGroupCommentNotificationForHost() {
        Groups group = group(GroupStatus.RECRUITING);
        User commenter = user(30L);
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), commenter.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        commentService.create(group.getId(), commenter, createRequest("일반 사용자 댓글"));

        ArgumentCaptor<Object> eventCaptor = forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        CommentEvent event = (CommentEvent) eventCaptor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.GROUP_COMMENT_CREATED);
        assertThat(event.receiverIds()).containsExactly(group.getHost().getId());
        assertThat(event.commentId()).isEqualTo(100L);
        assertThat(event.parentCommentId()).isNull();
    }

    @Test
    void hostCommentDoesNotPublishNotification() {
        Groups group = group(GroupStatus.RECRUITING);
        User host = group.getHost();
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), host.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(group.getId(), host, createRequest("호스트 댓글"));

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void replyPublishesOnlyReplyNotificationForParentWriter() {
        Groups group = group(GroupStatus.RECRUITING);
        User parentWriter = user(20L);
        User replyWriter = user(30L);
        Comment parent = comment(group, parentWriter);
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), replyWriter.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment reply = invocation.getArgument(0);
            ReflectionTestUtils.setField(reply, "id", 101L);
            return reply;
        });

        CommentCreateReqDTO request = createRequest("답글");
        ReflectionTestUtils.setField(request, "parentId", parent.getId());
        commentService.create(group.getId(), replyWriter, request);

        ArgumentCaptor<Object> eventCaptor = forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        CommentEvent event = (CommentEvent) eventCaptor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.GROUP_COMMENT_REPLIED);
        assertThat(event.receiverIds()).containsExactly(parentWriter.getId());
        assertThat(event.commentId()).isEqualTo(101L);
        assertThat(event.parentCommentId()).isEqualTo(parent.getId());
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    void selfReplyDoesNotPublishNotification() {
        Groups group = group(GroupStatus.RECRUITING);
        User writer = user(20L);
        Comment parent = comment(group, writer);
        stubGroup(group);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), writer.getId()))
                .thenReturn(Optional.empty());
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentCreateReqDTO request = createRequest("내 댓글에 답글");
        ReflectionTestUtils.setField(request, "parentId", parent.getId());
        commentService.create(group.getId(), writer, request);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void recruitingGroupCommentCanOnlyBeDeletedByAuthor() {
        Groups group = group(GroupStatus.RECRUITING);
        User author = user(10L);
        User other = user(20L);
        Comment comment = comment(group, author);
        stubGroup(group);
        when(commentRepository.findByIdAndGroupIdWithUser(comment.getId(), group.getId()))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(group.getId(), comment.getId(), other))
                .isInstanceOf(CommentException.class);

        commentService.delete(group.getId(), comment.getId(), author);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    void joinedHostAndGuestCanReadMatchedGroupComments() {
        Groups group = group(GroupStatus.MATCHED);
        User host = user(10L);
        User guest = user(20L);
        stubGroup(group);
        allowTrackerAccess(group, host);
        allowTrackerAccess(group, guest);
        when(commentRepository.findVisibleTree(group.getId(), host.getId(), host.getLastResetAt())).thenReturn(List.of());
        when(commentRepository.findVisibleTree(group.getId(), guest.getId(), guest.getLastResetAt())).thenReturn(List.of());
        when(matchedMemberRepository.findWriterRowsByGroupId(group.getId())).thenReturn(List.of());

        assertThatCode(() -> commentService.getTree(group.getId(), host)).doesNotThrowAnyException();
        assertThatCode(() -> commentService.getTree(group.getId(), guest)).doesNotThrowAnyException();
    }

    @Test
    void hostTreeContainsPublicParentAndAuthorsSecretSelfReply() {
        Groups group = group(GroupStatus.RECRUITING);
        User author = user(20L);
        User host = group.getHost();
        Comment parent = comment(group, author);
        Comment secretReply = Comment.builder()
                .id(101L)
                .group(group)
                .user(author)
                .parent(parent)
                .secret(true)
                .secretTargetUserId(author.getId())
                .content("A의 비밀 답글")
                .build();
        stubGroup(group);
        when(commentRepository.findVisibleTree(group.getId(), host.getId(), host.getLastResetAt()))
                .thenReturn(List.of(parent, secretReply));
        when(matchedMemberRepository.findWriterRowsByGroupId(group.getId())).thenReturn(List.of());

        var tree = commentService.getTree(group.getId(), host);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getId()).isEqualTo(parent.getId());
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getId()).isEqualTo(secretReply.getId());
        assertThat(tree.get(0).getChildren().get(0).isSecret()).isTrue();
    }

    @Test
    void replyWhoseParentIsHiddenByResetBoundaryIsNotPromotedToRoot() {
        Groups group = group(GroupStatus.RECRUITING);
        User viewer = user(20L);
        User replyAuthor = user(30L);
        Comment hiddenParent = comment(group, viewer);
        Comment visibleReply = Comment.builder()
                .id(101L)
                .group(group)
                .user(replyAuthor)
                .parent(hiddenParent)
                .content("다른 사용자의 답글")
                .build();
        stubGroup(group);
        when(commentRepository.findVisibleTree(group.getId(), viewer.getId(), viewer.getLastResetAt()))
                .thenReturn(List.of(visibleReply));
        when(matchedMemberRepository.findWriterRowsByGroupId(group.getId())).thenReturn(List.of());

        var tree = commentService.getTree(group.getId(), viewer);

        assertThat(tree).isEmpty();
    }

    @Test
    void matchedGroupCommentPublishesTrackerNotificationForPartnerOnly() {
        Groups group = group(GroupStatus.MATCHED);
        User host = user(10L);
        User guest = user(20L);
        stubGroup(group);
        allowTrackerAccess(group, host);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), host.getId()))
                .thenReturn(Optional.of(com.example.bookiibookii.domain.group.enums.RoleStatus.HOST));
        when(matchedMemberRepository.findPartnerUserId(group.getId(), host.getId()))
                .thenReturn(Optional.of(guest.getId()));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        commentService.create(group.getId(), host, createRequest("호스트 댓글"));

        ArgumentCaptor<Object> eventCaptor = forClass(Object.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        TrackerNotificationEvent event =
                (TrackerNotificationEvent) eventCaptor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_COMMENT_CREATED);
        assertThat(event.receiverIds()).containsExactly(guest.getId());
        assertThat(event.commentId()).isEqualTo(100L);
    }

    @Test
    void outsiderCannotReadOrCreateMatchedGroupComments() {
        Groups group = group(GroupStatus.MATCHED);
        User outsider = user(30L);
        stubGroup(group);

        assertThatThrownBy(() -> commentService.getTree(group.getId(), outsider))
                .isInstanceOf(CommentException.class);
        assertThatThrownBy(() -> commentService.create(
                group.getId(), outsider, createRequest("접근 불가")
        )).isInstanceOf(CommentException.class);

        verify(commentRepository, never()).findVisibleTree(group.getId(), outsider.getId(), outsider.getLastResetAt());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void outsiderCannotDeleteMatchedGroupComment() {
        Groups group = group(GroupStatus.MATCHED);
        User outsider = user(30L);
        stubGroup(group);

        assertThatThrownBy(() -> commentService.delete(group.getId(), 100L, outsider))
                .isInstanceOf(CommentException.class);

        verify(commentRepository, never()).findByIdAndGroupIdWithUser(100L, group.getId());
    }

    @Test
    void matchedGroupCommentCanOnlyBeDeletedByAuthorAmongMembers() {
        Groups group = group(GroupStatus.MATCHED);
        User author = user(10L);
        User otherMember = user(20L);
        Comment comment = comment(group, author);
        stubGroup(group);
        allowTrackerAccess(group, author);
        allowTrackerAccess(group, otherMember);
        when(commentRepository.findByIdAndGroupIdWithUser(comment.getId(), group.getId()))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(group.getId(), comment.getId(), otherMember))
                .isInstanceOf(CommentException.class);

        commentService.delete(group.getId(), comment.getId(), author);

        assertThat(comment.isDeleted()).isTrue();
    }

    private void stubGroup(Groups group) {
        when(groupsRepository.findByIdWithBookAndHost(group.getId())).thenReturn(Optional.of(group));
    }

    private void allowTrackerAccess(Groups group, User user) {
        when(matchedMemberRepository.existsByGroup_IdAndUser_IdAndStatus(
                group.getId(), user.getId(), MemberStatus.JOINED
        )).thenReturn(true);
    }

    private Groups group(GroupStatus status) {
        return Groups.builder()
                .id(1L)
                .groupStatus(status)
                .host(user(10L))
                .book(Book.builder().id(1L).title("책").build())
                .build();
    }

    private User user(Long id) {
        return User.builder().id(id).nickName("user-" + id).build();
    }

    private Comment comment(Groups group, User author) {
        return Comment.builder()
                .id(100L)
                .group(group)
                .user(author)
                .content("원문")
                .build();
    }

    private CommentCreateReqDTO createRequest(String content) {
        CommentCreateReqDTO request = new CommentCreateReqDTO();
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

}
