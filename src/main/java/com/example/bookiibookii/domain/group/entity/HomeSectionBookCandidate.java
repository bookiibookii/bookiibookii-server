package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.HomeCandidateSectionType;
import com.example.bookiibookii.domain.group.enums.HomeCandidateSourceType;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "home_section_book_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeSectionBookCandidate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 50)
    private HomeCandidateSectionType sectionType;

    @Column(name = "isbn13", nullable = false, length = 13)
    private String isbn13;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "aladin_item_id")
    private Long aladinItemId;

    @Column(name = "source_cid")
    private Long sourceCid;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private HomeCandidateSourceType sourceType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;
}
