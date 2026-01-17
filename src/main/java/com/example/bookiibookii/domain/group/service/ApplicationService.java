package com.example.bookiibookii.domain.group.service;


import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
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

        //3. 이미 처리된 신청인지 확인
        // 상태가 PENDING(대기 중)이 아닐 때 수락/거절을 시도하면 예외 발생
        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new GeneralException(GroupErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        // 4. 수락(ACCEPTED) 시도 시 정원 초과 여부 사전 체크
        if (status == ApplicationStatus.ACCEPTED) {
            Groups groups = application.getGroup();

            // 현재 이미 수락된 인원수 계산
            long currentAcceptedCount = applicationRepository.countByGroupGroupIdAndApplicationStatus(groups.getGroupId(), ApplicationStatus.ACCEPTED);

            // 이미 정원이 찼는데 또 수락하려는 경우 예외 발생
            if (currentAcceptedCount >= groups.getMaxCapacity()) {
                throw new GeneralException(GroupErrorCode.GROUP_FULL);
            }
        }

        // 4. 상태 업데이트
        application.updateStatus(status);

        // 5. 수락 시: 그룹 상태를 진행중으로 변경(MATCHED)
        if (status == ApplicationStatus.ACCEPTED) {
            Groups groups = application.getGroup();
            // 현재 수락된 총 인원수 계산
            long currentAcceptedCount = applicationRepository.countByGroupGroupIdAndApplicationStatus(groups.getGroupId(), ApplicationStatus.ACCEPTED);

            // 정원이 다 찼을 경우만
            if (currentAcceptedCount >= groups.getMaxCapacity()) {
                groups.updateStatus(GroupStatus.MATCHED);

                // 나머지 PENDING 인원들 조회 및 일괄 거절
                List<Application> pendingApplications = applicationRepository.findAllPendingByGroupId(groups.getGroupId());

                for (Application pendingApp : pendingApplications) {
                    pendingApp.updateStatus(ApplicationStatus.REJECTED);

                    //시스템 알람 발송 (알람 파트 구현 시 주석 해제)
                    // notificationService.sendAutoRejectNotification(pendingApp.getGuest(), group);
                }
            }

        }

        // 6. 거절 시: 알람 발송
        //if (status == ApplicationStatus.REJECTED) {
        //    notificationService.sendRejectNotification(application.getGuest(), application.getGroup());
        //}

        //  결과 DTO 반환
        return ApplicationResponseDTO.UpdateResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus())
                .groupStatus(application.getGroup().getGroupStatus())
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
