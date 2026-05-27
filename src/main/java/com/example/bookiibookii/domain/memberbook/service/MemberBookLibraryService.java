package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.memberbook.dto.res.LibraryMemberBookResponseDTO;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBookLibraryService {

    private final MemberBookRepository memberBookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final UserImageS3Service userImageS3Service;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * 서재에서만 제거(소프트 삭제). 그룹·카드·상대 MemberBook은 삭제되지 않습니다.
     * 본인 MatchedMember에 속한 MemberBook만 제거할 수 있습니다.
     */
    @Transactional
    public void removeFromLibrary(Long memberBookId, Long userId) {
        MemberBook memberBook = memberBookRepository.findByIdAndMatchedMember_User_Id(memberBookId, userId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_BOOK_NOT_FOUND));
        memberBook.markRemoved();
    }

    /**
     * 현재 사용자의 라이브러리(MemberBook 목록)를 조회합니다.
     * matchedMember.user_id = userId 이고 removedAt IS NULL 인 MemberBook만 반환합니다.
     * 그룹당 최대 2권(멤버북)이 각각 별도 항목으로 노출됩니다.
     */
    @Transactional(readOnly = true)
    public List<LibraryMemberBookResponseDTO> getLibraryMemberBooks(Long userId) {
        return toLibraryMemberBookResponseList(
                memberBookRepository.findAllByMatchedMember_User_IdWithGroupAndBookAndHost(userId)
        );
    }

    @Transactional(readOnly = true)
    public List<LibraryMemberBookResponseDTO> searchLibraryMemberBooks(Long userId, String keyword) {
        String normalizedKeyword = (keyword == null) ? null : keyword.trim();
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return getLibraryMemberBooks(userId);
        }
        return toLibraryMemberBookResponseList(memberBookRepository.searchMyLibrary(userId, normalizedKeyword));
    }

    private List<LibraryMemberBookResponseDTO> toLibraryMemberBookResponseList(List<MemberBook> memberBooks) {
        List<MemberBook> validMemberBooks = memberBooks.stream()
                .filter(this::isValidForLibraryList)
                .toList();

        if (validMemberBooks.isEmpty()) {
            return List.of();
        }

        List<Long> memberBookIds = validMemberBooks.stream().map(MemberBook::getId).toList();
        Map<Long, BookReview> reviewMap = bookReviewRepository.findByMemberBook_IdIn(memberBookIds).stream()
                .collect(Collectors.toMap(br -> br.getMemberBook().getId(), br -> br));

        return validMemberBooks.stream()
                .map(mb -> toLibraryMemberBookResponseDTO(
                        mb,
                        reviewMap.get(mb.getId())
                ))
                .toList();
    }

    private boolean isValidForLibraryList(MemberBook memberBook) {
        if (memberBook.getGroup() != null && memberBook.getBook() != null && memberBook.getGroup().getHost() != null) {
            return true;
        }
        log.warn(
                "라이브러리 목록에서 제외: memberBookId={}, hasGroup={}, hasBook={}, hasHost={}",
                memberBook.getId(),
                memberBook.getGroup() != null,
                memberBook.getBook() != null,
                memberBook.getGroup() != null && memberBook.getGroup().getHost() != null
        );
        return false;
    }

    private LibraryMemberBookResponseDTO toLibraryMemberBookResponseDTO(
            MemberBook memberBook,
            BookReview bookReview
    ) {
        var group = memberBook.getGroup();
        var book = memberBook.getBook();
        var host = group.getHost();

        String hostProfileImageUrl = null;
        if (host.getUserImage() != null) {
            try {
                hostProfileImageUrl = userImageS3Service.generatePresignedGetUrl(
                        host.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
            } catch (Exception e) {
                log.warn("호스트 프로필 이미지 Presigned URL 생성 실패", e);
            }
        }

        LocalDate finalEndDate = null;
        if (group.getStartDate() != null && group.getReadingPeriod() != null) {
            finalEndDate = group.getStartDate().plusDays(group.getReadingPeriod());
        }

        Double rating = bookReview != null ? bookReview.getStar() : null;
        String comment = bookReview != null ? bookReview.getComment() : null;

        return LibraryMemberBookResponseDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .memberBookId(memberBook.getId())
                .isMine(memberBook.isMine())
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .image(book.getImage())
                .hostId(host.getId())
                .hostNickName(host.getNickName())
                .hostProfileImageUrl(hostProfileImageUrl)
                .groupType(group.getGroupType())
                .groupStatus(group.getGroupStatus())
                .startDate(group.getStartDate())
                .endDate(finalEndDate)
                .duration(group.getReadingPeriod())
                .progressRate(calculateProgressRate(memberBook.getCurrentPage(), book.getTotalPages()))
                .rating(rating)
                .comment(comment)
                .build();
    }

    private int calculateProgressRate(Integer currentPage, Integer totalPages) {
        if (currentPage == null || totalPages == null || totalPages <= 0) {
            return 0;
        }
        int normalizedPage = Math.min(Math.max(currentPage, 0), totalPages);
        return (normalizedPage * 100) / totalPages;
    }
}
