package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBookService {
    private final UserBookRepository userBookRepository;

    public String findRecentBookTitleByUserId(Long userId) {
        List<String> titles = userBookRepository.findRecentBookTitle(userId, PageRequest.of(0, 1));
        return titles.isEmpty() ? null : titles.get(0);
    }
}
