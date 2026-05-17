package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberBookService {

    private final MemberBookRepository memberBookRepository;
    private final MatchedMemberRepository matchedMemberRepository;

    /**
     * 게스트 매칭 수락 시 멤버별 2권씩, 총 4건의 MemberBook을 한 번에 생성합니다.
     * - 방장: 내 책(group.book) + 상대 책(application.book)
     * - 게스트: 내 책(application.book) + 상대 책(group.book)
     */
    public void createLibraryOnMatch(
            Groups group,
            MatchedMember guestMember,
            Book guestBook,
            LocalDateTime matchedAt
    ) {
        MatchedMember hostMember = matchedMemberRepository
                .findByGroup_GroupIdAndRole(group.getGroupId(), RoleStatus.HOST)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MATCHED_MEMBER_NOT_FOUND));

        Book hostBook = group.getBook();

        List<MemberBook> hostBooks = List.of(
                createIfAbsent(hostMember, hostBook, true),
                createIfAbsent(hostMember, guestBook, false)
        );

        List<MemberBook> guestBooks = List.of(
                createIfAbsent(guestMember, guestBook, true),
                createIfAbsent(guestMember, hostBook, false)
        );

        hostMember.startMatchedReading(findMyBook(hostBooks), matchedAt);
        guestMember.startMatchedReading(findMyBook(guestBooks), matchedAt);
    }

    /**
     * MatchedMember×book×isMine 조합이 없을 때만 MemberBook을 생성합니다.
     */
    public MemberBook createIfAbsent(MatchedMember matchedMember, Book book, boolean isMine) {
        return memberBookRepository
                .findByMatchedMember_IdAndBook_IdAndIsMine(
                        matchedMember.getId(),
                        book.getId(),
                        isMine
                )
                .orElseGet(() -> saveMemberBook(matchedMember, book, isMine));
    }

    private MemberBook saveMemberBook(MatchedMember matchedMember, Book book, boolean isMine) {
        try {
            return memberBookRepository.save(
                    MemberBook.builder()
                            .group(matchedMember.getGroup())
                            .book(book)
                            .matchedMember(matchedMember)
                            .isMine(isMine)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            return memberBookRepository
                    .findByMatchedMember_IdAndBook_IdAndIsMine(
                            matchedMember.getId(),
                            book.getId(),
                            isMine
                    )
                    .orElseThrow(() -> e);
        }
    }

    private MemberBook findMyBook(List<MemberBook> memberBooks) {
        return memberBooks.stream()
                .filter(MemberBook::isMyBook)
                .findFirst()
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_BOOK_NOT_FOUND));
    }
}
