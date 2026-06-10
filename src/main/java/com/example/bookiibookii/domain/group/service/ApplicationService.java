package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.dto.req.ApplicationRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.memberbook.service.MemberBookService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final DomainEventPublisher publisher;
    private final MemberBookService memberBookService;
    private final UserImageS3Service userImageS3Service;
    private final BookService bookService;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    // 신청 조회 로직
    public ApplicationResponseDTO.ApplicationListDTO getApplicantList(Long groupId, Long currentUserId) {
        // 1. 그룹 존재 여부 확인
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 권한 체크: 현재 로그인한 유저가 이 그룹의 방장(Host)인지 확인
        if (!group.getHost().getId().equals(currentUserId)) {
            throw new GroupException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        // 3. 신청자 명단 조회
        List<Application> applications = applicationRepository.findAllWithGuestByGroupId(groupId);

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

        // 1. 신청 내역 및 그룹 조회 (비관적 락 적용으로 Race Condition 방지)
        Application application = applicationRepository.findById(applyId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.APPLICATION_NOT_FOUND));

        Groups group = groupsRepository.findByIdForUpdate(application.getGroup().getId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 권한 및 상태 체크
        if (!group.getHost().getId().equals(userId)) {
            throw new GroupException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new GroupException(GroupErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        // 알림용 그룹 정보 조회 (Fetch Join)
        Groups thisGroup = groupsRepository.findByIdWithBookAndHost(group.getId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 3. 수락(ACCEPTED) 로직
        if (status == ApplicationStatus.ACCEPTED) {
            // 현재 인원 체크 (호스트 포함 MatchedMember 수)
            long currentTotalCount = matchedMemberRepository.countByGroup(group);

            if (currentTotalCount >= group.getMaxCapacity()) {
                throw new GroupException(GroupErrorCode.GROUP_FULL);
            }

            // 신청서 수락 상태로 업데이트
            application.updateStatus(ApplicationStatus.ACCEPTED);

            // 확정 멤버(MatchedMember) 추가
            MatchedMember newMember = MatchedMember.builder()
                    .group(group)
                    .user(application.getGuest())
                    .role(RoleStatus.GUEST)
                    // .currentReadingRate(0)
                    .build();
            matchedMemberRepository.save(newMember);

            LocalDateTime matchedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

            // 서재: MemberBook 4건(멤버당 2권)
            memberBookService.createLibraryOnMatch(group, newMember, application.getBook(), matchedAt);

            // 개별 수락 알림 발송
            publisher.publish(new GroupNotificationEvent(
                    MATCH_SUCCEEDED, userId, groupTitle(thisGroup),
                    newMember.getUser().getId(), null, group.getId(),
                    application.getApplicationId(), ExchangeType.from(group.getTradeType())
            ));

            // 수락일이 독서 시작일 → 즉시 MATCHED 전환
            group.setStartDate(LocalDate.now(ZoneId.of("Asia/Seoul")));
            group.updateStatus(GroupStatus.MATCHED);

            // 나머지 대기자들 자동 거절 처리
            List<Application> pendingApplications = applicationRepository.findAllPendingByGroupId(group.getId());
            List<Long> autoRejectedReceiverIds = pendingApplications.stream()
                    .map(app -> app.getGuest().getId())
                    .distinct()
                    .toList();

            for (Application pendingApp : pendingApplications) {
                pendingApp.updateStatus(ApplicationStatus.REJECTED);
            }

            if (!autoRejectedReceiverIds.isEmpty()) {
                publisher.publish(new GroupNotificationEvent(
                        MATCH_AUTO_REJECTED, userId, thisGroup.getBook().getTitle(),
                        null, autoRejectedReceiverIds, group.getId(), null, null
                ));
            }

        }
        // 4. 거절(REJECTED) 로직
        else {
            application.updateStatus(ApplicationStatus.REJECTED);
            publisher.publish(new GroupNotificationEvent(
                    MATCH_REJECTED, userId, groupTitle(thisGroup),
                    application.getGuest().getId(), null, group.getId(),
                    application.getApplicationId(), null
            ));
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

        //그룹 존재여부 확인 (락 적용 — 수락 트랜잭션과 동일한 락을 사용해 MATCHED 직후 신청 유입 차단)
        Groups group = groupsRepository.findByIdForUpdateWithBookAndHost(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        //그룹 상태 확인(RECRUTING)
        if(group.getGroupStatus() != GroupStatus.RECRUITING){
            throw new GroupException(GroupErrorCode.GROUP_NOT_RECRUITING);
        }

        // 게스트 참여 제한 체크 (최대 3개)
        List<GroupStatus> activeStatuses = List.of(GroupStatus.RECRUITING, GroupStatus.MATCHED);

        // 참여 확정된 게스트 그룹 수
        long matchedGuestCount = matchedMemberRepository.countByUserIdAndRoleAndGroup_GroupStatusIn(userId, RoleStatus.GUEST, activeStatuses);

        // 현재 대기 중인 신청 수
        long pendingApplyCount = applicationRepository.countByGuestIdAndApplicationStatus(userId, ApplicationStatus.PENDING);

        //그룹 신청 3개 이상인 경우
        //demoday 대비 그룹참여 제한 300 설정
        if (matchedGuestCount + pendingApplyCount >= 300) {
            throw new GroupException(GroupErrorCode.GUEST_MAX_LIMIT_EXCEEDED);
        }


        //권한체크(HOST는 참가신청불가)
        if(group.getHost().getId().equals(userId)){
            throw new GroupException(GroupErrorCode.HOST_CANNOT_APPLY);
        }

        //중복신청확인
        if(applicationRepository.existsByGroupIdAndGuestId(groupId,userId)){
            throw new GroupException(GroupErrorCode.ALREADY_PROCESSED_APPLICATION);
        }

        //유저 정보 조회
        User guest = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        // 교환할 책 조회/생성
        Book book = bookService.getOrCreateByIsbn13(request.getIsbn13());

        // 신청 엔티티 생성 및 저장
        Application application = Application.builder()
                .group(group)
                .guest(guest)
                .book(book)
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
        publisher.publish(new GroupNotificationEvent(
                JOIN_REQUESTED,
                userId,
                groupTitle(group),
                group.getHost().getId(),
                null,
                groupId,
                application.getApplicationId(),
                null
        ));

        return ApplicationResponseDTO.JoinResultDTO.builder()
                .applicationId(application.getApplicationId())
                .status(application.getApplicationStatus().name())
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .build();
    }

    private String groupTitle(Groups group) {
        if (group.getGroupName() != null && !group.getGroupName().isBlank()) {
            return group.getGroupName();
        }
        return group.getBook().getTitle();
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
        Application application = applicationRepository.findByGroupIdAndGuestId(groupId, userId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.APPLICATION_NOT_FOUND));

        // 4. 만약 이미 승인까지 난 멤버라면 MatchedMember에서도 데이터 삭제
        matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .ifPresent(member -> {
                    if (member.getRole() == RoleStatus.HOST) {
                        throw new GroupException(GroupErrorCode.HOST_CANNOT_LEAVE);
                    }
                    matchedMemberRepository.delete(member);
                });

        // 5. 신청 내역(Application) 삭제
        applicationRepository.delete(application);

        return ApplicationResponseDTO.CancelResultDTO.builder()
                .groupId(groupId)
                .canceledAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();
    }

    // 내가 신청한 그룹 목록 조회
    public ApplicationResponseDTO.MyApplicationListDTO getMyApplicationList(Long userId) {
        List<Application> applications = applicationRepository.findMyActiveApplications(userId);

        List<ApplicationResponseDTO.MyApplicationCardDTO> cards = applications.stream()
                .map(this::toMyApplicationCardDTO)
                .collect(Collectors.toList());

        return ApplicationResponseDTO.MyApplicationListDTO.builder()
                .applicationList(cards)
                .totalCount(cards.size())
                .build();
    }

    private ApplicationResponseDTO.MyApplicationCardDTO toMyApplicationCardDTO(Application application) {
        Groups group = application.getGroup();
        String hostProfileImageUrl = null;
        if (group.getHost().getUserImage() != null) {
            hostProfileImageUrl = userImageS3Service.generatePresignedGetUrl(
                    group.getHost().getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        }

        return ApplicationResponseDTO.MyApplicationCardDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .hostNickname(group.getHost().getNickName())
                .hostProfileImageUrl(hostProfileImageUrl)
                .bookImage(group.getBook().getImage())
                .bookTitle(group.getBook().getTitle())
                .author(group.getBook().getAuthor())
                .readingPeriod(group.getReadingPeriod())
                .tradeType(group.getTradeType().name())
                .applicationStatus(application.getApplicationStatus().name())
                .build();
    }

    private ApplicationResponseDTO.ApplicationDetailDTO toDetailDTO(Application application) {
        User guest = application.getGuest();

        String profileImageUrl = null;
        if (guest.getUserImage() != null) {
            profileImageUrl = userImageS3Service.generatePresignedGetUrl(
                    guest.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        }

        Book book = application.getBook();

        return ApplicationResponseDTO.ApplicationDetailDTO.builder()
                .applicationId(application.getApplicationId())
                .user(guest.getId())
                .name(guest.getNickName())
                .profileImageUrl(profileImageUrl)
                .createdAt(application.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .applyMsg(application.getApplyMsg())
                .bookTitle(book != null ? book.getTitle() : null)
                .bookAuthor(book != null ? book.getAuthor() : null)
                .bookImage(book != null ? book.getImage() : null)
                .build();
    }
}
