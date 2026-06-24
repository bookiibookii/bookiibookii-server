package com.example.bookiibookii.domain.support.inquiry.service;

import com.example.bookiibookii.domain.support.inquiry.converter.InquiryConverter;
import com.example.bookiibookii.domain.support.inquiry.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.inquiry.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.inquiry.entity.Inquiry;
import com.example.bookiibookii.domain.support.inquiry.repository.InquiryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * [유저] 문의하기 등록
     */
    @Transactional
    public void createInquiry(User user, InquiryRequestDTO.CreateInquiryDTO request) {
        Inquiry newInquiry = Inquiry.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();

        inquiryRepository.save(newInquiry);
    }

    /**
     * [유저] 내 문의 내역 리스트 조회
     */
    public List<InquiryResponseDTO.InquiryListDTO> getInquiryList(Long userId) {
        // Repository에서 이미 @EntityGraph와 ORDER BY가 적용된 메서드 호출
        List<Inquiry> inquiries = inquiryRepository.findAllByUserId(userId);

        // Converter를 사용하여 DTO 리스트로 변환
        return InquiryConverter.toInquiryListDTOList(inquiries);
    }
}