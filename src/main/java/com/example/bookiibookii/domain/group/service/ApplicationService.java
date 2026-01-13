package com.example.bookiibookii.domain.group.service;


import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final GroupsRepository groupsRepository;

    // 신청 조회 로직
    public ApplicationResponseDTO.ApplicationListDTO getApplicantList(Long groupId, Long currentUserId) {
        // 1. 그룹 존재 여부 확인
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 권한 체크: 현재 로그인한 유저가 이 그룹의 방장(Host)인지 확인
        if (!group.getHost().getId().equals(currentUserId)) {
            throw new GeneralException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        // 3. 신청자 명단 조회 (Fetch Join으로 User와 Tag까지 한 번에!)
        List<Application> applications = applicationRepository.findAllWithGuestAndTagsByGroupId(groupId);

        // 4. 엔티티 리스트를 DTO 리스트로 변환
        List<ApplicationResponseDTO.ApplicationDetailDTO> detailDTOs = applications.stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());

        return ApplicationResponseDTO.ApplicationListDTO.builder()
                .applicationList(detailDTOs)
                .totalCount(detailDTOs.size())
                .build();
    }

    //참가 수락 거절 로직
    @Transactional(readOnly = false) // 쓰기 작업이므로 readOnly = false (기본값)로 덮어씌움
    public ApplicationResponseDTO.UpdateResultDTO updateApplicationStatus(Long applyId, Long userId, ApplicationStatus status) {

        // 1. 신청 내역 존재 여부 확인
        Application application = applicationRepository.findById(applyId)
                .orElseThrow(() -> new GeneralException(GroupErrorCode.APPLICATION_NOT_FOUND));

        // 2. 권한 체크: 이 신청이 들어온 그룹의 방장이 요청자(userId)와 일치하는지 확인
        if (!application.getGroup().getHost().getId().equals(userId)) {
            throw new GeneralException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        // 3. 상태 업데이트 (JPA Dirty Checking으로 자동 반영)
        application.updateStatus(status);

        // 4. 결과 DTO 반환
        return ApplicationResponseDTO.UpdateResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus())
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();
    }
    private ApplicationResponseDTO.ApplicationDetailDTO toDetailDTO(Application application) {
        User guest = application.getGuest();

        // 태그 이름만 String 리스트로 추출
        //List<String> tagNames = guest.getUserTags().stream()
                //.map(ut -> ut.getTag().getName())
                //.collect(Collectors.toList());

        return ApplicationResponseDTO.ApplicationDetailDTO.builder()
                .applicationId(application.getApplicationId())
                .userId(guest.getId())
                .name(guest.getName())
                //.profileImageUrl(guest.getImageUrl()) //프로필 사진
                .createdAt(application.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                //.tags(tagNames) //grouptag
                .applyMsg(application.getApplyMsg())
                .build();
    }
}
