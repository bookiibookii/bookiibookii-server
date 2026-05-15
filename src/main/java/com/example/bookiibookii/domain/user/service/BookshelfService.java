package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import com.example.bookiibookii.domain.user.dto.res.BookshelfResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBook;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserBookRepository;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookshelfService {

    private final UserBookRepository userBookRepository;
    private final GroupBookRepository groupBookRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final UserRepository userRepository;
    private final BookService bookService;

    private static final int MAX_FAVORITE_BOOKS = 3;

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

    // 온보딩 전용: 인생책 등록 + displayOrder 자동 부여로 대표책 동시 등록
    @Transactional
    public void replaceAllFavoriteBooks(User user, List<BookReqDTO.UserPickISBN> isbnList) {
        List<String> isbn13s = isbnList.stream()
                .filter(Objects::nonNull)
                .map(BookReqDTO.UserPickISBN::isbn13)
                .filter(isbn -> isbn != null && !isbn.isBlank())
                .distinct()
                .toList();

        if (isbn13s.size() > MAX_FAVORITE_BOOKS) {
            throw new UserException(UserErrorCode.FAVORITE_BOOK_LIMIT_EXCEEDED);
        }

        userBookRepository.deleteAllByUser(user);

        if (isbn13s.isEmpty()) return;

        List<UserBook> favorites = new ArrayList<>();
        for (int i = 0; i < isbn13s.size(); i++) {
            Book book = bookService.getOrCreateByIsbn13(isbn13s.get(i));
            favorites.add(UserBook.createFavorite(user, book, i + 1));
        }
        userBookRepository.saveAll(favorites);
    }

    // 인생 책 등록
    @Transactional
    public void addFavoriteBook(Long userId, String isbn13) {
        if (userBookRepository.countByUser_IdAndIsFavoriteTrue(userId) >= MAX_FAVORITE_BOOKS) {
            throw new UserException(UserErrorCode.FAVORITE_BOOK_LIMIT_EXCEEDED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
        Book book = bookService.getOrCreateByIsbn13(isbn13);

        Optional<UserBook> existing = userBookRepository.findByUser_IdAndBook_Id(userId, book.getId());
        if (existing.isPresent()) {
            UserBook ub = existing.get();
            if (ub.isFavorite()) {
                throw new UserException(UserErrorCode.FAVORITE_BOOK_ALREADY_EXISTS);
            }
            ub.updateIsFavorite(true);
            return;
        }

        userBookRepository.save(UserBook.create(user, book, true));
    }

    // 인생 책 삭제
    @Transactional
    public void deleteFavoriteBook(Long userId, Long userBookId) {
        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));

        if (!userBook.isFavorite()) {
            throw new UserException(UserErrorCode.USER_BOOK_NOT_FOUND);
        }

        userBookRepository.delete(userBook);
    }
}
