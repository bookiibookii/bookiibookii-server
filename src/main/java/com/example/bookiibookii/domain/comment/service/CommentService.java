package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.converter.CommentConverter;
import com.example.bookiibookii.domain.comment.dto.res.CommentCreateResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentTreeResDTO;
import com.example.bookiibookii.domain.comment.dto.req.CommentCreateReqDTO;
import com.example.bookiibookii.domain.comment.entity.Comment;
import com.example.bookiibookii.domain.comment.enums.WriterRole;
import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.comment.exception.code.CommentErrorCode;
import com.example.bookiibookii.domain.comment.exception.CommentException;
import com.example.bookiibookii.domain.comment.repository.CommentRepository;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final GroupsRepository groupRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final DomainEventPublisher eventPublisher;
    private final CommentConverter commentConverter;

    @Transactional
    public CommentCreateResDTO create(Long groupId, User user, CommentCreateReqDTO req) {
        Groups group = groupRepository.findByIdWithBookAndHost(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 댓글 작성자의 그룹 내 역할
        WriterRole writerRole = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .map(r -> r == RoleStatus.HOST ? WriterRole.HOST : WriterRole.GUEST)
                .orElse(WriterRole.NONE);

        // 진행 중 그룹에 그룹 멤버가 아닌 유저가 댓글 달 시 에러처리
        if (group.getGroupStatus() != GroupStatus.RECRUITING && writerRole == WriterRole.NONE) {
            throw new CommentException(CommentErrorCode.COMMENT_WRITE_FORBIDDEN);
        }

        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new CommentException(CommentErrorCode.PARENT_COMMENT_NOT_FOUND));

            if (!parent.getGroup().getGroupId().equals(groupId)) {
                throw new CommentException(CommentErrorCode.PARENT_COMMENT_GROUP_MISMATCH);
            }

            if (parent.getParent() != null) {
                throw new CommentException(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
            }
        }

        Comment comment = Comment.builder()
                .content(req.getContent())
                .user(user)
                .group(group)
                .parent(parent)
                .build();
        Comment saved = commentRepository.save(comment);

        eventPublisher.publish(new CommentEvent(user.getNickName(), group.getBook().getTitle(), group.getHost().getId(), group.getGroupId()));

        return commentConverter.toCommentCreateResDTO(saved, writerRole);
    }

    // 그룹 페이지 내 모든 댓글 조회(대댓글 포함)
    @Transactional(readOnly = true)
    public List<CommentTreeResDTO> getTree(Long groupId) {
        List<Comment> comments =
                commentRepository.findAllByGroupIdWithUserOrderByCreatedAtAsc(groupId);

        // 그룹 멤버 역할 전체 로드 (user_id, mm.RoleStatus 가져옴)
        Map<Long, WriterRole> writerRoleMap = matchedMemberRepository.findWriterRowsByGroupId(groupId)
                .stream()
                .collect(Collectors.toMap(
                        MatchedMemberRepository.WriterRow::getUserId,
                        row -> toWriterRole(row.getRoleStatus()) // writerRow 내 RoleStatus를 WriteRole로 변환
                ));

        return commentConverter.toCommentTree(comments, writerRoleMap);
    }

    @Transactional
    public void delete(Long groupId, Long commentId, User user) {
        Comment comment = commentRepository.findByIdAndGroupIdWithUser(commentId, groupId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new CommentException(CommentErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        if (comment.isDeleted()) return;

        comment.markDeleted();
    }

    private static WriterRole toWriterRole(RoleStatus roleStatus) {
        if (roleStatus == null) return WriterRole.NONE;
        return roleStatus == RoleStatus.HOST ? WriterRole.HOST : WriterRole.GUEST;
    }
}
