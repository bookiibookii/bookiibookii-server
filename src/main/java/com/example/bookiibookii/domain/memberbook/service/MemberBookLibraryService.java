package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.memberbook.dto.res.LibraryMemberBookResponseDTO;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
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
    private final TrackerRepository trackerRepository;

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
        List<MemberBook> memberBooks = memberBookRepository.findAllByMatchedMember_User_IdWithGroupAndBookAndHost(userId);

        List<MemberBook> validMemberBooks = memberBooks.stream()
                .filter(this::isValidForLibraryList)
                .toList();

        if (validMemberBooks.isEmpty()) {
            return List.of();
        }

        List<Long> memberBookIds = validMemberBooks.stream().map(MemberBook::getId).toList();
        Map<Long, BookReview> reviewMap = bookReviewRepository.findByMemberBook_IdIn(memberBookIds).stream()
                .collect(Collectors.toMap(br -> br.getMemberBook().getId(), br -> br));

        List<Long> groupIds = validMemberBooks.stream()
                .map(mb -> mb.getGroup().getGroupId())
                .distinct()
                .toList();

        Map<Long, Tracker> trackerMap = trackerRepository.findByGroup_GroupIdIn(groupIds).stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t));

        return validMemberBooks.stream()
                .map(mb -> toLibraryMemberBookResponseDTO(
                        mb,
                        reviewMap.get(mb.getId()),
                        trackerMap.get(mb.getGroup().getGroupId())
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
            BookReview bookReview,
            Tracker tracker
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
        if (tracker != null && tracker.getEndDate() != null) {
            finalEndDate = tracker.getEndDate().toLocalDate();
        } else if (group.getStartDate() != null && group.getReadingPeriod() != null) {
            finalEndDate = group.getStartDate().plusDays(group.getReadingPeriod());
        }

        Double rating = bookReview != null ? bookReview.getStar() : null;
        String comment = bookReview != null ? bookReview.getComment() : null;

        return LibraryMemberBookResponseDTO.builder()
                .groupId(group.getGroupId())
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
                .progressRate(memberBook.getProgressRate())
                .rating(rating)
                .comment(comment)
                .build();
    }
}
