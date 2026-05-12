package com.example.bookiibookii.domain.groupbook.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupBookService {
    private final GroupBookRepository groupBookRepository;

    public String findRecentBookTitleByUserId(Long userId) {
        List<String> titles = groupBookRepository.findRecentBookTitle(userId, PageRequest.of(0, 1));
        return titles.isEmpty() ? null : titles.get(0);
    }

    /**
     * 그룹 참가 확정 시 서재(GroupBook)에 한 건 추가합니다.
     * rating, comment는 null로 생성됩니다.
     */
    public GroupBook createForParticipation(User user, Groups group) {
        GroupBook groupBook = GroupBook.builder()
                .user(user)
                .book(group.getBook())
                .group(group)
                .rating(null)
                .comment(null)
                .build();
        return groupBookRepository.save(groupBook);
    }
}
