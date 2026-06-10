package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.dto.WriterDto;
import com.example.bookiibookii.domain.comment.dto.res.CommentCreateResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentTreeResDTO;
import com.example.bookiibookii.domain.comment.dto.req.CommentCreateReqDTO;
import com.example.bookiibookii.domain.comment.entity.Comment;
import com.example.bookiibookii.domain.comment.enums.CommentContext;
import com.example.bookiibookii.domain.comment.enums.WriterRole;
import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.comment.exception.code.CommentErrorCode;
import com.example.bookiibookii.domain.comment.exception.CommentException;
import com.example.bookiibookii.domain.comment.repository.CommentRepository;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final GroupsRepository groupRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final CommentAccessPolicy commentAccessPolicy;
    private final DomainEventPublisher eventPublisher;
    private final UserImageS3Service userImageS3Service;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Transactional
    public CommentCreateResDTO create(Long groupId, User user, CommentCreateReqDTO req) {
        Groups group = groupRepository.findByIdWithBookAndHost(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        CommentContext context = commentAccessPolicy.resolveContext(group);
        commentAccessPolicy.validateAccess(context, groupId, user);

        WriterRole writerRole = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .map(r -> r == RoleStatus.HOST ? WriterRole.HOST : WriterRole.GUEST)
                .orElse(WriterRole.NONE);

        return switch (context) {
            case GROUP_DETAIL -> createGroupDetailComment(group, user, req, writerRole);
            case TRACKER -> createTrackerComment(group, user, req, writerRole);
        };
    }

    private CommentCreateResDTO createGroupDetailComment(
            Groups group,
            User user,
            CommentCreateReqDTO req,
            WriterRole writerRole
    ) {
        Comment saved = saveComment(group, user, req);
        publishGroupDetailCommentNotification(group, user, saved);
        return toCreateResDTO(saved, writerRole);
    }

    private CommentCreateResDTO createTrackerComment(
            Groups group,
            User user,
            CommentCreateReqDTO req,
            WriterRole writerRole
    ) {
        Comment saved = saveComment(group, user, req);
        // NOTI-TRK-003 is intentionally implemented in a later PR.
        return toCreateResDTO(saved, writerRole);
    }

    private void publishGroupDetailCommentNotification(Groups group, User writer, Comment comment) {
        Comment parent = comment.getParent();
        if (parent != null) {
            Long parentWriterId = parent.getUser().getId();
            if (!parentWriterId.equals(writer.getId())) {
                eventPublisher.publish(new CommentEvent(
                        NotificationType.GROUP_COMMENT_REPLIED,
                        writer.getNickName(),
                        groupTitle(group),
                        List.of(parentWriterId),
                        group.getId(),
                        comment.getId(),
                        parent.getId()
                ));
            }
            return;
        }

        Long hostId = group.getHost().getId();
        if (hostId.equals(writer.getId())) return;

        eventPublisher.publish(new CommentEvent(
                NotificationType.GROUP_COMMENT_CREATED,
                writer.getNickName(),
                groupTitle(group),
                List.of(hostId),
                group.getId(),
                comment.getId(),
                null
        ));
    }

    private String groupTitle(Groups group) {
        if (group.getGroupName() != null && !group.getGroupName().isBlank()) {
            return group.getGroupName();
        }
        return group.getBook().getTitle();
    }

    private Comment saveComment(Groups group, User user, CommentCreateReqDTO req) {
        Comment parent = null;
        if (req.getParentId() != null) {
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new CommentException(CommentErrorCode.PARENT_COMMENT_NOT_FOUND));

            if (!parent.getGroup().getId().equals(group.getId())) {
                throw new CommentException(CommentErrorCode.PARENT_COMMENT_GROUP_MISMATCH);
            }

            if (parent.getParent() != null) {
                throw new CommentException(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
            }
        }

        boolean isSecret = req.isSecret();
        if (isSecret && parent == null) {
            throw new CommentException(CommentErrorCode.SECRET_REPLY_ONLY);
        }

        Long secretTargetUserId = null;
        if (isSecret) {
            secretTargetUserId = parent.getUser().getId();
        }

        Comment comment = Comment.builder()
                .content(req.getContent())
                .user(user)
                .group(group)
                .parent(parent)
                .secret(isSecret)
                .secretTargetUserId(secretTargetUserId)
                .build();
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentTreeResDTO> getTree(Long groupId, User viewer) {
        Groups group = getGroup(groupId);
        CommentContext context = commentAccessPolicy.resolveContext(group);
        commentAccessPolicy.validateAccess(context, groupId, viewer);

        List<Comment> comments = commentRepository.findVisibleTree(groupId, viewer.getId());

        // 그룹 멤버 역할 전체 로드 (user_id, mm.RoleStatus 가져옴)
        Map<Long, WriterRole> writerRoleMap = matchedMemberRepository.findWriterRowsByGroupId(groupId)
                .stream()
                .collect(Collectors.toMap(
                        MatchedMemberRepository.WriterRow::getUserId,
                        row -> toWriterRole(row.getRoleStatus()) // writerRow 내 RoleStatus를 WriteRole로 변환
                ));

        // 1) id -> dto 매핑 (writerRole은 Map에서 꺼내고 없으면 NONE)
        Map<Long, CommentTreeResDTO> map = new LinkedHashMap<>();
        for (Comment c : comments) {
            Long userId = c.getUser().getId();
            WriterRole writerRole = writerRoleMap.getOrDefault(userId, WriterRole.NONE);
            map.put(c.getId(), toTreeDTO(c, writerRole));
        }

        // 2) 부모-자식 연결 + 루트 수집
        List<CommentTreeResDTO> roots = new ArrayList<>();
        for (CommentTreeResDTO dto : map.values()) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentTreeResDTO parent = map.get(dto.getParentId());
                if (parent != null) parent.addChild(dto);
                else roots.add(dto);
            }
        }

        return roots;
    }

    @Transactional
    public void delete(Long groupId, Long commentId, User user) {
        Groups group = getGroup(groupId);
        CommentContext context = commentAccessPolicy.resolveContext(group);
        commentAccessPolicy.validateAccess(context, groupId, user);

        Comment comment = getComment(groupId, commentId);

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new CommentException(CommentErrorCode.COMMENT_DELETE_FORBIDDEN);
        }

        if (comment.isDeleted()) return;

        comment.markDeleted();
    }

    private Groups getGroup(Long groupId) {
        return groupRepository.findByIdWithBookAndHost(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    private Comment getComment(Long groupId, Long commentId) {
        return commentRepository.findByIdAndGroupIdWithUser(commentId, groupId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private static WriterRole toWriterRole(RoleStatus roleStatus) {
        if (roleStatus == null) return WriterRole.NONE;
        return roleStatus == RoleStatus.HOST ? WriterRole.HOST : WriterRole.GUEST;
    }

    // converter
    private CommentCreateResDTO toCreateResDTO(Comment c, WriterRole writerRole) {
        return CommentCreateResDTO.builder()
                .commentId(c.getId())
                .groupId(c.getGroup().getId())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .writer(toWriterDto(c.getUser(), writerRole))
                .build();
    }

    private CommentTreeResDTO toTreeDTO(Comment c, WriterRole writerRole) {
        return CommentTreeResDTO.builder()
                .id(c.getId())
                .deleted(c.isDeleted())
                .secret(c.isSecret())
                .content(c.isDeleted() ? "삭제된 댓글입니다." : c.getContent())
                .parentId(c.getParent() == null ? null : c.getParent().getId())
                .createdAt(c.getCreatedAt())
                .writer(toWriterDto(c.getUser(), writerRole))
                .build();
    }

    private WriterDto toWriterDto(User u, WriterRole writerRole) {
        String profileImageUrl = u.getUserImage() != null
                ? userImageS3Service.generatePresignedGetUrl(u.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES)
                : null;
        return WriterDto.builder()
                .userId(u.getId())
                .name(u.getNickName())
                .profileImageUrl(profileImageUrl)
                .role(writerRole)
                .build();
    }
}
