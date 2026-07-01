package com.example.bookiibookii.domain.support.faq.service;

import com.example.bookiibookii.domain.support.faq.dto.res.FaqResponseDTO;
import com.example.bookiibookii.domain.support.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<FaqResponseDTO.FaqItemDTO> getFaqList() {
        return faqRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(faq -> new FaqResponseDTO.FaqItemDTO(faq.getId(), faq.getQuestion(), faq.getAnswer()))
                .toList();
    }
}
