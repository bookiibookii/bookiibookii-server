package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.enums.Role;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserResponseDTO.AdminUserListDTO> getAdminUsers() {
        return userRepository.findAllByRoleOrderByIdAsc(Role.ADMIN).stream()
                .map(user -> UserResponseDTO.AdminUserListDTO.builder()
                        .id(user.getId())
                        .nickname(user.getNickName())
                        .introduction(user.getIntroduction())
                        .build())
                .toList();
    }
}
