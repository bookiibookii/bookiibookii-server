package com.example.bookiibookii.domain.support.inquiry.converter;

import com.example.bookiibookii.domain.support.inquiry.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.inquiry.entity.Inquiry;

import java.util.List;
import java.util.stream.Collectors;

public class InquiryConverter {

    // 단일 엔티티를 DTO로 변환
    public static InquiryResponseDTO.InquiryListDTO toInquiryListDTO(Inquiry inquiry) {
        return new InquiryResponseDTO.InquiryListDTO(
                inquiry.getId(),
                inquiry.getUser().getId(),
                inquiry.getUser().getNickName(),
                inquiry.getCreatedAt(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getSupportStatus(),
                inquiry.getAdminReply(),
                inquiry.getResolvedAt()
        );
    }

    // 엔티티 리스트를 DTO 리스트로 변환
    public static List<InquiryResponseDTO.InquiryListDTO> toInquiryListDTOList(List<Inquiry> inquiries) {
        return inquiries.stream()
                .map(InquiryConverter::toInquiryListDTO)
                .collect(Collectors.toList());
    }
}