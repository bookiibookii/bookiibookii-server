package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.dto.CardWriterDto;
import com.example.bookiibookii.domain.comment.dto.req.CardCommentReqDTO;
import com.example.bookiibookii.domain.comment.dto.res.CardCommentResDTO;
import com.example.bookiibookii.domain.comment.entity.CardComment;
import com.example.bookiibookii.domain.comment.exception.CommentException;
import com.example.bookiibookii.domain.comment.exception.code.CommentErrorCode;
import com.example.bookiibookii.domain.comment.repository.CardCommentRepository;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import org.springframework.stereotype.Service;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCommentService {

    private final CardRepository cardRepository;
    private final CardCommentRepository cardCommentRepository;
    private final UserImageS3Service userImageS3Service;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Transactional
    public CardCommentResDTO.Create create(Long cardId, User user, CardCommentReqDTO.Create req) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        CardComment saved = cardCommentRepository.save(
                CardComment.builder()
                        .card(card)
                        .user(user)
                        .content(req.content().trim())
                        .build()
        );

        return new CardCommentResDTO.Create(saved.getId());
    }

    public CardCommentResDTO.ListResponse getList(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardImageException(CardImageErrorCode.CARD_NOT_FOUND);
        }

        var comments = cardCommentRepository.findAllByCardIdWithUserOrderByCreatedAtAsc(cardId).stream()
                .map(this::toCommentDto)
                .toList();
        long totalCount = comments.size();

        return new CardCommentResDTO.ListResponse(totalCount, comments);
    }

    @Transactional
    public void delete(Long cardId, Long commentId, User user) {
        CardComment comment = cardCommentRepository.findByIdAndCardId(commentId, cardId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new CommentException(CommentErrorCode.NO_PERMISSION);
        }

        cardCommentRepository.delete(comment);
    }

    // converter
    private CardCommentResDTO.Comment toCommentDto(CardComment c) {
        return new CardCommentResDTO.Comment(
                c.getId(),
                c.getContent(),
                toWriterDto(c.getUser()),
                c.getCreatedAt()
        );
    }

    private CardWriterDto toWriterDto(User u) {
        String profileImageUrl = u.getUserImage() != null
                ? userImageS3Service.generatePresignedGetUrl(
                u.getUserImage().getS3Key(),
                PRESIGNED_GET_URL_EXPIRATION_MINUTES
        )
                : null;

        return CardWriterDto.builder()
                .userId(u.getId())
                .name(u.getNickName())
                .profileImage(profileImageUrl)
                .build();
    }
}