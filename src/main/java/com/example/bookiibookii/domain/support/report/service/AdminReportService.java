package com.example.bookiibookii.domain.support.report.service;

import com.example.bookiibookii.domain.support.report.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.report.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.support.report.entity.Report;
import com.example.bookiibookii.domain.support.report.exception.ReportException;
import com.example.bookiibookii.domain.support.report.exception.code.ReportErrorCode;
import com.example.bookiibookii.domain.support.report.repository.ReportRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public Page<ReportResponseDTO.AdminReportListDTO> getAllReports(Pageable pageable) {
        Page<Report> reportPage = reportRepository.findAllOrderByCreatedAtDesc(pageable);

        return reportPage.map(report -> {
            // targetId를 통해 피신고자 닉네임 조회 (없을 경우 탈퇴한 사용자로 처리)
            String targetNickname = userRepository.findById(report.getTargetId())
                    .map(User::getNickName)
                    .orElse("알 수 없는 사용자");

            return new ReportResponseDTO.AdminReportListDTO(
                    report.getId(),
                    report.getUser().getNickName(),
                    targetNickname,
                    report.getGroup().getBook().getTitle(),
                    report.getReportType(),
                    report.getSupportStatus(),
                    report.getResolvedAt()
            );
        });
    }

    @Transactional(readOnly = true)
    public ReportResponseDTO.AdminReportDetailDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));
        // targetId를 통해 피신고자 닉네임 조회 (없을 경우 탈퇴한 사용자로 처리)
        String targetNickname = userRepository.findById(report.getTargetId())
                .map(User::getNickName)
                .orElse("알 수 없는 사용자");

        return new ReportResponseDTO.AdminReportDetailDTO(
                report.getId(),
                report.getUser().getNickName(),
                targetNickname,
                report.getGroup().getBook().getTitle(),
                report.getReportType(),
                report.getContent(),
                report.getCreatedAt(),
                report.getSupportStatus(),
                report.getAdminReply(),
                report.getAdminMemo(),
                report.getResolvedAt()
        );
    }

    public Void processReport(Long reportId, ReportRequestDTO.ProcessReportDTO request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        report.resolveReport(request.adminReply(), request.adminMemo());

        reportRepository.save(report);

        return null;
    }
}
