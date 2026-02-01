package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.userbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserBookRepository userBookRepository;
    private final UserImageS3Service userImageS3Service;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * 현재 사용자의 라이브러리(UserBook 목록)를 조회합니다.
     * user_id = userId 인 UserBook 목록을 그룹·책·호스트·호스트 프로필 이미지와 함께 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<LibraryBookResponseDTO> getLibraryBooks(Long userId) {
        List<UserBook> userBooks = userBookRepository.findAllByUser_IdWithGroupAndBookAndHost(userId);
        return userBooks.stream()
                .map(this::toLibraryBookResponseDTO)
                .toList();
    }

    private LibraryBookResponseDTO toLibraryBookResponseDTO(UserBook ub) {
        var group = Objects.requireNonNull(ub.getGroup(), "UserBook.group is null");
        var book = Objects.requireNonNull(group.getBook(), "Group.book is null");
        var host = Objects.requireNonNull(group.getHost(), "Group.host is null");

        String hostProfileImageUrl = null;
        if (host.getUserImage() != null) {
            try {
                hostProfileImageUrl = userImageS3Service.generatePresignedGetUrl(
                        host.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
            } catch (Exception e) {
                log.warn("호스트 프로필 이미지 Presigned URL 생성 실패 (hostId={})", host.getId(), e);
            }
        }

        return LibraryBookResponseDTO.builder()
                .userBookId(ub.getId())
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .image(book.getImage())
                .hostId(host.getId())
                .hostProfileImageUrl(hostProfileImageUrl)
                .startDate(group.getStartDate())
                .duration(group.getReadingPeriod())
                .rating(ub.getRating())
                .comment(ub.getComment())
                .build();
    }
}
