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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        long currentCount = userBookRepository.countByUser_IdAndDisplayOrderIsNotNull(userId);
        if (currentCount >= MAX_REPRESENTATIVE_BOOKS) {
            throw new UserException(UserErrorCode.USER_PICK_LIMIT_EXCEEDED);
        }

        Set<Integer> usedOrders = userBookRepository.findRepresentativeBooks(userId).stream()
                .map(UserBook::getDisplayOrder)
                .collect(Collectors.toSet());
        int nextOrder = IntStream.rangeClosed(1, MAX_REPRESENTATIVE_BOOKS)
                .filter(i -> !usedOrders.contains(i))
                .findFirst()
                .orElseThrow(() -> new UserException(UserErrorCode.USER_PICK_LIMIT_EXCEEDED));

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

    // 인생 책 교체 (구 책 제거 + 신 책 등록 + 대표책 연동 원자적 처리)
    @Transactional
    public void replaceFavoriteBook(Long userId, Long userBookId, String newIsbn13) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        UserBook oldBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));

        if (!oldBook.isFavorite()) {
            throw new UserException(UserErrorCode.USER_BOOK_NOT_FOUND);
        }

        Book newBook = bookService.getOrCreateByIsbn13(newIsbn13);

        if (oldBook.getBook().getId().equals(newBook.getId())) {
            return;
        }

        Optional<UserBook> existingNew = userBookRepository.findByUser_IdAndBook_Id(userId, newBook.getId());
        if (existingNew.isPresent() && existingNew.get().isFavorite()) {
            throw new UserException(UserErrorCode.FAVORITE_BOOK_ALREADY_EXISTS);
        }
        Long existingNewId = existingNew.map(UserBook::getId).orElse(null);

        boolean wasRepresentative = oldBook.getDisplayOrder() != null;
        int oldDisplayOrder = wasRepresentative ? oldBook.getDisplayOrder() : -1;

        // ── 대표책 순서 재배치 준비 ──────────────────────────────────────────
        // user_book 테이블의 (user_id, display_order)에는 UNIQUE 제약이 걸려 있음
        // 예) 구 책이 슬롯 3에 있고 슬롯 [4, 5]가 뒤에 있다면, 구 책을 제거한 뒤 4→3, 5→4로 당겨야 함
        //
        // [스냅샷 찍기]
        // Hibernate는 엔티티의 변경(dirty tracking)을 모아두었다가 flush 시점에 한꺼번에 DB에 반영.
        // 이때 UPDATE 실행 순서는 보장되지 않음.
        // 만약 순서가 어긋나면:
        //   - 슬롯 3에 구 책이 아직 남아 있는 상태에서 슬롯 4짜리 책을 display_order=3 으로 UPDATE → UNIQUE 위반 발생
        //
        // [해결 방법: null 경유 2-step 업데이트]
        //   1) 아직 아무 변경도 가하지 않은 시점에 이동 대상 목록과 각각의 현재 순서(snapshot)를 메모리에 보관
        //   2) 구 책의 display_order를 null로 마킹하고, 인생책 제거를 수행
        //   3) clearDisplayOrderInRange()로 이동 대상 범위를 일괄 null 로 초기화
        //      (@Modifying이 flush를 먼저 강제하므로 위 2)의 변경이 DB에 반영됨)
        //   4) 재조회 후 snapshotOrder-1 을 각각 배정
        //      null → 새 값 으로 가는 UPDATE는 같은 숫자끼리 충돌할 일 없음
        // ──────────────────────────────────────────────────────────────────
        List<UserBook> toShift = List.of();
        if (wasRepresentative) {
            // 변경 전 시점에 이동 대상(구 책보다 뒤 슬롯)의 목록과 현재 순서를 확보
            toShift = userBookRepository.findRepresentativeBooks(userId).stream()
                    .filter(ub -> !ub.getId().equals(userBookId) && ub.getDisplayOrder() > oldDisplayOrder)
                    .toList();
            oldBook.updateDisplayOrder(null); // 구 책 슬롯 해제 (dirty tracking)
        }

        // 구 책 인생책 제거
        boolean hasReview = bookReviewRepository.existsReviewedBookByUserIdAndBookId(userId, oldBook.getBook().getId());
        if (hasReview) {
            oldBook.updateIsFavorite(false);
        } else {
            userBookRepository.delete(oldBook);
        }

        // 이동 대상 범위를 null로 일괄 초기화한 뒤 snapshotOrder - 1 로 재배정
        if (wasRepresentative && !toShift.isEmpty()) {
            List<Long> toShiftIds = toShift.stream().map(UserBook::getId).toList();
            // null 초기화 후에는 원래 순서를 DB에서 읽을 수 없으므로 미리 보관
            Map<Long, Integer> snapshotOrders = toShift.stream()
                    .collect(Collectors.toMap(UserBook::getId, UserBook::getDisplayOrder));
            int maxShiftOrder = snapshotOrders.values().stream().mapToInt(Integer::intValue).max().orElseThrow();

            // 범위 내 display_order를 null로 초기화 + L1 캐시 초기화(clearAutomatically)
            userBookRepository.clearDisplayOrderInRange(userId, oldDisplayOrder + 1, maxShiftOrder);
            // 캐시 초기화 후 DB에서 재조회(display_order = null 상태), 이후 새 순서 배정
            userBookRepository.findAllById(toShiftIds)
                    .forEach(ub -> ub.updateDisplayOrder(snapshotOrders.get(ub.getId()) - 1));
        }

        // 신 책 인생책 등록
        // clearDisplayOrderInRange 호출 후 L1 캐시가 초기화되므로 ID로 재조회
        UserBook newUserBook;
        if (existingNewId != null) {
            newUserBook = userBookRepository.findById(existingNewId)
                    .orElseThrow(() -> new UserException(UserErrorCode.USER_BOOK_NOT_FOUND));
            newUserBook.updateIsFavorite(true);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
            newUserBook = userBookRepository.save(UserBook.create(user, newBook, true));
        }

        // 구 책이 대표책이었다면 신 책도 대표책으로 등록 (아직 대표책이 아닌 경우에만)
        if (wasRepresentative && newUserBook.getDisplayOrder() == null) {
            long currentCount = userBookRepository.countByUser_IdAndDisplayOrderIsNotNull(userId);
            if (currentCount < MAX_REPRESENTATIVE_BOOKS) {
                Set<Integer> usedOrders = userBookRepository.findRepresentativeBooks(userId).stream()
                        .map(UserBook::getDisplayOrder)
                        .collect(Collectors.toSet());
                int nextOrder = IntStream.rangeClosed(1, MAX_REPRESENTATIVE_BOOKS)
                        .filter(i -> !usedOrders.contains(i))
                        .findFirst()
                        .orElseThrow(() -> new UserException(UserErrorCode.USER_PICK_LIMIT_EXCEEDED));
                newUserBook.updateDisplayOrder(nextOrder);
            }
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

        int deletedOrder = userBook.getDisplayOrder();

        // 이동 대상 책들의 스냅샷을 미리 확보
        List<UserBook> toShift = userBookRepository.findRepresentativeBooks(userId).stream()
                .filter(ub -> !ub.getId().equals(userBookId) && ub.getDisplayOrder() > deletedOrder)
                .toList();

        if (userBook.isFavorite()) {
            userBook.updateDisplayOrder(null);
        } else {
            userBookRepository.delete(userBook);
        }

        if (toShift.isEmpty()) return;

        List<Long> toShiftIds = toShift.stream().map(UserBook::getId).toList();
        Map<Long, Integer> snapshotOrders = toShift.stream()
                .collect(Collectors.toMap(UserBook::getId, UserBook::getDisplayOrder));
        int maxShiftOrder = snapshotOrders.values().stream().mapToInt(Integer::intValue).max().orElseThrow();

        // unique 제약 충돌 방지: 이동 범위를 먼저 null로 초기화 후 재배치
        userBookRepository.clearDisplayOrderInRange(userId, deletedOrder + 1, maxShiftOrder);

        userBookRepository.findAllById(toShiftIds)
                .forEach(ub -> ub.updateDisplayOrder(snapshotOrders.get(ub.getId()) - 1));
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
