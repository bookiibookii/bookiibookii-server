package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.userbook.converter.UserBookConverter;
import com.example.bookiibookii.domain.userbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserBookRepository userBookRepository;
    private final TrackerRepository trackerRepository;
    private final UserBookConverter userBookConverter;

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
        List<UserBook> userBooks = userBookRepository.findAllByUser_IdWithGroupAndBookAndHost(userId);

        if (userBooks.isEmpty()) return List.of();

        return convertToLibraryDTOs(userBooks);
    }

    //서재검색
    @Transactional(readOnly = true)
    public List<LibraryBookResponseDTO> searchLibraryBooks(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getLibraryBooks(userId);
        }

        List<UserBook> userBooks = userBookRepository.searchMyLibrary(userId, keyword);

        if (userBooks.isEmpty()) return List.of();

        return convertToLibraryDTOs(userBooks);
    }

    /**
     * 공통 변환 로직 (내부 헬퍼 메서드)
     */
    private List<LibraryBookResponseDTO> convertToLibraryDTOs(List<UserBook> userBooks) {
        List<Long> groupIds = userBooks.stream()
                .map(ub -> ub.getGroup().getGroupId())
                .toList();

        Map<Long, Tracker> trackerMap = trackerRepository.findByGroup_GroupIdIn(groupIds).stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t));

        return userBooks.stream()
                .map(ub -> userBookConverter.toLibraryBookResponseDTO(
                        ub,
                        trackerMap.get(ub.getGroup().getGroupId()),
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .toList();
    }
}