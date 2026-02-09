package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
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

    /**
     * 그룹 참가 확정 시 서재(UserBook)에 한 건 추가합니다.
     * rating, comment는 null로 생성됩니다.
     */
    public UserBook createForParticipation(User user, Groups group) {
        UserBook userBook = UserBook.builder()
                .user(user)
                .book(group.getBook())
                .group(group)
                .rating(null)
                .comment(null)
                .build();
        return userBookRepository.save(userBook);
    }
}
