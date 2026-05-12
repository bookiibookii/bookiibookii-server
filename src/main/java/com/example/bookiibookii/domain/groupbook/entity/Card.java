package com.example.bookiibookii.domain.groupbook.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_book_id", nullable = false)
    private GroupBook groupBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @Column(name = "page")
    private Integer page;

    @Column(name = "memo", length = 500)
    private String memo;

    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private CardImage cardImage;

    public void updatePage(Integer page) {
        this.page = page;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
