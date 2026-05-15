package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember matchedMember;

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
