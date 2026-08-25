package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.CardReaction;
import com.example.bookiibookii.domain.memberbook.entity.CardShareToken;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.entity.MemberCard;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import com.example.bookiibookii.domain.memberbook.enums.ShareLayout;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ImportAutoConfiguration(exclude = DataJpaRepositoriesAutoConfiguration.class)
@EnableJpaRepositories(basePackageClasses = CardsRepository.class)
@ActiveProfiles("test")
class CardsRepositoryTest {

    @Autowired
    private CardsRepository cardsRepository;

    @Autowired
    private CardShareTokenRepository cardShareTokenRepository;

    @Autowired
    private MemberCardRepository memberCardRepository;

    @Autowired
    private CardReactionRepository cardReactionRepository;

    @Autowired
    private MemberBookRepository memberBookRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void groupAndBookQueryReturnsBothReadersCardsAndExcludesOtherBooksGroupsAndDeletedCards() {
        User user1 = persistUser("cards-user-1", "유저1");
        User user2 = persistUser("cards-user-2", "유저2");
        Book book1 = persistBook("9780000010001", "책1");
        Book book2 = persistBook("9780000010002", "책2");

        Groups groupA = persistGroup(book1, user1, "Group A");
        MatchedMember groupAUser1 = persistMatchedMember(groupA, user1);
        MatchedMember groupAUser2 = persistMatchedMember(groupA, user2);

        MemberBook mb1 = persistMemberBook(groupA, groupAUser1, book1, true);
        MemberBook mb2 = persistMemberBook(groupA, groupAUser1, book2, false);
        MemberBook mb3 = persistMemberBook(groupA, groupAUser2, book2, true);
        MemberBook mb4 = persistMemberBook(groupA, groupAUser2, book1, false);

        Cards cardA = persistCard(mb1, "책1 카드 A");
        Cards cardB = persistCard(mb2, "책2 카드 B");
        Cards cardC = persistCard(mb3, "책2 카드 C");
        Cards cardD = persistCard(mb4, "책1 카드 D");
        Cards deletedBook1Card = persistCard(mb4, "삭제된 책1 카드");
        deletedBook1Card.markDeleted(Instant.parse("2026-08-17T00:00:00Z"));

        Groups groupB = persistGroup(book1, user1, "Group B");
        MatchedMember groupBUser1 = persistMatchedMember(groupB, user1);
        MemberBook otherGroupBook1 = persistMemberBook(groupB, groupBUser1, book1, true);
        Cards otherGroupCard = persistCard(otherGroupBook1, "다른 그룹 책1 카드");

        entityManager.flush();
        entityManager.clear();

        List<Cards> book1Cards = cardsRepository
                .findByGroupIdAndBookIdWithMemberBookAndBookAndCreator(groupA.getId(), book1.getId());
        List<Cards> book2Cards = cardsRepository
                .findByGroupIdAndBookIdWithMemberBookAndBookAndCreator(groupA.getId(), book2.getId());
        List<Cards> allGroupCards = cardsRepository
                .findByGroupIdWithMemberBookAndBookAndCreator(groupA.getId());

        assertThat(book1Cards).extracting(Cards::getId)
                .containsExactly(cardA.getId(), cardD.getId())
                .doesNotContain(cardB.getId(), cardC.getId(), deletedBook1Card.getId(), otherGroupCard.getId());
        assertThat(book2Cards).extracting(Cards::getId)
                .containsExactly(cardB.getId(), cardC.getId());
        assertThat(allGroupCards).extracting(Cards::getId)
                .containsExactly(cardA.getId(), cardB.getId(), cardC.getId(), cardD.getId());
    }

