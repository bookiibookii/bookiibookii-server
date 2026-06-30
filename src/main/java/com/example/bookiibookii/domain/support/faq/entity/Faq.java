package com.example.bookiibookii.domain.support.faq.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faq")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Faq extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", length = 255, nullable = false)
    private String question;

    @Column(name = "answer", length = 2000, nullable = false)
    private String answer;

    public void updateQuestion(String question) { this.question = question; }
    public void updateAnswer(String answer) { this.answer = answer; }
}
