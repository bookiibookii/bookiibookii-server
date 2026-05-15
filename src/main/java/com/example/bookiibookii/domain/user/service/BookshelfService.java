package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import com.example.bookiibookii.domain.user.dto.res.BookshelfResponseDTO;
import com.example.bookiibookii.domain.user.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookshelfService {

    private final UserBookRepository userBookRepository;
    private final GroupBookRepository groupBookRepository;
    private final MatchedMemberRepository matchedMemberRepository;

    public BookshelfResponseDTO.BookshelfResDTO getBookshelf(Long userId) {
        return BookshelfResponseDTO.BookshelfResDTO.builder()
                .completedBooks(buildCompletedBooks(userId))
                .favoriteBooks(buildFavoriteBooks(userId))
                .representativeBooks(buildRepresentativeBooks(userId))
                .build();
    }

    // 완독 + 리뷰 완료 책 목록 조회
    private List<BookshelfResponseDTO.CompletedBookDto> buildCompletedBooks(Long userId) {
        List<GroupBook> groupBooks = groupBookRepository.findCompletedBooksByUserId(userId);

        Map<Long, LocalDateTime> completionDateByGroupId = matchedMemberRepository
                .findCompletedByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        mm -> mm.getGroup().getGroupId(),
                        MatchedMember::getCompletedAt,
                        (a, b) -> a
                ));

        return groupBooks.stream()
                .map(gb -> {
                    var book = gb.getGroup().getBook();
                    LocalDateTime completedAt = completionDateByGroupId.get(gb.getGroup().getGroupId());
                    return new BookshelfResponseDTO.CompletedBookDto(
                            gb.getId(),
                            book.getTitle(),
                            book.getAuthor(),
                            book.getImage(),
                            book.getCategory() != null ? book.getCategory().name() : null,
                            gb.getRating(),
                            completedAt != null ? completedAt.toLocalDate() : null
                    );
                })
                .toList();
    }

    // 인생 책 조회
    private List<BookshelfResponseDTO.FavoriteBookDto> buildFavoriteBooks(Long userId) {
        return userBookRepository.findFavoriteBooksByUserId(userId)
                .stream()
                .map(ub -> new BookshelfResponseDTO.FavoriteBookDto(
                        ub.getId(),
                        ub.getBook().getTitle(),
                        ub.getBook().getAuthor(),
                        ub.getBook().getCategory() != null ? ub.getBook().getCategory().name() : null,
                        ub.getBook().getImage()
                ))
                .toList();
    }

    // 나를 대표하는 책 조회
    private List<BookshelfResponseDTO.RepresentativeBookDto> buildRepresentativeBooks(Long userId) {
        return userBookRepository.findRepresentativeBooks(userId)
                .stream()
                .map(ub -> new BookshelfResponseDTO.RepresentativeBookDto(
                        ub.getId(),
                        ub.getBook().getTitle(),
                        ub.getDisplayOrder(),
                        ub.isFavorite()
                ))
                .toList();
    }
}
