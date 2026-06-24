package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.memberbook.repository.CardReactionRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MatchedMember 삭제 전 카드 부가 상태(북마크·숨김·리액션)를 정리합니다.
 */
@Service
@RequiredArgsConstructor
public class MatchedMemberCardStateCleanupService {

    private final MemberCardRepository memberCardRepository;
    private final CardReactionRepository cardReactionRepository;

    @Transactional
    public void deleteByMatchedMember(MatchedMember matchedMember) {
        Long matchedMemberId = matchedMember.getId();
        memberCardRepository.deleteByMatchedMember_Id(matchedMemberId);
        cardReactionRepository.deleteByMatchedMember_Id(matchedMemberId);
    }
}
