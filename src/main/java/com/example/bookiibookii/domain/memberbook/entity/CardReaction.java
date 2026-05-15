package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "card_reaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_reaction_matchedmember_card_reaction",
                columnNames = {"matchedmember_id", "card_id", "reaction"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardReaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_reaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Cards card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember matchedMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", length = 30, nullable = false)
    private CardReactionType reaction;

    public static CardReaction create(Cards card, MatchedMember matchedMember, CardReactionType reaction) {
        Objects.requireNonNull(card, "card must not be null");
        Objects.requireNonNull(matchedMember, "matchedMember must not be null");
        Objects.requireNonNull(reaction, "reaction must not be null");
        return CardReaction.builder()
                .card(card)
                .matchedMember(matchedMember)
                .reaction(reaction)
                .build();
    }

}
