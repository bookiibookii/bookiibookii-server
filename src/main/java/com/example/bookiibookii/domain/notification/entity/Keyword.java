package com.example.bookiibookii.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "keyword",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_keyword_content", columnNames = {"content"})
        }
)
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long id;

    @Column(name = "content", nullable = false, length=50)
    private String content;
}
