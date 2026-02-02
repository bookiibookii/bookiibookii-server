package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.dto.req.ApplicationRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.bookiibookii.domain.group.enums.GroupNotiType.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final GroupsRepository groupsRepository;
    private final UserRepository userRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DomainEventPublisher publisher;

    // 신청 조회 로직
    public ApplicationResponseDTO.ApplicationListDTO getApplicantList(Long groupId, Long currentUserId) {
        // 1. 그룹 존재 여부 확인
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 권한 체크: 현재 로그인한 유저가 이 그룹의 방장(Host)인지 확인
        if (!group.getHost().getId().equals(currentUserId)) {
            throw new GroupException(GroupErrorCode.MEMBER_NOT_HOST);
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

    // 참가 수락 || 거절 로직
    @Transactional(readOnly = false) // 쓰기 작업이므로 readOnly = false (기본값)로 덮어씌움
    public ApplicationResponseDTO.UpdateResultDTO updateApplicationStatus(Long applyId, Long userId, ApplicationStatus status)
    {

        // 1. 신청 내역 존재 여부 확인
        Application application = applicationRepository.findById(applyId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.APPLICATION_NOT_FOUND));

        // 그룹 조회를 할 때 락을 걸어 Race Condition 해결 (이 시점에 다른 쓰레드는 대기 상태가 됨)
        Groups group = groupsRepository.findByIdForUpdate(application.getGroup().getGroupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 권한 체크: 이 신청이 들어온 그룹의 방장이 요청자(userId)와 일치하는지 확인
        if (!application.getGroup().getHost().getId().equals(userId)) {
            throw new GroupException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        //3. 이미 처리된 신청인지 확인
        // 상태가 PENDING(대기 중)이 아닐 때 수락/거절을 시도하면 예외 발생
        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new GroupException(GroupErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        // 4. 수락(ACCEPTED) 시도 시 정원 초과 여부 사전 체크
        if (status == ApplicationStatus.ACCEPTED) {
            // [핵심] 방장을 포함한 현재 확정 인원수 계산 (MatchedMember에서 조회)
            long currentTotalCount = matchedMemberRepository.countByGroup(group);
                // 정원 초과 체크 (방장 포함 maxCapacity와 비교)
                if (currentTotalCount >= group.getMaxCapacity()) {
                    throw new GroupException(GroupErrorCode.GROUP_FULL);
                }

                // [핵심] MatchedMember에 새 멤버 등록 및 순서 부여
                // 방장이 1번이므로, 첫 번째 수락자는 2번(1+1)이 됩니다.
                MatchedMember newMember = MatchedMember.builder()
                        .group(group)
                        .user(application.getGuest())
                        .role(RoleStatus.GUEST)
                        .readingOrder((int) currentTotalCount + 1) // 현재 인원 + 1 순서 부여
                        .build();
                // 수락 알람 발송 (추가 가능)
                // notificationService.sendAcceptNotification(application.getGuest(), group);
                matchedMemberRepository.save(newMember);

                // 신청서 상태 업데이트
                application.updateStatus(ApplicationStatus.ACCEPTED);

                // 3. 정원이 다 찼을 경우 (RELAY는 2명, TOGETHER는 설정값) 그룹 상태 변경 및 나머지 거절
                if (currentTotalCount + 1 >= group.getMaxCapacity()) {
                    group.updateStatus(GroupStatus.MATCHED);

                    //매칭 이벤트 발행
                    eventPublisher.publishEvent(new GroupMatchedEvent(
                            group.getGroupId(),
                            group.getHost().getId(),
                            group.getStartDate(),
                            group.getMaxCapacity()
                    ));

                    // 나머지 대기 인원 일괄 거절
                    List<Application> pendingApplications = applicationRepository.findAllPendingByGroupId(group.getGroupId());
                    for (Application pendingApp : pendingApplications) {
                        pendingApp.updateStatus(ApplicationStatus.REJECTED);
                        //  시스템 자동 거절 알람 발송
                        // notificationService.sendAutoRejectNotification(pendingApp.getGuest(), group);
                    }
                }
                // 알림 publish
                publisher.publish(new GroupNotificationEvent(MATCH_AUTO_REJECTED, userId, null, group.getGroupId()));
            }

        } else {
            application.updateStatus(ApplicationStatus.REJECTED);

            // 알림 publish
            publisher.publish(new GroupNotificationEvent(MATCH_REJECTED, userId, application.getGuest().getId(), group.getGroupId()));
        }

        return ApplicationResponseDTO.UpdateResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus())
                .groupStatus(group.getGroupStatus())
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();
    }

    //참가신청 service
    @Transactional(readOnly = false)
    public ApplicationResponseDTO.JoinResultDTO joinGroup(Long groupId, Long userId, ApplicationRequestDTO.JoinApplicationDTO request){
        //그룹 존재여부 확인
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        //그룹 상태 확인(RECRUTING)
        if(group.getGroupStatus() != GroupStatus.RECRUITING){
            throw new GroupException(GroupErrorCode.GROUP_NOT_RECRUITING);
        }

        //권한체크(HOST는 참가신청불가)
        if(group.getHost().getId().equals(userId)){
            throw new GroupException(GroupErrorCode.HOST_CANNOT_APPLY);
        }

        //중복신청확인
        if(applicationRepository.existsByGroupGroupIdAndGuestId(groupId,userId)){
            throw new GroupException(GroupErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        //유저 정보 조회
        User guest = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        // 신청 엔티티 생성 및 저장
        Application application = Application.builder()
                .group(group)
                .guest(guest)
                .applyMsg(request.getApplyMsg())
                .applicationStatus(ApplicationStatus.PENDING)
                .build();

        try {
            applicationRepository.save(application);
        } catch (DataIntegrityViolationException ex) {
            // DB에서 유니크 제약조건 위반이 발생하면(중복 신청 시) 처리
            throw new GroupException(GroupErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        // 알림 publish
        publisher.publish( new GroupNotificationEvent(JOIN_REQUESTED, userId, group.getHost().getId(), groupId) );

        return ApplicationResponseDTO.JoinResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus().name())
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .build();
    }

    //참여 취소하기
    @Transactional(readOnly = false)
    public ApplicationResponseDTO.CancelResultDTO cancelApplication (Long groupId, Long userId){

        // 그룹에 비관적 락을 먼저 걸고 조회
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 이미 모집 완료(MATCHED)된 경우 취소 불가 (GROUP400_12)
        if (group.getGroupStatus() == GroupStatus.MATCHED ||
                group.getGroupStatus() == GroupStatus.COMPLETED ||
                group.getGroupStatus() == GroupStatus.DELETED) {
            throw new GroupException(GroupErrorCode.APPLY_CANT_CANCEL);
        }

        // 3. Application을 조회
        Application application = applicationRepository.findByGroupGroupIdAndGuestId(groupId, userId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.APPLICATION_NOT_FOUND));

        // 4. 만약 이미 승인까지 난 멤버라면 MatchedMember에서도 데이터 삭제
        matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .ifPresent(member -> {
                    if (member.getRole() == RoleStatus.HOST) {
                        throw new GroupException(GroupErrorCode.HOST_CANNOT_LEAVE);
                    }
                    matchedMemberRepository.delete(member);
                });

        // 5. 신청 내역(Application) 삭제
        applicationRepository.delete(application);

        // 6. 인원 재계산 및 필요 시 모집 중(RECRUITING)으로 상태 복구
        long currentCount = matchedMemberRepository.countByGroup(group);
        if (currentCount < group.getMaxCapacity()) {
            group.updateStatus(GroupStatus.RECRUITING);
        }

        return ApplicationResponseDTO.CancelResultDTO.builder()
                .groupId(groupId)
                .canceledAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();
    }

    private ApplicationResponseDTO.ApplicationDetailDTO toDetailDTO(Application application) {
        User guest = application.getGuest();

        // 태그 이름만 String 리스트로 추출
        // 1. ERD 구조대로 유저 -> 유저태그 리스트 -> 각 태그의 코드를 추출
        List<String> top3Tags = (guest.getUserTags() == null) ? new ArrayList<>() :
                guest.getUserTags().stream()
                        // 1. 점수(score) 높은 순서대로 정렬
                        .sorted(Comparator.comparingInt(UserTag::getScore).reversed())
                        // 2. 상위 3개만 자르기
                        .limit(3)
                        // 3. 태그의 이름(또는 코드) 꺼내기
                        .map(ut -> ut.getTag().getCode())
                        .toList();

        return ApplicationResponseDTO.ApplicationDetailDTO.builder()
                .applicationId(application.getApplicationId())
                .user(guest.getId())
                .name(guest.getName())
                //.profileImageUrl(guest.getImageUrl()) //프로필 사진
                .createdAt(application.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .tags(top3Tags)
                .applyMsg(application.getApplyMsg())
                .build();
    }


}
