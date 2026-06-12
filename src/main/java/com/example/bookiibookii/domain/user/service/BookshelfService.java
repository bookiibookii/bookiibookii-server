package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
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
    private final MemberBookRepository memberBookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final UserRepository userRepository;
    private final BookService bookService;

    private static final int MAX_FAVORITE_BOOKS = 3;
    private static final int MAX_REPRESENTATIVE_BOOKS = 7;

    public BookshelfResponseDTO.BookshelfResDTO getBookshelf(Long userId) {
        return BookshelfResponseDTO.BookshelfResDTO.builder()
                .completedBooks(buildCompletedBooks(userId))
                .favoriteBooks(buildFavoriteBooks(userId))
                .representativeBooks(buildRepresentativeBooks(userId))
                .build();
    }

    // 완독 + 리뷰 완료 책 목록 조회
    private List<BookshelfResponseDTO.CompletedBookDto> buildCompletedBooks(Long userId) {
        List<MemberBook> memberBooks = memberBookRepository.findCompletedBooksByUserId(userId);

        Map<Long, LocalDateTime> completionDateByGroupId = matchedMemberRepository
                .findCompletedByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        mm -> mm.getGroup().getId(),
                        MatchedMember::getCompletedAt,
                        (a, b) -> a
                ));

        List<Long> memberBookIds = memberBooks.stream().map(MemberBook::getId).toList();
        Map<Long, BookReview> reviewByMemberBookId = memberBookIds.isEmpty()
                ? Map.of()
                : bookReviewRepository.findByMemberBook_IdIn(memberBookIds).stream()
                        .collect(Collectors.toMap(br -> br.getMemberBook().getId(), br -> br));

        return memberBooks.stream()
                .map(mb -> {
                    Book book = mb.getGroup().getBook();
                    BookReview review = reviewByMemberBookId.get(mb.getId());
                    LocalDateTime completedAt = completionDateByGroupId.get(mb.getGroup().getId());
                    return new BookshelfResponseDTO.CompletedBookDto(
                            mb.getId(),
                            book.getTitle(),
                            book.getAuthor(),
                            book.getImage(),
                            book.getCategory() != null ? book.getCategory().name() : null,
                            review != null ? review.getStar() : null,
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
        List<UserBook> representativeBooks = userBookRepository.findRepresentativeBooks(userId);
        if (representativeBooks.isEmpty()) {
            return List.of();
        }

        List<Long> bookIds = representativeBooks.stream()
                .map(ub -> ub.getBook().getId())
                .distinct()
                .toList();
        Map<Long, Double> ratingByBookId = bookReviewRepository
                .findLatestByUserIdAndBookIds(userId, bookIds)
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getMemberBook().getBook().getId(),
                        BookReview::getStar,
                        (latest, ignored) -> latest
                ));

        return representativeBooks.stream()
                .map(ub -> new BookshelfResponseDTO.RepresentativeBookDto(
                        ub.getId(),
                        ub.getBook().getTitle(),
                        ub.getDisplayOrder(),
                        ub.isFavorite(),
                        ratingByBookId.get(ub.getBook().getId())
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
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        if (userBookRepository.countByUser_IdAndIsFavoriteTrue(userId) >= MAX_FAVORITE_BOOKS) {
            throw new UserException(UserErrorCode.FAVORITE_BOOK_LIMIT_EXCEEDED);
        }

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

    // 대표책 등록
    @Transactional
    public void addRepresentativeBook(Long userId, Long userBookId, Long memberBookId) {
        if ((userBookId == null) == (memberBookId == null)) {
            throw new UserException(UserErrorCode.USER_BOOK_NOT_FOUND);
        }

        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        if (userBookRepository.countByUser_IdAndDisplayOrderIsNotNull(userId) >= MAX_REPRESENTATIVE_BOOKS) {
            throw new UserException(UserErrorCode.USER_PICK_LIMIT_EXCEEDED);
        }

        int nextOrder = userBookRepository.findMaxDisplayOrderByUserId(userId) + 1;

        if (userBookId != null) {
            addRepresentativeFromFavorite(userId, userBookId, nextOrder);
        } else {
            addRepresentativeFromCompleted(userId, memberBookId, nextOrder);
        }
    }

    // 인생책을 대표책으로 등록
    private void addRepresentativeFromFavorite(Long userId, Long userBookId, int nextOrder) {
        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));
        if (!userBook.isFavorite()) {
            throw new UserException(UserErrorCode.NOT_ELIGIBLE_FOR_REPRESENTATIVE);
        }
        userBook.updateDisplayOrder(nextOrder);
    }

    // 완독책을 대표책으로 등록
    private void addRepresentativeFromCompleted(Long userId, Long memberBookId, int nextOrder) {
        MemberBook memberBook = memberBookRepository.findByIdAndMatchedMember_User_IdWithBook(memberBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));
        if (!bookReviewRepository.existsByMemberBookId(memberBookId)) {
            throw new UserException(UserErrorCode.NOT_ELIGIBLE_FOR_REPRESENTATIVE);
        }

        Book book = memberBook.getBook();
        Optional<UserBook> existing = userBookRepository.findByUser_IdAndBook_Id(userId, book.getId());
        if (existing.isPresent()) {
            existing.get().updateDisplayOrder(nextOrder);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
            userBookRepository.save(UserBook.createRepresentative(user, book, nextOrder));
        }
    }

    // 인생 책 삭제
    @Transactional
    public void deleteFavoriteBook(Long userId, Long userBookId) {
        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));

        if (!userBook.isFavorite()) {
            throw new UserException(UserErrorCode.USER_BOOK_NOT_FOUND);
        }

        if (userBookRepository.countByUser_IdAndIsFavoriteTrue(userId) <= 1) {
            throw new UserException(UserErrorCode.FAVORITE_BOOK_MIN_REQUIRED);
        }

        boolean shouldKeep = userBook.getDisplayOrder() != null
                || bookReviewRepository.existsReviewedBookByUserIdAndBookId(userId, userBook.getBook().getId());

        if (shouldKeep) {
            userBook.updateIsFavorite(false);
        } else {
            userBookRepository.delete(userBook);
        }
    }

    // 대표책 삭제
    @Transactional
    public void deleteRepresentativeBook(Long userId, Long userBookId) {
        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));

        if (userBook.getDisplayOrder() == null) {
            throw new UserException(UserErrorCode.USER_BOOK_NOT_FOUND);
        }

        if (userBook.isFavorite()
                && userBookRepository.countByUser_IdAndIsFavoriteTrueAndDisplayOrderIsNotNull(userId) <= 1) {
            throw new UserException(UserErrorCode.REPRESENTATIVE_MUST_CONTAIN_FAVORITE);
        }

        if (userBook.isFavorite()) {
            userBook.updateDisplayOrder(null);
        } else {
            userBookRepository.delete(userBook);
        }
    }

    // 대표책 순서 변경 (드래그앤드롭)
    @Transactional
    public void reorderRepresentativeBooks(Long userId, Long userBookId, Integer targetOrder) {
        UserBook dragged = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));

        if (dragged.getDisplayOrder() == null) {
            throw new UserException(UserErrorCode.USER_BOOK_NOT_FOUND);
        }

        int sourceOrder = dragged.getDisplayOrder();
        if (sourceOrder == targetOrder) return;

        long totalCount = userBookRepository.countByUser_IdAndDisplayOrderIsNotNull(userId);
        if (targetOrder < 1 || targetOrder > totalCount) {
            throw new UserException(UserErrorCode.INVALID_REPRESENTATIVE_ORDER);
        }

        int low = Math.min(sourceOrder, targetOrder);
        int high = Math.max(sourceOrder, targetOrder);
        int shift = sourceOrder > targetOrder ? 1 : -1;

        List<UserBook> affected = userBookRepository.findRepresentativeBooks(userId).stream()
                .filter(ub -> ub.getDisplayOrder() >= low && ub.getDisplayOrder() <= high)
                .toList();
        List<Long> affectedIds = affected.stream().map(UserBook::getId).toList();
        Map<Long, Integer> snapshotOrders = affected.stream()
                .collect(Collectors.toMap(UserBook::getId, UserBook::getDisplayOrder));

        userBookRepository.clearDisplayOrderInRange(userId, low, high);

        userBookRepository.findAllById(affectedIds).forEach(ub -> {
            if (ub.getId().equals(userBookId)) {
                ub.updateDisplayOrder(targetOrder);
            } else {
                ub.updateDisplayOrder(snapshotOrders.get(ub.getId()) + shift);
            }
        });
    }
}
