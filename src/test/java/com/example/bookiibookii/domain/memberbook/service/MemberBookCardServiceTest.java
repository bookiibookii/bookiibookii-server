package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardUpdateRequestDTO;
import com.example.bookiibookii.domain.memberbook.entity.CardImages;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.entity.CardReaction;
import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.memberbook.repository.CardImagesRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardReactionRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardsRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberCardRepository;
import com.example.bookiibookii.domain.notification.event.ReadingCardReactionNotificationEvent;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberBookCardServiceTest {

    @Mock
    private MemberBookRepository memberBookRepository;
    @Mock
    private CardsRepository cardsRepository;
    @Mock
    private CardImagesRepository cardImagesRepository;
    @Mock
    private MemberCardRepository memberCardRepository;
    @Mock
    private CardReactionRepository cardReactionRepository;
    @Mock
    private GroupsRepository groupsRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private CardImageS3Service cardImageS3Service;
    @Mock
    private CardImageValidationService cardImageValidationService;
    @Mock
    private UserImageS3Service userImageS3Service;
    @Mock
    private ReadingCardShareService readingCardShareService;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private MemberBookCardService memberBookCardService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-06-20T05:00:00Z"));
    }

    @Test
    void cardDetailIncludesBookMetadataAndMutualReviewCompletionTime() {
        MemberBook memberBook = memberBook(200);
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 12, 21, 30);
        Instant completedInstant = completedAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();
        memberBook.getMatchedMember().completeReading(completedInstant);
        Cards card = textCard(52L, memberBook, 200);

        when(cardsRepository.findByIdWithDetails(card.getId())).thenReturn(Optional.of(card));
        when(memberCardRepository.findByUserIdAndCardId(1L, card.getId())).thenReturn(Optional.empty());
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(memberBook.getGroup().getId(), 1L))
                .thenReturn(Optional.of(memberBook.getMatchedMember()));

        var response = memberBookCardService.getCardDetail(card.getId(), 1L, 60);

        assertThat(response.getTotalPages()).isEqualTo(200);
        assertThat(response.getGenre()).isEqualTo("한국소설");
        assertThat(response.getCompletedAt()).isEqualTo(completedInstant);
    }

    @Test
    void memberBookCardListUsesReferenceMemberBooksGroupAndBookAndKeepsHiddenFiltering() {
        MemberBook referenceMemberBook = memberBook(0);
        Cards visibleCard = textCard(10L, referenceMemberBook, 10);
        Cards hiddenCard = textCard(20L, referenceMemberBook, 20);

        when(memberBookRepository.findById(referenceMemberBook.getId()))
                .thenReturn(Optional.of(referenceMemberBook));
        when(matchedMemberRepository.existsByGroup_IdAndUser_Id(
                referenceMemberBook.getGroup().getId(), 1L
        )).thenReturn(true);
        when(cardsRepository.findByGroupIdAndBookIdWithMemberBookAndBookAndCreator(
                referenceMemberBook.getGroup().getId(), referenceMemberBook.getBook().getId()
        )).thenReturn(List.of(visibleCard, hiddenCard));
        when(memberCardRepository.findHiddenCardIdsByUserIdAndGroupId(
                1L, referenceMemberBook.getGroup().getId()
        )).thenReturn(List.of(hiddenCard.getId()));
        when(memberCardRepository.findBookmarkedCardIdsByUserIdAndCardIdIn(
                1L, List.of(visibleCard.getId())
        )).thenReturn(Set.of());
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(
                referenceMemberBook.getGroup().getId(), 1L
        )).thenReturn(Optional.of(referenceMemberBook.getMatchedMember()));

        var response = memberBookCardService.getCardsByMemberBookId(
                referenceMemberBook.getId(), 1L, 60);

        assertThat(response.getGroupId()).isEqualTo(referenceMemberBook.getGroup().getId());
        assertThat(response.getCards()).extracting(card -> card.getCardId())
                .containsExactly(visibleCard.getId());
        verify(cardsRepository).findByGroupIdAndBookIdWithMemberBookAndBookAndCreator(
                referenceMemberBook.getGroup().getId(), referenceMemberBook.getBook().getId());
    }

    @Test
    void eitherReferenceMemberBookForSameGroupAndBookUsesSameRepositoryScope() {
        MemberBook firstReference = memberBook(0);
        MatchedMember otherReader = MatchedMember.builder()
                .id(2L)
                .group(firstReference.getGroup())
                .user(User.builder().id(2L).nickName("other").build())
                .build();
        MemberBook secondReference = MemberBook.builder()
                .id(2L)
                .group(firstReference.getGroup())
                .book(firstReference.getBook())
                .matchedMember(otherReader)
                .isMine(false)
                .build();

        when(memberBookRepository.findById(firstReference.getId())).thenReturn(Optional.of(firstReference));
        when(memberBookRepository.findById(secondReference.getId())).thenReturn(Optional.of(secondReference));
        when(matchedMemberRepository.existsByGroup_IdAndUser_Id(firstReference.getGroup().getId(), 1L))
                .thenReturn(true);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(firstReference.getGroup().getId(), 1L))
                .thenReturn(Optional.of(firstReference.getMatchedMember()));

        memberBookCardService.getCardsByMemberBookId(firstReference.getId(), 1L, 60);
        memberBookCardService.getCardsByMemberBookId(secondReference.getId(), 1L, 60);

        verify(cardsRepository, org.mockito.Mockito.times(2))
                .findByGroupIdAndBookIdWithMemberBookAndBookAndCreator(
                        firstReference.getGroup().getId(), firstReference.getBook().getId());
    }

    @Test
    void memberBookCardListRejectsUserOutsideReferenceMemberBooksGroup() {
        MemberBook referenceMemberBook = memberBook(0);
        when(memberBookRepository.findById(referenceMemberBook.getId()))
                .thenReturn(Optional.of(referenceMemberBook));
        when(matchedMemberRepository.existsByGroup_IdAndUser_Id(
                referenceMemberBook.getGroup().getId(), 999L
        )).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        memberBookCardService.getCardsByMemberBookId(referenceMemberBook.getId(), 999L, 60))
                .isInstanceOf(GroupException.class);

        verify(cardsRepository, never()).findByGroupIdAndBookIdWithMemberBookAndBookAndCreator(
                anyLong(), anyLong());
    }

    @Test
    void addingReactionPublishesNotificationForCardOwner() {
        Groups group = Groups.builder().id(10L).build();
        MatchedMember owner = MatchedMember.builder()
                .id(1L)
                .group(group)
                .user(User.builder().id(100L).nickName("작성자").build())
                .build();
        MatchedMember reactor = MatchedMember.builder()
                .id(2L)
                .group(group)
                .user(User.builder().id(200L).nickName("반응자").build())
                .build();
        MemberBook ownerBook = MemberBook.builder()
                .id(1L)
                .group(group)
                .matchedMember(owner)
                .book(Book.builder().id(1L).title("책").build())
                .build();
        Cards card = textCard(50L, ownerBook, 10);

        when(cardsRepository.findByIdWithDetails(card.getId())).thenReturn(Optional.of(card));
        when(matchedMemberRepository.existsByGroup_IdAndUser_Id(group.getId(), reactor.getUser().getId()))
                .thenReturn(true);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), reactor.getUser().getId()))
                .thenReturn(Optional.of(reactor));
        when(memberCardRepository.findByMatchedMember_IdAndCard_Id(reactor.getId(), card.getId()))
                .thenReturn(Optional.empty());
        when(cardReactionRepository.findByMatchedMember_IdAndCard_IdAndReaction(
                reactor.getId(), card.getId(), CardReactionType.LIKE
        )).thenReturn(Optional.empty());
        when(cardReactionRepository.saveAndFlush(any(CardReaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        memberBookCardService.toggleReaction(card.getId(), reactor.getUser().getId(), CardReactionType.LIKE);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        ReadingCardReactionNotificationEvent event =
                (ReadingCardReactionNotificationEvent) captor.getValue();
        assertThat(event.cardId()).isEqualTo(card.getId());
        assertThat(event.memberBookId()).isEqualTo(ownerBook.getId());
        assertThat(event.reactorId()).isEqualTo(reactor.getUser().getId());
        assertThat(event.receiverId()).isEqualTo(owner.getUser().getId());
    }

    @Test
    void addingReactionToOwnCardDoesNotPublishNotification() {
        MemberBook ownerBook = memberBook(30);
        MatchedMember owner = ownerBook.getMatchedMember();
        Cards card = textCard(51L, ownerBook, 10);

        when(cardsRepository.findByIdWithDetails(card.getId())).thenReturn(Optional.of(card));
        when(matchedMemberRepository.existsByGroup_IdAndUser_Id(
                ownerBook.getGroup().getId(), owner.getUser().getId()
        )).thenReturn(true);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(
                ownerBook.getGroup().getId(), owner.getUser().getId()
        )).thenReturn(Optional.of(owner));
        when(memberCardRepository.findByMatchedMember_IdAndCard_Id(owner.getId(), card.getId()))
                .thenReturn(Optional.empty());
        when(cardReactionRepository.findByMatchedMember_IdAndCard_IdAndReaction(
                owner.getId(), card.getId(), CardReactionType.LIKE
        )).thenReturn(Optional.empty());
        when(cardReactionRepository.saveAndFlush(any(CardReaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        memberBookCardService.toggleReaction(card.getId(), owner.getUser().getId(), CardReactionType.LIKE);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void removingExistingReactionDoesNotPublishNotification() {
        ReactionFixture fixture = reactionFixture();
        CardReaction existing = CardReaction.create(
                fixture.card(),
                fixture.reactor(),
                CardReactionType.LIKE
        );
        stubReactionAccess(fixture);
        when(cardReactionRepository.findByMatchedMember_IdAndCard_IdAndReaction(
                fixture.reactor().getId(), fixture.card().getId(), CardReactionType.LIKE
        )).thenReturn(Optional.of(existing));

        memberBookCardService.toggleReaction(
                fixture.card().getId(),
                fixture.reactor().getUser().getId(),
                CardReactionType.LIKE
        );

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void addingAnotherReactionPublishesSameCardAndReactorIdentityForDeduplication() {
        ReactionFixture fixture = reactionFixture();
        stubReactionAccess(fixture);
        when(cardReactionRepository.findByMatchedMember_IdAndCard_IdAndReaction(
                fixture.reactor().getId(), fixture.card().getId(), CardReactionType.SAD
        )).thenReturn(Optional.empty());
        when(cardReactionRepository.saveAndFlush(any(CardReaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        memberBookCardService.toggleReaction(
                fixture.card().getId(),
                fixture.reactor().getUser().getId(),
                CardReactionType.SAD
        );

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        ReadingCardReactionNotificationEvent event =
                (ReadingCardReactionNotificationEvent) captor.getValue();
        assertThat(event.cardId()).isEqualTo(fixture.card().getId());
        assertThat(event.reactorId()).isEqualTo(fixture.reactor().getUser().getId());
    }

    @Test
    void createTextCardDoesNotChangeCurrentPage() {
        MemberBook memberBook = memberBook(30);
        MemberCardCreateRequestDTO request = createRequest(CardType.TEXT, 100);
        ReflectionTestUtils.setField(request, "quotation", "기억할 문장");
        stubCardCreation(memberBook);

        memberBookCardService.createCard(memberBook.getId(), 1L, request, 10);

        assertThat(memberBook.getCurrentPage()).isEqualTo(30);
    }

    @Test
    void createImageCardDoesNotChangeCurrentPage() {
        MemberBook memberBook = memberBook(30);
        MemberCardCreateRequestDTO request = createRequest(CardType.IMAGE, 100);
        ReflectionTestUtils.setField(request, "s3Key", "cards/image.jpg");
        stubCardCreation(memberBook);
        when(cardImageValidationService.isValidS3Key("cards/image.jpg")).thenReturn(true);
        when(cardImageS3Service.doesImageExist("cards/image.jpg")).thenReturn(true);
        when(cardImagesRepository.saveAndFlush(any(CardImages.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        memberBookCardService.createCard(memberBook.getId(), 1L, request, 10);

        assertThat(memberBook.getCurrentPage()).isEqualTo(30);
    }

    @Test
    void updateCardPageDoesNotChangeCurrentPage() {
        MemberBook memberBook = memberBook(80);
        Cards card = textCard(1L, memberBook, 50);
        when(cardsRepository.findByIdAndOwnerUserId(card.getId(), 1L)).thenReturn(Optional.of(card));

        MemberCardUpdateRequestDTO request = new MemberCardUpdateRequestDTO();
        ReflectionTestUtils.setField(request, "page", 120);

        memberBookCardService.updateCard(card.getId(), 1L, request, 10);

        assertThat(card.getPage()).isEqualTo(120);
        assertThat(memberBook.getCurrentPage()).isEqualTo(80);
    }

    @Test
    void updateCardContentDoesNotChangeCurrentPage() {
        MemberBook memberBook = memberBook(80);
        Cards card = textCard(1L, memberBook, 50);
        when(cardsRepository.findByIdAndOwnerUserId(card.getId(), 1L)).thenReturn(Optional.of(card));

        MemberCardUpdateRequestDTO request = new MemberCardUpdateRequestDTO();
        ReflectionTestUtils.setField(request, "memo", "수정한 메모");
        ReflectionTestUtils.setField(request, "quotation", "수정한 문장");

        memberBookCardService.updateCard(card.getId(), 1L, request, 10);

        assertThat(card.getMemo()).isEqualTo("수정한 메모");
        assertThat(card.getQuotation()).isEqualTo("수정한 문장");
        assertThat(memberBook.getCurrentPage()).isEqualTo(80);
    }

    @Test
    void updateCardImageDoesNotChangeCurrentPage() {
        MemberBook memberBook = memberBook(80);
        Cards card = Cards.builder()
                .id(1L)
                .memberBook(memberBook)
                .cardType(CardType.IMAGE)
                .page(50)
                .build();
        when(cardsRepository.findByIdAndOwnerUserId(card.getId(), 1L)).thenReturn(Optional.of(card));
        when(cardImageValidationService.isValidS3Key("cards/updated-image.jpg")).thenReturn(true);
        when(cardImageS3Service.doesImageExist("cards/updated-image.jpg")).thenReturn(true);
        when(cardImagesRepository.findByCard_Id(card.getId())).thenReturn(Optional.empty());
        when(cardImagesRepository.saveAndFlush(any(CardImages.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MemberCardUpdateRequestDTO request = new MemberCardUpdateRequestDTO();
        ReflectionTestUtils.setField(request, "s3Key", "cards/updated-image.jpg");

        memberBookCardService.updateCard(card.getId(), 1L, request, 10);

        assertThat(memberBook.getCurrentPage()).isEqualTo(80);
    }

    @Test
    void deleteCardDoesNotChangeCurrentPage() {
        MemberBook memberBook = memberBook(80);
        Cards card = textCard(1L, memberBook, 120);
        when(cardsRepository.findByIdWithDetails(card.getId())).thenReturn(Optional.of(card));
        when(memberCardRepository.findByMatchedMember_IdAndCard_Id(
                memberBook.getMatchedMember().getId(), card.getId()
        )).thenReturn(Optional.empty());

        memberBookCardService.removeCardFromView(card.getId(), 1L);

        assertThat(card.getDeletedAt()).isNotNull();
        assertThat(memberBook.getCurrentPage()).isEqualTo(80);
    }

    private void stubCardCreation(MemberBook memberBook) {
        AtomicReference<Cards> savedCard = new AtomicReference<>();
        when(memberBookRepository.findByIdAndMatchedMember_User_IdWithBook(memberBook.getId(), 1L))
                .thenReturn(Optional.of(memberBook));
        when(cardsRepository.save(any(Cards.class))).thenAnswer(invocation -> {
            Cards card = invocation.getArgument(0);
            ReflectionTestUtils.setField(card, "id", 1L);
            savedCard.set(card);
            return card;
        });
        when(cardsRepository.findByIdWithDetails(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(savedCard.get()));
    }

    private MemberCardCreateRequestDTO createRequest(CardType cardType, int page) {
        MemberCardCreateRequestDTO request = new MemberCardCreateRequestDTO();
        ReflectionTestUtils.setField(request, "cardType", cardType);
        ReflectionTestUtils.setField(request, "page", page);
        return request;
    }

    private MemberBook memberBook(int currentPage) {
        User user = User.builder().id(1L).nickName("reader").build();
        Groups group = Groups.builder().id(1L).groupName("교환 독서").build();
        MatchedMember matchedMember = MatchedMember.builder()
                .id(1L)
                .group(group)
                .user(user)
                .build();
        MemberBook memberBook = MemberBook.builder()
                .id(1L)
                .group(group)
                .book(Book.builder()
                        .id(1L)
                        .title("책")
                        .totalPages(200)
                        .category(CustomCategory.KOREAN_NOVEL)
                        .build())
                .matchedMember(matchedMember)
                .isMine(true)
                .currentPage(currentPage)
                .build();
        matchedMember.getMemberBooks().add(memberBook);
        return memberBook;
    }

    private Cards textCard(Long id, MemberBook memberBook, int page) {
        return Cards.builder()
                .id(id)
                .memberBook(memberBook)
                .cardType(CardType.TEXT)
                .page(page)
                .memo("메모")
                .quotation("문장")
                .build();
    }

    private ReactionFixture reactionFixture() {
        Groups group = Groups.builder().id(20L).build();
        MatchedMember owner = MatchedMember.builder()
                .id(10L)
                .group(group)
                .user(User.builder().id(100L).nickName("작성자").build())
                .build();
        MatchedMember reactor = MatchedMember.builder()
                .id(20L)
                .group(group)
                .user(User.builder().id(200L).nickName("반응자").build())
                .build();
        MemberBook ownerBook = MemberBook.builder()
                .id(30L)
                .group(group)
                .matchedMember(owner)
                .book(Book.builder().id(40L).title("책").build())
                .build();
        return new ReactionFixture(textCard(50L, ownerBook, 10), reactor);
    }

    private void stubReactionAccess(ReactionFixture fixture) {
        Long groupId = fixture.card().getMemberBook().getGroup().getId();
        Long reactorUserId = fixture.reactor().getUser().getId();
        when(cardsRepository.findByIdWithDetails(fixture.card().getId()))
                .thenReturn(Optional.of(fixture.card()));
        when(matchedMemberRepository.existsByGroup_IdAndUser_Id(groupId, reactorUserId))
                .thenReturn(true);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, reactorUserId))
                .thenReturn(Optional.of(fixture.reactor()));
        when(memberCardRepository.findByMatchedMember_IdAndCard_Id(
                fixture.reactor().getId(), fixture.card().getId()
        )).thenReturn(Optional.empty());
    }

    private record ReactionFixture(Cards card, MatchedMember reactor) {
    }
}
