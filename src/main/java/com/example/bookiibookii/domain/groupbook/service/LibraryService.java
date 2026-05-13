package com.example.bookiibookii.domain.groupbook.service;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.groupbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.groupbook.exception.CardImageException;
import com.example.bookiibookii.domain.groupbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
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
public class LibraryService {

    private final GroupBookRepository groupBookRepository;
    private final UserImageS3Service userImageS3Service;
    private final TrackerRepository trackerRepository;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * 서재에서만 제거(소프트 삭제). 그룹·카드는 삭제되지 않고, 다른 멤버는 계속 조회 가능.
     * 본인 소유 GroupBook만 제거 가능.
     */
    @Transactional
    public void removeFromLibrary(Long groupBookId, Long userId) {
        GroupBook groupBook = groupBookRepository.findByIdAndUser_Id(groupBookId, userId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.GROUP_BOOK_NOT_FOUND));
        groupBook.markRemoved();
    }

    /**
     * 현재 사용자의 라이브러리(GroupBook 목록)를 조회합니다.
     * user_id = userId 이고 removedAt IS NULL 인 GroupBook만 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<LibraryBookResponseDTO> getLibraryBooks(Long userId) {
        List<GroupBook> groupBooks = groupBookRepository.findAllByUser_IdWithGroupAndBookAndHost(userId);

        if (groupBooks.isEmpty()) return List.of();

        List<Long> groupIds = groupBooks.stream().map(ub -> ub.getGroup().getGroupId()).toList();

        Map<Long, Tracker> trackerMap = trackerRepository.findByGroup_GroupIdIn(groupIds).stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t));

        return groupBooks.stream()
                .map(ub -> toLibraryBookResponseDTO(ub, trackerMap.get(ub.getGroup().getGroupId())))
                .toList();
    }

    private LibraryBookResponseDTO toLibraryBookResponseDTO(GroupBook ub, Tracker tracker) {
        if (ub.getGroup() == null || ub.getGroup().getBook() == null || ub.getGroup().getHost() == null) {
            throw new CardImageException(CardImageErrorCode.GROUP_BOOK_NOT_FOUND);
        }

        var group = ub.getGroup();
        var book = group.getBook();
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

        return LibraryBookResponseDTO.builder()
                .groupId(group.getGroupId())
                .groupBookId(ub.getId())
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
                .rating(ub.getRating())
                .comment(ub.getComment())
                .build();
    }

    //서재검색
    @Transactional(readOnly = true)
    public List<LibraryBookResponseDTO> searchLibraryBooks(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getLibraryBooks(userId);
        }

        List<GroupBook> groupBooks = groupBookRepository.searchMyLibrary(userId, keyword);

        if (groupBooks.isEmpty()) return List.of();

        List<Long> groupIds = groupBooks.stream()
                .map(ub -> ub.getGroup().getGroupId())
                .toList();

        Map<Long, Tracker> trackerMap = trackerRepository.findByGroup_GroupIdIn(groupIds).stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t));

        return groupBooks.stream()
                .map(ub -> toLibraryBookResponseDTO(ub, trackerMap.get(ub.getGroup().getGroupId())))
                .toList();
    }
}