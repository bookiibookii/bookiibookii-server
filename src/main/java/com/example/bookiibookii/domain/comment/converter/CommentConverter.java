package com.example.bookiibookii.domain.comment.converter;

import com.example.bookiibookii.domain.comment.dto.CardWriterDto;
import com.example.bookiibookii.domain.comment.dto.WriterDto;
import com.example.bookiibookii.domain.comment.dto.res.CardCommentResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentCreateResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentTreeResDTO;
import com.example.bookiibookii.domain.comment.entity.CardComment;
import com.example.bookiibookii.domain.comment.entity.Comment;
import com.example.bookiibookii.domain.comment.enums.WriterRole;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommentConverter {
    private final UserImageS3Service userImageS3Service;
    private static final int EXPIRATION_MINUTES = 60;

    // --- 카드 댓글(CardComment) 관련 ---

    public CardCommentResDTO.Comment toCardCommentDto(CardComment c) {
        return new CardCommentResDTO.Comment(
                c.getId(),
                c.getContent(),
                toCardWriterDto(c.getUser()),
                c.getCreatedAt()
        );
    }

    private CardWriterDto toCardWriterDto(User u) {
        String profileImageUrl = u.getUserImage() != null
                ? userImageS3Service.generatePresignedGetUrl(u.getUserImage().getS3Key(), EXPIRATION_MINUTES)
                : null;

        return CardWriterDto.builder()
                .userId(u.getId())
                .name(u.getNickName())
                .profileImage(profileImageUrl)
                .build();
    }

    // --- 그룹 댓글(Comment) 관련 ---

    public CommentCreateResDTO toCommentCreateResDTO(Comment c, WriterRole writerRole) {
        return CommentCreateResDTO.builder()
                .commentId(c.getId())
                .groupId(c.getGroup().getGroupId())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .writer(toWriterDto(c.getUser(), writerRole))
                .build();
    }

    /**
     * 댓글 리스트를 계층형 트리 구조로 변환
     */
    public List<CommentTreeResDTO> toCommentTree(List<Comment> comments, Map<Long, WriterRole> writerRoleMap) {
        Map<Long, CommentTreeResDTO> map = new LinkedHashMap<>();

        // 1) DTO 생성 및 매핑
        for (Comment c : comments) {
            WriterRole role = writerRoleMap.getOrDefault(c.getUser().getId(), WriterRole.NONE);
            map.put(c.getId(), toTreeDTO(c, role));
        }

        // 2) 부모-자식 관계 조립
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

    private CommentTreeResDTO toTreeDTO(Comment c, WriterRole writerRole) {
        return CommentTreeResDTO.builder()
                .id(c.getId())
                .deleted(c.isDeleted())
                .content(c.isDeleted() ? "삭제된 댓글입니다." : c.getContent())
                .parentId(c.getParent() == null ? null : c.getParent().getId())
                .createdAt(c.getCreatedAt())
                .writer(toWriterDto(c.getUser(), writerRole))
                .build();
    }

    private WriterDto toWriterDto(User u, WriterRole writerRole) {
        String profileImageUrl = u.getUserImage() != null
                ? userImageS3Service.generatePresignedGetUrl(u.getUserImage().getS3Key(), EXPIRATION_MINUTES)
                : null;
        return WriterDto.builder()
                .userId(u.getId())
                .name(u.getNickName())
                .profileImage(profileImageUrl)
                .role(writerRole)
                .build();
    }
}
