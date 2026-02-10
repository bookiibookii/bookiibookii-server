package com.example.bookiibookii.domain.support.service;

import com.example.bookiibookii.domain.support.converter.SupportConverter;
import com.example.bookiibookii.domain.support.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.entity.Inquiry;
import com.example.bookiibookii.domain.support.repository.InquiryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final SupportConverter supportConverter;

    public void createInquiry(User user, InquiryRequestDTO.CreateInquiryDTO request) {
        Inquiry newInquiry = Inquiry.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();

        inquiryRepository.save(newInquiry);
    }

    @Transactional(readOnly = true)
    public List<InquiryResponseDTO.InquiryListDTO> getInquiryList(Long userId) {
        List<Inquiry> inquiries = inquiryRepository.findAllByUserId(userId);

        return supportConverter.toInquiryListDTO(inquiries);
    }
}
