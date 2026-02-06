package com.example.bookiibookii.domain.support.service;

import com.example.bookiibookii.domain.support.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.entity.Inquiry;
import com.example.bookiibookii.domain.support.repository.InquiryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {
    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;

    public void createInquiry(User user, InquiryRequestDTO.CreateInquiryDTO request) {
        Inquiry newInquiry = Inquiry.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .build();

        inquiryRepository.save(newInquiry);
    }

    public List<InquiryResponseDTO.InquiryListDTO> getInquiryList(Long userId) {
        List<Inquiry> inquiries = inquiryRepository.findAllByUserId(userId);

        return inquiries.stream()
                .map(inquiry -> new InquiryResponseDTO.InquiryListDTO(
                        inquiry.getId(),
                        inquiry.getUser().getNickName(),
                        inquiry.getCreatedAt(),
                        inquiry.getTitle(),
                        inquiry.getContent(),
                        inquiry.getSupportStatus(),
                        inquiry.getAdminReply(),
                        inquiry.getResolvedAt()
                ))
                .toList();
    }

}
