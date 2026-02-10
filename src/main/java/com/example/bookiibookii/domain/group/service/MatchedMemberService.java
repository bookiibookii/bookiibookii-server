package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.converter.GroupConverter;
import com.example.bookiibookii.domain.group.dto.res.MatchedMemberResponseDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.MatchedMemberException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.exception.code.MatchedMemberErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchedMemberService {
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupsRepository groupsRepository;
    private final GroupConverter groupConverter;

    @Transactional
    public MatchedMemberResponseDTO.CompleteReadingResultDTO finishTogetherReading(Long userId, Long groupId) {

        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (group.getGroupType() != GroupType.TOGETHER) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_TYPE);
        }

        MatchedMember matchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new MatchedMemberException(MatchedMemberErrorCode.MEMBER_NOT_FOUND));

        if (matchedMember.getCurrentReadingRate() != null && matchedMember.getCurrentReadingRate() == 100) {
            throw new MatchedMemberException(MatchedMemberErrorCode.ALREADY_COMPLETED);
        }

        matchedMember.completeReading();

        return groupConverter.toCompleteReadingResultDTO(matchedMember);
    }
}
