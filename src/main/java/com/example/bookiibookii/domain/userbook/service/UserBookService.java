package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.exception.UserBookException;
import com.example.bookiibookii.domain.userbook.exception.code.UserBookErrorCode;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBookService {
    private final UserBookRepository userBookRepository;

    public String findRecentBookTitleByUserId(Long userId) {
        return userBookRepository.findRecentBookTitle(userId)
                .orElseThrow(() -> new UserBookException(UserBookErrorCode.NOT_FOUND));
    }
}
