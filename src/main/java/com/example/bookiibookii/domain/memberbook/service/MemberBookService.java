package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void createLibraryOnMatch(Groups group, MatchedMember guestMember, Book guestBook) {
        MatchedMember hostMember = matchedMemberRepository
                .findFirstByGroup_GroupIdOrderByCreatedAtAsc(group.getGroupId())
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MATCHED_MEMBER_NOT_FOUND));

        Book hostBook = group.getBook();

        createIfAbsent(hostMember, hostBook);
        createIfAbsent(hostMember, guestBook);
        createIfAbsent(guestMember, guestBook);
        createIfAbsent(guestMember, hostBook);
    }

    /**
     * MatchedMember×book 조합이 없을 때만 MemberBook을 생성합니다.
     */
    public MemberBook createIfAbsent(MatchedMember matchedMember, Book book) {
        return memberBookRepository
                .findByMatchedMember_IdAndBook_Id(matchedMember.getId(), book.getId())
                .orElseGet(() -> saveMemberBook(matchedMember, book));
    }

    private MemberBook saveMemberBook(MatchedMember matchedMember, Book book) {
        try {
            return memberBookRepository.save(
                    MemberBook.builder()
                            .group(matchedMember.getGroup())
                            .book(book)
                            .matchedMember(matchedMember)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            return memberBookRepository
                    .findByMatchedMember_IdAndBook_Id(matchedMember.getId(), book.getId())
                    .orElseThrow(() -> e);
        }
    }
}
