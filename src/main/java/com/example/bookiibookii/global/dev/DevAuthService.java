package com.example.bookiibookii.global.dev;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import com.example.bookiibookii.global.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// todo : 테스트용이므로 나중에 삭제
@Service
@Profile("local")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevAuthService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthResponseDTO.LoginResponse login(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        return authService.issueLoginToken(user);
    }
}
