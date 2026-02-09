package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.GroupTag;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.*;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.*;
import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.tag.exception.TagException;
import com.example.bookiibookii.domain.tag.exception.code.TagErrorCode;
import com.example.bookiibookii.domain.tag.repository.TagRepository;
import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.service.KeywordMatchService;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.AddressRepository;
import com.example.bookiibookii.domain.user.repository.UserTagRepository;
import com.example.bookiibookii.domain.user.service.BadWordService;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.userbook.service.UserBookService;
import com.example.bookiibookii.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.bookiibookii.domain.group.enums.GroupNotiType.GROUP_DELETED;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupService {

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final ApplicationRepository applicationRepository;
    private final BookService bookService;
    private final GroupQueryRepository groupQueryRepository;
    private final UserTagRepository userTagRepository;
    private final TagRepository tagRepository;
    private final KeywordMatchService keywordMatchService;
    private final DomainEventPublisher publisher;
    private final GroupTagRepository groupTagRepository;
    private final MatchedMemberQueryRepository matchedMemberQueryRepository;
    private final UserImageS3Service userImageS3Service;
    private final UserBookService userBookService;
    private final AddressRepository addressRepository;
    private final RedisUtil redisUtil;
    private final MeetingRepository meetingRepository;
    private final BadWordService badWordService;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    //그룹생성 service
    public GroupResponseDTO.CreateResultDTO createGroup(User host, GroupRequestDTO.CreateDTO request){

        //로그인 여부 검증
        if(host == null){
            throw new UserException(UserErrorCode.NOT_FOUND);
        }

        // 1. 공통 정책 검증 (도서 필수, 날짜, 기간 체크)
        validateCommonPolicy(request);


        // 2. 도서 존재 여부 확인
        Book book = bookService.getOrCreateByIsbn13(request.getIsbn13());

        // 3. 타입별 변수 설정 및 정책 검증
        Integer finalCapacity;
        TradeType finalTradeType;

        if (request.getGroupType() == GroupType.RELAY) {
            // [1:1 이어읽기] 방장 포함 2명 고정 및 위치 정보 검증
            validateRelayPolicy(host, request);
            finalCapacity = 2;
            finalTradeType = request.getTradeType(); //사용자가 선택한 교환 방식 적용
        } else {
            // [1:N 함께읽기] 방장 포함 최대 8명 제한 및 기본 택배 설정
            validateTogetherPolicy(request);
            finalCapacity = request.getMaxCapacity();
            finalTradeType = TradeType.NONE;
        }

        // 4. Groups 엔티티 빌드 (ID 기반 연관관계 매핑)
        Groups group = Groups.builder()
                .book(book)
                .host(host)
                .maxCapacity(finalCapacity)
                .startDate(request.getStartDate())
                .readingPeriod(request.getReadingPeriod())
                .groupComment(request.getGroupComment())
                .customTag(request.getCustomTag())
                .groupType(request.getGroupType())
                .tradeType(finalTradeType)
                .groupStatus(GroupStatus.RECRUITING) // 초기 상태는 모집 중
                .preferRegion(request.getPreferRegion()) //선호장소 저장
                .build();

        // 5. 독서 태그 저장 로직
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (GroupRequestDTO.TagSettingDTO tagDto : request.getTags()) {
                TagType type = tagDto.type();
                List<String> codes = tagDto.value();
                List<Tag> tags = tagRepository.findByTypeAndCodeIn(type, codes);

                if (tags.size() != codes.size()) {
                    throw new TagException(TagErrorCode.INVALID_TAG_CODE);
                }
                tags.forEach(group::addGroupTag);
            }
        }

        Groups savedGroup = groupsRepository.save(group);

        // 1:1 직접 교환일 때 Meeting 초기 데이터 생성
        if (request.getGroupType() == GroupType.RELAY && request.getTradeType() == TradeType.DIRECT) {

            // request.getMeetPlace()를 통해 유저가 최종 수정한 주소를 저장
            Meeting initialMeeting = Meeting.builder()
                    .group(savedGroup)
                    .meetingPlace(request.getMeetPlace()) //host 프로필이 아닌 DTO 값 사용
                    .trackerStatus(TrackerStatus.READY)
                    .meetingTime(null)
                    .build();

            meetingRepository.save(initialMeeting);
        }

        // 방장을 MatchedMember의 첫 번째 멤버로 등록
        MatchedMember hostMember = MatchedMember.builder()
                .group(savedGroup)           // 엔티티의 private Groups group;
                .user(host)                // 엔티티의 private User user;
                .role(RoleStatus.HOST)       // 엔티티의 RoleStatus 타입 사용
                .readingOrder(1)             // 엔티티의 private Integer readingOrder;
                .currentReadingRate(0)      // 초기 독서율 0으로 세팅
                .build();

        matchedMemberRepository.save(hostMember);

        // 방장 서재(UserBook)에 추가
        userBookService.createForParticipation(host, savedGroup);

        List<Keyword> matched = keywordMatchService.matchForBook(book.getTitle(), book.getAuthor());

        List<Long> ids = matched.stream().map(Keyword::getId).toList();
        List<String> texts = matched.stream().map(Keyword::getContent).toList();

        publisher.publish(new KeywordGroupCreatedEvent(group.getGroupId(), texts, ids));

        return GroupResponseDTO.CreateResultDTO.builder()
                .groupId(savedGroup.getGroupId()) //
                .groupStatus(savedGroup.getGroupStatus()) //
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .build();
    }

    private void validateCommonPolicy(GroupRequestDTO.CreateDTO request) {
        // 도서 선택 필수
        if (request.getIsbn13() == null || request.getIsbn13().isBlank()) {
            throw new GroupException(GroupErrorCode.BOOK_NOT_SELECTED);
        }

        // 시작 날짜는 오늘 이후(내일부터) 선택 가능
        //if (request.getStartDate() == null || !request.getStartDate().isAfter(LocalDate.now())) {
          //  throw new GroupException(GroupErrorCode.INVALID_START_DATE);
        //}

        // 독서 기간 최소 3일 ~ 최대 30일
        if (request.getReadingPeriod() == null || request.getReadingPeriod() < 3 || request.getReadingPeriod() > 30) {
            throw new GroupException(GroupErrorCode.INVALID_READING_PERIOD);
        }

        // 그룹 소개글 검증 로직
        if (request.getGroupComment() == null || request.getGroupComment().isBlank()) {
            throw new GroupException(GroupErrorCode.COMMENT_REQUIRED);
        }

        // 4. 소개글 글자 수 제한 (최대 500자)
        if (request.getGroupComment().length() > 500) {
            throw new GroupException(GroupErrorCode.INTRODUCTION_TOO_LONG);
        }

        // 5. 금칙어 검증 (BadWordService 활용)
        // 닉네임에서 썼던 아호-코라식 알고리즘이 500자 문장도 순식간에 훑어줍니다.
        if (badWordService.containsBadWord(request.getGroupComment())) {
            throw new GroupException(GroupErrorCode.FORBIDDEN_WORD_INCLUDED);
        }
    }

    //1:1 relay 읽기 정책
    private void validateRelayPolicy(User host, GroupRequestDTO.CreateDTO request) {
        // 직접 교환 시 유저 엔티티의 지역/상세장소 정보 필수
        if (request.getTradeType() == TradeType.DIRECT) {
            // host 프로필이 비어있어도 request에 값이 있다면 통과
            if (request.getPreferRegion() == null || request.getMeetPlace() == null) {
                throw new GroupException(GroupErrorCode.USER_LOCATION_NOT_FOUND);
            }
        }

        // 택배 교환(DELIVERY) 시: 등록된 배송지(Address) 존재 여부 확인
        if (request.getTradeType() == TradeType.DELIVERY) {
            // addressRepository를 통해 해당 유저의 주소가 등록되어 있는지 확인
            boolean hasAddress = addressRepository.existsByUserId(host.getId());
            if (!hasAddress) {
                throw new GroupException(GroupErrorCode.ADDRESS_NOT_FOUND);
            }
        }
    }



   //1:n together 읽기 정책
    private void validateTogetherPolicy(GroupRequestDTO.CreateDTO request) {
        // 방장 포함 인원수는 최소 2명에서 최대 8명까지
        if (request.getMaxCapacity() == null || request.getMaxCapacity() < 2 || request.getMaxCapacity() > 8) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_CAPACITY);
        }
    }

    //그룹수정 service
    @Transactional
    public GroupResponseDTO.UpdateResultDTO updateGroup(Long groupId, User host, GroupRequestDTO.UpdateDTO request){

        //락으로 그룹 조회
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        //host 권한 체크
        if(!group.getHost().getId().equals(host.getId())){
            throw new GroupException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        //진행중(MATCHED)인 그룹은 수정 불가
        if(group.getGroupStatus() != GroupStatus.RECRUITING){
            throw new GroupException(GroupErrorCode.GROUP_CANT_UPDATE);
        }

        //날짜수정 (이미 시작한 그룹은 수정불가) 유효성 검사
        // 4. 날짜 및 기간 수정 시 유효성 검사 (생성 시 규칙과 동일)
        if (request.getStartDate() != null) {
            // 시작 날짜는 오늘 이후(내일부터) 선택 가능
            if (!request.getStartDate().isAfter(LocalDate.now())) {
                throw new GroupException(GroupErrorCode.INVALID_START_DATE);
            }
            group.setStartDate(request.getStartDate());
        }

        if (request.getReadingPeriod() != null) {
            // 독서 기간 최소 3일 ~ 최대 30일
            if (request.getReadingPeriod() < 3 || request.getReadingPeriod() > 30) {
                throw new GroupException(GroupErrorCode.INVALID_READING_PERIOD);
            }
            group.setReadingPeriod(request.getReadingPeriod());
        }

        //그룹 소개글 수정
        if(request.getGroupComment() != null){
            group.setGroupComment(request.getGroupComment());
        }

        // 커스텀 태그 수정
        if(request.getCustomTag() != null){
            group.setCustomTag(request.getCustomTag());
        }

        //독서 태그 수정
        if (request.getTags() != null) {
            group.clearGroupTags();
            for (GroupRequestDTO.TagSettingDTO tagDto : request.getTags()) {
                TagType type = tagDto.type();
                List<String> codes = tagDto.value();

                List<Tag> tags = tagRepository.findByTypeAndCodeIn(type, codes);

                if (tags.size() != codes.size()) {
                    throw new TagException(TagErrorCode.INVALID_TAG_CODE);
                }
                tags.forEach(group::addGroupTag);
            }
        }

        return GroupResponseDTO.UpdateResultDTO.builder()
                .groupId(group.getGroupId())
                .updatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();
    }

    //그룹삭제 service
    @Transactional
    public GroupResponseDTO.DeleteResultDTO deleteGroup(Long groupId, User host){

        //그룹 조회
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        //Host 권한 체크
        if (!group.getHost().getId().equals(host.getId())) {
            throw new GroupException(GroupErrorCode.MEMBER_NOT_HOST);
        }

        //진행중인 그룹 삭제 불가
        if(group.getGroupStatus() != GroupStatus.RECRUITING){
            throw new GroupException(GroupErrorCode.GROUP_CANT_DELETE);
        }

        List<Long> receiverIds = applicationRepository.findApplicantUserIdsByGroupId(groupId).stream()
                .filter(id -> !id.equals(host.getId()))
                .distinct()
                .toList();

        // 알림 publish
        publisher.publish(new GroupNotificationEvent(GROUP_DELETED, host.getId(), group.getBook().getTitle(), null, receiverIds, group.getGroupId()));

        //soft delete 실행
        group.markAsDELETED();

        return GroupResponseDTO.DeleteResultDTO.builder()
                .groupId(groupId)
                .deletedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();

    }

    //그룹조회
    @Transactional(readOnly = true)
    public GroupResponseDTO.GroupDetailDTO getGroupDetail(Long groupId, Long userId) {

        // 1. 그룹의 핵심 정보(도서, 호스트)를 한 번에 조회 (Fetch Join 활용으로 성능 최적화)
        Groups group = groupsRepository.findDetailById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        String persistedMeetPlace = null;
        if (group.getTradeType() == TradeType.DIRECT) {
            // findByGroup 대신 더 명확한 이름을 사용
            persistedMeetPlace = meetingRepository.findFirstByGroupOrderByCreatedAtDesc(group)
                    .map(Meeting::getMeetingPlace) // 엔티티 필드명이 meetingPlace이므로 정확함!
                    .orElse(null);
        }

        // 2. 해당 그룹에 참여가 확정된 멤버 리스트를 조회 (동그란 멤버 아이콘 리스트용)
        List<MatchedMember> matchedMembers = matchedMemberRepository.findAllByGroupOrderByReadingOrderAsc(group);

        // 3. 현재 '대기 중'인 신청자 수를 카운트 (방장 버튼의 숫자 표시 및 HOT 배지 계산용)
        int waitingCount = (int) applicationRepository.countByGroupGroupIdAndApplicationStatus(groupId, ApplicationStatus.PENDING);

        // 4. 대기 인원이 정원의 3배 이상일 경우 'HOT' 배지 활성화 여부 판단
        boolean isHot = waitingCount >= (group.getMaxCapacity() * 3);

        // 5. 기획서 UI에 맞춰 확정 멤버 정보와 빈 슬롯(EMPTY)을 혼합하여 참여자 목록 가공
        List<GroupResponseDTO.ParticipantSlotDTO> participantSlots = buildParticipantSlots(group, matchedMembers, userId);

        // 6. 조회자의 역할(방장/게스트)과 그룹 상태에 따라 하단에 노출될 버튼의 종류를 결정
        String buttonStatus = determineButtonStatus(group, userId, matchedMembers);

        List<String> groupTag = group.getGroupTags().stream().map(ut -> ut.getTag().getCode()).toList();


        // 7. 최종 DTO 조립 (엔티티 데이터를 화면 요구사항에 맞게 변환)
        return GroupResponseDTO.GroupDetailDTO.builder()
                .groupId(group.getGroupId())
                .title(group.getBook().getTitle())
                .groupComment(group.getGroupComment())
                .groupStatus(group.getGroupStatus().name())
                .isHost(group.getHost().getId().equals(userId))
                .preferRegion(group.getPreferRegion())
                .meetPlace(persistedMeetPlace)
                .bookTitle(group.getBook().getTitle())
                .bookImage(group.getBook().getImage())
                .author(group.getBook().getAuthor())
                .category(group.getBook().getCategory().label())
                .readingPeriod(group.getReadingPeriod())
                .matchedCount(matchedMembers.size())
                .maxCapacity(group.getMaxCapacity())
                .waitingCount(waitingCount)
                .isHot(isHot)
                .hostNickname(group.getHost().getNickName())
                .hostProfileImage(userProfileImageUrl(group.getHost()))
                .createdAt(group.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))) // 그룹생성일
                .groupTags(groupTag)
                .customTag(group.getCustomTag())
                .participantSlots(participantSlots)
                .buttonStatus(buttonStatus)
                .build();
    }

    private List<GroupResponseDTO.ParticipantSlotDTO> buildParticipantSlots(Groups group, List<MatchedMember> matchedMembers, Long userId) {
        List<GroupResponseDTO.ParticipantSlotDTO> slots = new ArrayList<>();

        for (MatchedMember mm : matchedMembers) {
            slots.add(GroupResponseDTO.ParticipantSlotDTO.builder()
                    .nickname(mm.getUser().getNickName())
                    .profileImage(userProfileImageUrl(mm.getUser()))
                    .role(mm.getRole().name())
                    .isMe(mm.getUser().getId().equals(userId))
                    .build());
        }

        int emptyCount = group.getMaxCapacity() - matchedMembers.size();
        for (int i = 0; i < emptyCount; i++) {
            slots.add(GroupResponseDTO.ParticipantSlotDTO.builder()
                    .role("EMPTY")
                    .isMe(false)
                    .build());
        }
        return slots;
    }

    private String userProfileImageUrl(User user) {
        if (user == null || user.getUserImage() == null) {
            return null;
        }
        return userImageS3Service.generatePresignedGetUrl(user.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
    }

    private String determineButtonStatus(Groups group, Long userId, List<MatchedMember> matchedMembers) {
        // 1. 방장인 경우
        if (group.getHost().getId().equals(userId)) {
            return group.getGroupStatus() == GroupStatus.RECRUITING ? "MANAGE" : "TRACKER";
        }

        // 2. 이미 참여 확정된 게스트인지 확인
        boolean isMatched = matchedMembers.stream()
                .anyMatch(mm -> mm.getUser().getId().equals(userId));
        if (isMatched) {
            return "TRACKER";
        }

        // 3. 신청 대기 중인지 확인
        if (applicationRepository.existsByGroupGroupIdAndGuestIdAndApplicationStatus(group.getGroupId(), userId, ApplicationStatus.PENDING)) {
            return "CANCEL";
        }

        // 4. 모집 완료 및 신청 가능 여부
        if (group.getGroupStatus() == GroupStatus.MATCHED) {
            return "FULL";
        }

        return "APPLY";
    }

    // 그룹 목록 조회 (필터링 + 추천순)
    @Transactional(readOnly = true)
    public GroupResponseDTO.GroupSliceResponseDTO getGroupList(User user, GroupRequestDTO.FilterDTO filter) {
        PageRequest pageable = PageRequest.of(filter.page(), filter.size());

        // 1. 추천 로직용 태그 ID 수집
        List<Long> userTagIds = (user != null) ?
                userTagRepository.findAllByUser(user).stream().map(ut -> ut.getTag().getId()).toList() : new ArrayList<>();

        // 2. 메인 그룹 리스트 조회 (1번 쿼리)
        Slice<Groups> groupsSlice = groupQueryRepository.findGroupsByFilters(filter, userTagIds, pageable);
        List<Long> groupIds = groupsSlice.getContent().stream().map(Groups::getGroupId).toList();

        if (groupIds.isEmpty()) {
            return new GroupResponseDTO.GroupSliceResponseDTO(new ArrayList<>(), 0, false);
        }

        // 3. [N+1 해결 1] 대기자 수 배치 조회 (2번 쿼리)
        Map<Long, Integer> waitingCountMap = applicationRepository.countPendingByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        // 4. [N+1 해결 2] 그룹별 태그 목록 배치 조회 (3번 쿼리)
        // yml을 못 만지므로 직접 In 절 쿼리로 태그를 땡겨옵니다.
        List<GroupTag> allGroupTags = groupTagRepository.findAllByGroupIdIn(groupIds);
        Map<Long, List<String>> tagListMap = allGroupTags.stream()
                .collect(Collectors.groupingBy(
                        gt -> gt.getGroup().getGroupId(),
                        Collectors.mapping(gt -> gt.getTag().getCode(), Collectors.toList())
                ));

        // 5. DTO 변환 (메모리상의 Map에서 데이터를 매핑)
        List<GroupResponseDTO.GroupSummaryDTO> dtoList = groupsSlice.stream()
                .map(group -> {
                    int waitingCount = waitingCountMap.getOrDefault(group.getGroupId(), 0);
                    List<String> tags = tagListMap.getOrDefault(group.getGroupId(), new ArrayList<>());
                    boolean isHot = waitingCount >= (group.getMaxCapacity() * 3);

                    return GroupResponseDTO.GroupSummaryDTO.builder()
                            .groupId(group.getGroupId())
                            .title(group.getBook().getTitle())
                            .author(group.getBook().getAuthor())
                            .genre(group.getBook().getCategory().label())
                            .hostProfileImage(userProfileImageUrl(group.getHost()))
                            .bookImage(group.getBook().getImage())
                            .hostNickname(group.getHost().getNickName())
                            .groupStatus(group.getGroupStatus().name())
                            .currentCount(group.getMatchedMember().size()) // matchedMember는 메인 쿼리에서 fetchJoin 권장
                            .maxCapacity(group.getMaxCapacity())
                            .waitingCount(waitingCount)
                            .isHot(isHot)
                            .groupType(group.getGroupType().name())
                            .tradeType(group.getTradeType().name())
                            .pictureBadge(determinePictureBadge(group))
                            .readingPeriod(group.getReadingPeriod())
                            .startDate(group.getStartDate() != null ? group.getStartDate().toString() : null)
                            .tags(tags) // 미리 수집한 태그 리스트 주입
                            .customTag(group.getCustomTag())
                            .build();
                }).toList();

        return new GroupResponseDTO.GroupSliceResponseDTO(dtoList, groupsSlice.getNumber(), groupsSlice.hasNext());
    }

    // 배지 텍스트 결정 로직
    private String determinePictureBadge(Groups group) {
        // '함께읽기' 타입이면 그대로 배지 노출
        if (group.getGroupType() == GroupType.TOGETHER) return "함께읽기";

        // '이어읽기' 중 '택배'면 택배 노출
        if (group.getTradeType() == TradeType.DELIVERY) return "택배";

        // '이어읽기' 중 '직접교환'이면 지역 정보 노출 (예: 서울 마포구 -> 마포구)
        String region = group.getPreferRegion();
        if (region == null || region.isBlank()) return "지역미정";

        String[] parts = region.split(" ");
        return parts[parts.length - 1];
    }

    //그룹검색
    @Transactional
    public GroupResponseDTO.SearchResultDTO searchGroups(GroupRequestDTO.SearchDTO request) {

        // 검색어 정규화 (trim 처리된 변수를 하나로 통일)
        String rawSearchWord = request.keyword();
        String cleanSearchWord = (rawSearchWord != null) ? rawSearchWord.trim() : null;

        // 1. 검색어 기록 (정제된 단어로 Redis 기록)
        if (cleanSearchWord != null && !cleanSearchWord.isBlank()) {
            redisUtil.incrementSearchScore(cleanSearchWord);
        }

        // 2. 페이징 설정
        PageRequest pageable = PageRequest.of(request.page(), request.size());

        // 3. 키워드 기반 통합 검색 실행
        org.springframework.data.domain.Page<Groups> searchResult = groupQueryRepository.searchGroupsByKeyword(
                cleanSearchWord, // 원본 대신 trim 사용
                request.sort(),
                pageable
        );

        List<Groups> content = searchResult.getContent();
        List<Long> groupIds = content.stream().map(Groups::getGroupId).toList();

        // 검색 결과가 없는 경우 빈 결과 반환
        if (groupIds.isEmpty()) {
            return new GroupResponseDTO.SearchResultDTO(new ArrayList<>(), 0L, request.page(), false);
        }

        // 3. [N+1 해결 1] 검색된 그룹들의 대기자 수(waitingCount) 일괄 조회
        Map<Long, Integer> waitingCountMap = applicationRepository.countPendingByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        // 4. [N+1 해결 2] 검색된 그룹들의 태그 목록 일괄 조회
        List<GroupTag> allGroupTags = groupTagRepository.findAllByGroupIdIn(groupIds);
        Map<Long, List<String>> tagListMap = allGroupTags.stream()
                .collect(Collectors.groupingBy(
                        gt -> gt.getGroup().getGroupId(),
                        Collectors.mapping(gt -> gt.getTag().getCode(), Collectors.toList())
                ));

        // 5. 엔티티 -> GroupSummaryDTO 변환 (기존 리스트 조회와 동일한 카드 포맷)
        List<GroupResponseDTO.GroupSummaryDTO> dtoList = content.stream()
                .map(group -> {
                    int waitingCount = waitingCountMap.getOrDefault(group.getGroupId(), 0);
                    List<String> tags = tagListMap.getOrDefault(group.getGroupId(), new ArrayList<>());
                    boolean isHot = waitingCount >= (group.getMaxCapacity() * 3);

                    return GroupResponseDTO.GroupSummaryDTO.builder()
                            .groupId(group.getGroupId())
                            .title(group.getBook().getTitle())
                            .bookImage(group.getBook().getImage())
                            .hostNickname(group.getHost().getNickName())
                            .groupStatus(group.getGroupStatus().name())
                            .currentCount(group.getMatchedMember().size())
                            .maxCapacity(group.getMaxCapacity())
                            .waitingCount(waitingCount)
                            .isHot(isHot)
                            .groupType(group.getGroupType().name())
                            .tradeType(group.getTradeType().name())
                            .pictureBadge(determinePictureBadge(group)) // 기존 배지 결정 로직 재사용
                            .tags(tags)
                            .customTag(group.getCustomTag())
                            .build();
                }).toList();

        // 6. 최종 검색 결과 DTO 조립 (총 건수 포함)
        return new GroupResponseDTO.SearchResultDTO(
                dtoList,
                searchResult.getTotalElements(), // 전체 결과 수 (Page 사용 이유)
                searchResult.getNumber(),        // 현재 페이지
                searchResult.hasNext()           // 다음 페이지 여부
        );
    }

    //인기검색어 상위 10개 조회
    @Transactional(readOnly = true)
    public List<String> getPopularKeywords() {
        return redisUtil.getTopKeywords(10);
    }

    // 신고할 그룹 조회
    @Transactional(readOnly = true)
    public List<GroupResponseDTO.GroupSummaryResponse> getGroupSummary(Long userId) {
        return matchedMemberQueryRepository.findGroupDtosByStatus(userId, GroupStatus.MATCHED);
    }

    // 신고할 그룹 멤버 조회
    @Transactional(readOnly = true)
    public List<GroupResponseDTO.GroupMemberResponse> getGroupMembers(Long groupId, Long userId) {
        // 현재 유저가 해당 그룹에 속해있는지 검증
        if (!matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId)) {
            throw new GroupException(GroupErrorCode.FORBIDDEN_GROUP_ACCESS);
        }

        // 현재 user를 제외한 나머지 멤버 조회
        return matchedMemberQueryRepository.findMemberDtosByGroupId(groupId, userId);
    }
}




