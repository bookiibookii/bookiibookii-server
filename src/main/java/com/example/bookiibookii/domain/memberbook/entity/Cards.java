package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cards extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long id;

    /** 이 카드가 속한 멤버의 책 (그룹당 멤버·책 조합) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_book_id", nullable = false)
    private MemberBook memberBook;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "page")
    private Integer page;

    @Column(name = "memo", length = 110)
    private String memo;

    @Column(name = "quotation", length = 140)
    private String quotation;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private CardImages cardImages;

    @Builder.Default
    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardReaction> cardReactions = new ArrayList<>();

    public void updatePage(Integer page) {
        this.page = page;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateQuotation(String quotation) {
        this.quotation = quotation;
    }

    public void markDeleted(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
