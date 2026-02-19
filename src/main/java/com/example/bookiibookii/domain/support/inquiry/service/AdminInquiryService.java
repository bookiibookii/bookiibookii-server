package com.example.bookiibookii.domain.support.inquiry.service;

import com.example.bookiibookii.domain.support.inquiry.converter.InquiryConverter;
import com.example.bookiibookii.domain.support.inquiry.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.inquiry.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.inquiry.entity.Inquiry;
import com.example.bookiibookii.domain.support.inquiry.exception.InquiryException;
import com.example.bookiibookii.domain.support.inquiry.exception.code.InquiryErrorCode;
import com.example.bookiibookii.domain.support.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 전체 문의 리스트 조회
     */
    @Transactional(readOnly = true)
    public Page<InquiryResponseDTO.InquiryListDTO> getAllInquiries(Pageable pageable) {
        // 1. 엔티티 페이지 조회
        Page<Inquiry> inquiryPage = inquiryRepository.findAllOrderByCreatedAtDesc(pageable);

        // 2. Page.map()을 사용하여 내부 엔티티들을 DTO로 변환
        return inquiryPage.map(InquiryConverter::toInquiryListDTO);
    }

    /**
     * 문의 상세 조회
     */
    public InquiryResponseDTO.InquiryListDTO getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        return InquiryConverter.toInquiryListDTO(inquiry);
    }

    /**
     * 문의 답변 등록 및 수정
     */
    @Transactional
    public void answerInquiry(Long inquiryId, InquiryRequestDTO.AnswerInquiryDTO request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));

        inquiry.updateAnswer(request.adminReply());
    }
}