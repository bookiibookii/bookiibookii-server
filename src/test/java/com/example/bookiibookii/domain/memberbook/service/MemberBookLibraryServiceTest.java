package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberBookLibraryServiceTest {

    @Mock
    private MemberBookRepository memberBookRepository;
    @Mock
    private BookReviewRepository bookReviewRepository;
    @Mock
    private UserImageS3Service userImageS3Service;

    @InjectMocks
    private MemberBookLibraryService memberBookLibraryService;

    @Test
    void completedExchangeBookUsesPreservedCurrentPageForLibraryProgress() {
        User user = User.builder().id(1L).nickName("reader").build();
        Groups group = Groups.builder()
                .id(10L)
                .groupName("완료된 교환독서")
                .groupType(GroupType.RELAY)
                .groupStatus(GroupStatus.COMPLETED)
                .readingPeriod(7)
                .host(user)
                .build();
        MatchedMember member = MatchedMember.builder()
                .id(20L)
                .group(group)
                .user(user)
                .build();
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 12, 21, 30);
        Instant completedInstant = completedAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();
        member.completeReading(completedInstant);
        MemberBook memberBook = MemberBook.builder()
                .id(30L)
                .group(group)
                .matchedMember(member)
                .book(Book.builder()
                        .id(40L)
                        .title("완독 책")
                        .totalPages(200)
                        .category(CustomCategory.KOREAN_NOVEL)
                        .build())
                .isMine(true)
                .currentPage(200)
                .build();

        when(memberBookRepository.findAllByMatchedMember_User_IdWithGroupAndBookAndHost(user.getId()))
                .thenReturn(List.of(memberBook));
        when(bookReviewRepository.findByMemberBook_IdIn(List.of(memberBook.getId())))
                .thenReturn(List.of());

        var response = memberBookLibraryService.getLibraryMemberBooks(user.getId());

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.getProgressRate()).isEqualTo(100);
            assertThat(item.getTotalPages()).isEqualTo(200);
            assertThat(item.getGenre()).isEqualTo("한국소설");
            assertThat(item.getCompletedAt()).isEqualTo(completedInstant);
        });
    }
}
