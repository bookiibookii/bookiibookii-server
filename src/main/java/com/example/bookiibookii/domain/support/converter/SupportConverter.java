package com.example.bookiibookii.domain.support.converter;

import com.example.bookiibookii.domain.support.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.support.entity.Inquiry;
import com.example.bookiibookii.domain.support.entity.Notice;
import com.example.bookiibookii.domain.support.entity.Report;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupportConverter {
    public List<InquiryResponseDTO.InquiryListDTO> toInquiryListDTO(List<Inquiry> inquiries) {
        return inquiries.stream()
                .map(this::toInquiryDTO)
                .toList();
    }

    public InquiryResponseDTO.InquiryListDTO toInquiryDTO(Inquiry inquiry) {
        return new InquiryResponseDTO.InquiryListDTO(
                inquiry.getId(),
                inquiry.getUser().getNickName(),
                inquiry.getCreatedAt(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getSupportStatus(),
                inquiry.getAdminReply(),
                inquiry.getResolvedAt()
        );
    }

    public List<NoticeResponseDTO.NoticeListDTO> toNoticeListDTO(List<Notice> notices) {
        return notices.stream()
                .map(notice -> new NoticeResponseDTO.NoticeListDTO(
                        notice.getId(),
                        notice.getCreatedAt(),
                        notice.getTitle(),
                        notice.getSummary()
                ))
                .toList();
    }

    public NoticeResponseDTO.NoticeDetailDTO toNoticeDetailDTO(Notice notice) {
        return new NoticeResponseDTO.NoticeDetailDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getImage(),
                notice.getCreatedAt()
        );
    }

    public List<ReportResponseDTO.ReportListDTO> toReportListDTO(List<Report> reports) {
        return reports.stream()
                .map(this::toReportDTO)
                .toList();
    }

    public ReportResponseDTO.ReportListDTO toReportDTO(Report report) {
        return new ReportResponseDTO.ReportListDTO(
                report.getId(),
                report.getUser().getNickName(),
                report.getGroup().getBook().getTitle(), // 그룹 내 도서 제목 포함
                report.getCreatedAt(),
                report.getReportType(),
                report.getContent(),
                report.getSupportStatus(),
                report.getAdminReply(),
                report.getResolvedAt()
        );
    }
}
