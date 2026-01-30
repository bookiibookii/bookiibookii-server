package com.example.bookiibookii.domain.support.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.support.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.entity.Report;
import com.example.bookiibookii.domain.support.repository.ReportRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final ReportRepository reportRepository;
    private final GroupsRepository groupRepository; // 그룹 조회용
    private final UserRepository userRepository;   // 대상 유저 확인용

    public void createReport(User user, ReportRequestDTO.CreateReportDTO request) {
        Groups group = groupRepository.findById(request.groupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (!userRepository.existsById(request.targetId())) {
            throw new UserException(UserErrorCode.NOT_FOUND);
        }

        Report newReport = Report.builder()
                .user(user)
                .group(group)
                .targetId(request.targetId())
                .reportType(request.reportType())
                .content(request.content())
                .build();

        reportRepository.save(newReport);
    }
}
