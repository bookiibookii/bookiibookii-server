package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.userbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserBookRepository userBookRepository;
    private final UserImageS3Service userImageS3Service;
    private final TrackerRepository trackerRepository;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * 서재에서만 제거(소프트 삭제). 그룹·카드는 삭제되지 않고, 다른 멤버는 계속 조회 가능.
     * 본인 소유 UserBook만 제거 가능.
     */
    @Transactional
    public void removeFromLibrary(Long userBookId, Long userId) {
        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND));
        userBook.markRemoved();
    }

    /**
     * 현재 사용자의 라이브러리(UserBook 목록)를 조회합니다.
     * user_id = userId 이고 removedAt IS NULL 인 UserBook만 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<LibraryBookResponseDTO> getLibraryBooks(Long userId) {
        List<UserBook> userBooks = userBookRepository.findAllByUserId(userId);
        List<Long> groupIds = userBooks.stream().map(ub -> ub.getGroup().getGroupId()).toList();

        // 그룹 ID들로 트래커 맵 생성 (Map<GroupId, Tracker>)
        Map<Long, Tracker> trackerMap = trackerRepository.findByGroup_GroupIdIn(groupIds).stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t));

        return userBooks.stream()
                .map(ub -> toLibraryBookResponseDTO(ub, trackerMap.get(ub.getGroup().getGroupId())))
                .toList();
    }

    private LibraryBookResponseDTO toLibraryBookResponseDTO(UserBook ub, Tracker tracker) {
        var group = Objects.requireNonNull(ub.getGroup(), "UserBook.group is null");
        var book = Objects.requireNonNull(group.getBook(), "Group.book is null");
        var host = Objects.requireNonNull(group.getHost(), "Group.host is null");

        String hostProfileImageUrl = null;
        if (host.getUserImage() != null) {
            try {
                hostProfileImageUrl = userImageS3Service.generatePresignedGetUrl(
                        host.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
            } catch (Exception e) {
                log.warn("호스트 프로필 이미지 Presigned URL 생성 실패", e);
            }
        }

        LocalDate finalEndDate = (tracker != null && tracker.getEndDate() != null)
                ? tracker.getEndDate().toLocalDate()
                : group.getStartDate().plusDays(group.getReadingPeriod());

        return LibraryBookResponseDTO.builder()
                .groupId(group.getGroupId())
                .userBookId(ub.getId())
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .image(book.getImage())
                .hostId(host.getId())
                .hostNickName(host.getNickName())
                .hostProfileImageUrl(hostProfileImageUrl)
                .groupType(group.getGroupType())
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
        // 1. 키워드가 없으면 전체 목록 조회
        if (keyword == null || keyword.isBlank()) {
            return getLibraryBooks(userId);
        }

        // 2. 키워드로 검색된 UserBook 목록 조회
        List<UserBook> userBooks = userBookRepository.searchMyLibrary(userId, keyword);

        // 검색 결과에 해당하는 그룹 ID들로 트래커 맵 생성
        List<Long> groupIds = userBooks.stream()
                .map(ub -> ub.getGroup().getGroupId())
                .toList();

        Map<Long, Tracker> trackerMap = trackerRepository.findByGroup_GroupIdIn(groupIds).stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t));

        // 3. 트래커 정보를 포함하여 DTO 변환 (기존 메서드 활용)
        return userBooks.stream()
                .map(ub -> toLibraryBookResponseDTO(ub, trackerMap.get(ub.getGroup().getGroupId())))
                .toList();
    }
}
