package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.review.dto.res.MypageReviewResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.review.repository.MemberReviewRepository;
import com.example.bookiibookii.domain.tracker.resolver.UserProfileImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MypageReviewService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final BookReviewRepository bookReviewRepository;
    private final MemberReviewRepository memberReviewRepository;
    private final UserProfileImageUrlResolver userProfileImageUrlResolver;

    @Transactional(readOnly = true)
    public MypageReviewResponseDTO.WrittenReviews getWrittenReviews(Long userId, Pageable pageable) {
        Page<BookReview> reviews = bookReviewRepository.findWrittenReviewsByUserId(userId, pageable);

        return new MypageReviewResponseDTO.WrittenReviews(
                reviews.getTotalElements(),
                reviews.getContent().stream().map(this::toWrittenReviewItem).toList(),
                toPageInfo(reviews)
        );
    }

    @Transactional(readOnly = true)
    public MypageReviewResponseDTO.ReceivedReviews getReceivedReviews(Long userId, Pageable pageable) {
        Page<MemberReview> reviews = memberReviewRepository.findReceivedReviewsByUserId(userId, pageable);
        long positiveCount = memberReviewRepository.countByTargetUserIdAndReaction(
                userId,
                MemberReviewReaction.BOOM_UP
        );

        return new MypageReviewResponseDTO.ReceivedReviews(
                positiveCount,
                reviews.getContent().stream().map(this::toReceivedReviewItem).toList(),
                toPageInfo(reviews)
        );
    }

    private MypageReviewResponseDTO.WrittenReviewItem toWrittenReviewItem(BookReview review) {
        var memberBook = review.getMemberBook();
        var book = memberBook.getBook();
        ExchangeType exchangeType = ExchangeType.from(memberBook.getGroup().getTradeType());

        return new MypageReviewResponseDTO.WrittenReviewItem(
                review.getId(),
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                review.getStar(),
                review.getComment(),
                exchangeType,
                exchangeTypeLabel(exchangeType),
                review.getCreatedAt().format(DATE_FMT)
        );
    }

    private MypageReviewResponseDTO.ReceivedReviewItem toReceivedReviewItem(MemberReview review) {
        var reviewer = review.getWriter().getUser();

        return new MypageReviewResponseDTO.ReceivedReviewItem(
                review.getId(),
                reviewer.getId(),
                reviewer.getNickName(),
                userProfileImageUrlResolver.resolve(reviewer),
                review.getReaction(),
                partnerReviewLabel(review.getReaction()),
                review.getComment(),
                review.getCreatedAt().format(DATE_FMT)
        );
    }

    private String exchangeTypeLabel(ExchangeType exchangeType) {
        if (exchangeType == null) {
            return null;
        }
        return switch (exchangeType) {
            case DIRECT -> "직접";
            case DELIVERY -> "택배";
        };
    }

    private String partnerReviewLabel(MemberReviewReaction reaction) {
        if (reaction == null) {
            return null;
        }
        return switch (reaction) {
            case BOOM_UP -> "좋아요";
            case BOOM_DOWN -> "별로였어요";
        };
    }

    private MypageReviewResponseDTO.PageInfo toPageInfo(Page<?> page) {
        return new MypageReviewResponseDTO.PageInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
