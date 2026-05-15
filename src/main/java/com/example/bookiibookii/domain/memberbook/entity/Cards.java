package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "page")
    private Integer page;

    @Column(name = "memo", length = 255)
    private String memo;

    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private CardImages cardImages;

    public void updatePage(Integer page) {
        this.page = page;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