    @Test
    void deletingOwnersMemberBooksSucceedsWhenAnotherMemberCreatedTokenAndCardState() {
        User owner = persistUser("reset-owner", "탈퇴 후 재가입 사용자");
        User other = persistUser("reset-other", "다른 그룹 멤버");
        Book book = persistBook("9780000010099", "공유 카드 책");
        Groups group = persistGroup(book, owner, "재가입 초기화 그룹");
        MatchedMember ownerMember = persistMatchedMember(group, owner);
        MatchedMember otherMember = persistMatchedMember(group, other);
        MemberBook ownerBook = persistMemberBook(group, ownerMember, book, true);
        Cards ownerCard = persistCard(ownerBook, "공유된 카드");
        CardShareToken token = CardShareToken.create(ownerCard, other, ShareLayout.SPLIT);
        MemberCard otherState = MemberCard.builder()
                .card(ownerCard)
                .matchedMember(otherMember)
                .bookmarked(true)
                .build();
        entityManager.persist(token);
        entityManager.persist(otherState);
        entityManager.flush();
        entityManager.clear();

        cardShareTokenRepository.deleteAllByCardOwnerUserId(owner.getId());
        memberCardRepository.deleteAllByCardOwnerUserId(owner.getId());
        memberBookRepository.deleteAllByMatchedMember_User_Id(owner.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(cardShareTokenRepository.findById(token.getId())).isEmpty();
        assertThat(memberCardRepository.findById(otherState.getId())).isEmpty();
        assertThat(memberBookRepository.findById(ownerBook.getId())).isEmpty();
        assertThat(cardsRepository.findById(ownerCard.getId())).isEmpty();
    }

    @Test
    void deletingMemberCardStateByUserRemovesStateOnAnotherUsersCard() {
        User resettingUser = persistUser("reset-state-owner", "재가입 사용자");
        User cardOwner = persistUser("reset-state-card-owner", "카드 작성자");
        Book book = persistBook("9780000010100", "북마크 카드 책");
        Groups group = persistGroup(book, cardOwner, "북마크 초기화 그룹");
        MatchedMember resettingMember = persistMatchedMember(group, resettingUser);
        MatchedMember ownerMember = persistMatchedMember(group, cardOwner);
        MemberBook ownerBook = persistMemberBook(group, ownerMember, book, true);
        Cards ownerCard = persistCard(ownerBook, "다른 사용자의 카드");
        MemberCard resettingUsersState = MemberCard.builder()
                .card(ownerCard)
                .matchedMember(resettingMember)
                .bookmarked(true)
                .build();
        entityManager.persist(resettingUsersState);
        entityManager.flush();
        entityManager.clear();

        memberCardRepository.deleteAllByUserId(resettingUser.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(memberCardRepository.findById(resettingUsersState.getId())).isEmpty();
        assertThat(cardsRepository.findById(ownerCard.getId())).isPresent();
    }

    @Test
    void deletingReactionsByUserRemovesReactionOnAnotherUsersCard() {
        User resettingUser = persistUser("reset-reaction-owner", "재가입 사용자");
        User cardOwner = persistUser("reset-reaction-card-owner", "카드 작성자");
        Book book = persistBook("9780000010101", "리액션 카드 책");
        Groups group = persistGroup(book, cardOwner, "리액션 초기화 그룹");
        MatchedMember resettingMember = persistMatchedMember(group, resettingUser);
        MatchedMember ownerMember = persistMatchedMember(group, cardOwner);
        MemberBook ownerBook = persistMemberBook(group, ownerMember, book, true);
        Cards ownerCard = persistCard(ownerBook, "반응을 남긴 카드");
        CardReaction reaction = CardReaction.create(ownerCard, resettingMember, CardReactionType.LIKE);
        entityManager.persist(reaction);
        entityManager.flush();
        entityManager.clear();

        cardReactionRepository.deleteAllByUserId(resettingUser.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(cardReactionRepository.findById(reaction.getId())).isEmpty();
        assertThat(cardsRepository.findById(ownerCard.getId())).isPresent();
    }

    private User persistUser(String socialId, String nickname) {
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId(socialId)
                .nickName(nickname)
                .build();
        entityManager.persist(user);
        return user;
    }

    private Book persistBook(String isbn, String title) {
        Book book = Book.builder()
                .isbn13(isbn)
                .title(title)
                .author("저자")
                .publisher("출판사")
                .image("image")
                .totalPages(200)
                .link("link")
                .category(CustomCategory.KOREAN_NOVEL)
                .build();
        entityManager.persist(book);
        return book;
    }

    private Groups persistGroup(Book book, User host, String name) {
        Groups group = Groups.builder()
                .book(book)
                .host(host)
                .maxCapacity(2)
                .startDate(LocalDate.of(2026, 8, 1))
                .readingPeriod(14)
                .groupStatus(GroupStatus.MATCHED)
                .tradeType(TradeType.DIRECT)
                .groupName(name)
                .build();
        entityManager.persist(group);
        return group;
    }

    private MatchedMember persistMatchedMember(Groups group, User user) {
        MatchedMember matchedMember = MatchedMember.builder()
                .group(group)
                .user(user)
                .build();
        entityManager.persist(matchedMember);
        return matchedMember;
    }

    private MemberBook persistMemberBook(
            Groups group,
            MatchedMember matchedMember,
            Book book,
            boolean isMine
    ) {
        MemberBook memberBook = MemberBook.builder()
                .group(group)
                .matchedMember(matchedMember)
                .book(book)
                .isMine(isMine)
                .build();
        entityManager.persist(memberBook);
        return memberBook;
    }

    private Cards persistCard(MemberBook memberBook, String memo) {
        Cards card = Cards.builder()
                .memberBook(memberBook)
                .cardType(CardType.TEXT)
                .page(10)
                .memo(memo)
                .quotation("문장")
                .build();
        entityManager.persist(card);
        return card;
    }
}
