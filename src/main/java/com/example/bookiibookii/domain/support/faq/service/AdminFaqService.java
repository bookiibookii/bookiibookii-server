package com.example.bookiibookii.domain.support.faq.service;

import com.example.bookiibookii.domain.support.faq.dto.req.FaqRequestDTO;
import com.example.bookiibookii.domain.support.faq.dto.res.FaqResponseDTO;
import com.example.bookiibookii.domain.support.faq.entity.Faq;
import com.example.bookiibookii.domain.support.faq.exception.FaqException;
import com.example.bookiibookii.domain.support.faq.exception.code.FaqErrorCode;
import com.example.bookiibookii.domain.support.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminFaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<FaqResponseDTO.FaqListDTO> getFaqList() {
        return faqRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(faq -> new FaqResponseDTO.FaqListDTO(
                        faq.getId(),
                        faq.getQuestion(),
                        faq.getAnswer(),
                        faq.getCreatedAt(),
                        faq.getUpdatedAt()
                ))
                .toList();
    }

    public void createFaq(FaqRequestDTO.CreateFaqDTO request) {
        Faq faq = Faq.builder()
                .question(request.question())
                .answer(request.answer())
                .build();
        faqRepository.save(faq);
    }

    public void updateFaq(Long faqId, FaqRequestDTO.UpdateFaqDTO request) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new FaqException(FaqErrorCode.FAQ_NOT_FOUND));

        if (request.question() != null) faq.updateQuestion(request.question());
        if (request.answer() != null) faq.updateAnswer(request.answer());
    }

    public void deleteFaq(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new FaqException(FaqErrorCode.FAQ_NOT_FOUND));
        faqRepository.delete(faq);
    }
}
