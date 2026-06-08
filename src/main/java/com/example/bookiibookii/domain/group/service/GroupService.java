package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.location.entity.UserDelivery;
import com.example.bookiibookii.domain.location.entity.UserExchange;
import com.example.bookiibookii.domain.location.repository.UserDeliveryRepository;
import com.example.bookiibookii.domain.location.repository.UserExchangeRepository;
import com.example.bookiibookii.domain.group.dto.RuleDTO;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.*;
import com.example.bookiibookii.domain.group.enums.*;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.*;
import com.example.bookiibookii.domain.group.util.HomeSeedUtil;
import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.service.KeywordMatchService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.Tag;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.service.BadWordService;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final KeywordMatchService keywordMatchService;
    private final DomainEventPublisher publisher;
    private final MatchedMemberQueryRepository matchedMemberQueryRepository;
    private final UserImageS3Service userImageS3Service;
    private final RedisUtil redisUtil;
    private final MeetingRepository meetingRepository;
    private final BadWordService badWordService;
    private final UserExchangeRepository userExchangeRepository;
    private final UserDeliveryRepository userDeliveryRepository;
    private final GroupPlaceRepository groupPlaceRepository;

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;
    private static final Set<Tag> READING_STYLE_TAGS = Set.of(Tag.MEMO, Tag.POSTIT, Tag.PHOTO, Tag.All_ROUNDER);

    // 그룹 홈 화면 섹션별 노출 개수
    private static final int HOME_GROUP_LIMIT = 5;
    private static final int HOME_POPULAR_BOOK_LIMIT = 5;
    private static final int HOME_BESTSELLER_BOOK_LIMIT = 3;

    //그룹생성 service
    public GroupResponseDTO.CreateResultDTO createGroup(User host, GroupRequestDTO.CreateDTO request){

        //로그인 여부 검증
        if(host == null){
            throw new UserException(UserErrorCode.NOT_FOUND);
        }

        // RECRUITING 이거나 MATCHED인 그룹만 찾기
        List<GroupStatus> activeStatuses = List.of(GroupStatus.RECRUITING, GroupStatus.MATCHED);

        //활성화 된 그룹개수 카운트
        long hostingCount = groupsRepository.countByHostIdAndGroupStatusIn(host.getId(), activeStatuses);

        // 카운트 개수 3개 이상이면 그룹생성 제한
        // 데모데이 테스트용으로 최대 개수 300개로 수정
        if (hostingCount >= 300) {
            throw new GroupException(GroupErrorCode.HOST_MAX_LIMIT_EXCEEDED);
        }

        // 1. 공통 정책 검증 (도서 필수, 날짜, 기간 체크)
        validateCommonPolicy(request);


        // 2. 도서 존재 여부 확인
        Book book = bookService.getOrCreateByIsbn13(request.getIsbn13());

        // 3. 정책 검증
        validatePolicy(host, request);

        // 4. Groups 엔티티 빌드
        Groups group = Groups.builder()
                .book(book)
                .host(host)
                .maxCapacity(2)
                .readingPeriod(request.getReadingPeriod())
                .groupComment(request.getGroupComment())
                .groupType(GroupType.RELAY)
                .tradeType(request.getTradeType())
                .groupStatus(GroupStatus.RECRUITING)
                .groupName(request.getGroupName())
                .build();

        Groups savedGroup = groupsRepository.save(group);
        GroupPlace groupPlace = createGroupPlace(savedGroup, host, request);
        group.setGroupPlace(groupPlace);
        groupPlaceRepository.save(groupPlace);

        // 규칙 저장 (모든 트레이드 타입 공통)
        request.getRules().forEach(rule ->
                savedGroup.getGroupRules().add(GroupRule.create(savedGroup, resolveRuleContent(rule), rule.tag())));

        // 방장을 MatchedMember의 첫 번째 멤버로 등록
        MatchedMember hostMember = MatchedMember.builder()
                .group(savedGroup)
                .user(host)
                .role(RoleStatus.HOST)
                // .currentReadingRate(0)
                .build();

        matchedMemberRepository.save(hostMember);

        List<Keyword> matched = keywordMatchService.matchForBook(book.getTitle(), book.getAuthor());

        List<Long> ids = matched.stream().map(Keyword::getId).toList();
        List<String> texts = matched.stream().map(Keyword::getContent).toList();

        publisher.publish(new KeywordGroupCreatedEvent(group.getId(), texts, ids));

        return GroupResponseDTO.CreateResultDTO.builder()
                .groupId(savedGroup.getId()) //
                .groupStatus(savedGroup.getGroupStatus()) //
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .build();
    }

    private void validateCommonPolicy(GroupRequestDTO.CreateDTO request) {
        if (request.getIsbn13() == null || request.getIsbn13().isBlank()) {
            throw new GroupException(GroupErrorCode.BOOK_NOT_SELECTED);
        }

        if (request.getGroupName() == null || request.getGroupName().isBlank()) {
            throw new GroupException(GroupErrorCode.GROUP_NAME_REQUIRED);
        }

        if (request.getReadingPeriod() == null || !List.of(3, 7, 14, 21, 28).contains(request.getReadingPeriod())) {
            throw new GroupException(GroupErrorCode.INVALID_READING_PERIOD);
        }

        if (request.getTradeType() == null) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_TYPE);
        }

        validateGroupPlaceSelection(request);

        // 소개글 선택 입력 — 값이 있을 때만 검증
        if (request.getGroupComment() != null && !request.getGroupComment().isBlank()) {
            if (request.getGroupComment().length() > 500) {
                throw new GroupException(GroupErrorCode.INTRODUCTION_TOO_LONG);
            }
            if (badWordService.containsBadWord(request.getGroupComment())) {
                throw new GroupException(GroupErrorCode.FORBIDDEN_WORD_INCLUDED);
            }
        }
    }

    private void validateGroupPlaceSelection(GroupRequestDTO.CreateDTO request) {
        if (request.getTradeType() == TradeType.DIRECT) {
            if (request.getUserDeliveryId() != null) {
                throw new GroupException(GroupErrorCode.USER_DELIVERY_ID_NOT_ALLOWED_FOR_DIRECT);
            }
            if (request.getUserExchangeId() == null) {
                throw new GroupException(GroupErrorCode.USER_EXCHANGE_ID_REQUIRED);
            }
            return;
        }

        if (request.getTradeType() == TradeType.DELIVERY) {
            if (request.getUserExchangeId() != null) {
                throw new GroupException(GroupErrorCode.USER_EXCHANGE_ID_NOT_ALLOWED_FOR_DELIVERY);
            }
            if (request.getUserDeliveryId() == null) {
                throw new GroupException(GroupErrorCode.USER_DELIVERY_ID_REQUIRED);
            }
            return;
        }

        throw new GroupException(GroupErrorCode.INVALID_GROUP_SELECTED_PLACE);
    }

    private void validatePolicy(User host, GroupRequestDTO.CreateDTO request) {
        // 규칙 검증 (모든 트레이드 타입 공통)
        validateRules(request.getRules());
    }

    private GroupPlace createGroupPlace(Groups group, User host, GroupRequestDTO.CreateDTO request) {
        if (request.getTradeType() == TradeType.DELIVERY) {
            UserDelivery userDelivery = userDeliveryRepository
                    .findById(request.getUserDeliveryId())
                    .orElseThrow(() -> new GroupException(GroupErrorCode.DELIVERY_ADDRESS_NOT_FOUND));

            if (!userDelivery.getUser().getId().equals(host.getId())) {
                throw new GroupException(GroupErrorCode.NOT_MY_DELIVERY_ADDRESS);
            }

            GroupPlace groupPlace = GroupPlace.builder()
                    .group(group)
                    .sourceType(GroupPlaceSourceType.USER_DELIVERY)
                    .placeName(userDelivery.getLocation().getPlaceName())
                    .address(userDelivery.getLocation().getAddress())
                    .zipCode(userDelivery.getLocation().getZipCode())
                    .x(userDelivery.getLocation().getX())
                    .y(userDelivery.getLocation().getY())
                    .addressDetail(userDelivery.getAddressDetail())
                    .receiverName(userDelivery.getReceiverName())
                    .phoneNumber(userDelivery.getPhone())
                    .build();
            validateDeliveryGroupPlace(groupPlace);
            return groupPlace;
        }

        if (request.getTradeType() == TradeType.DIRECT) {
            UserExchange userExchange = userExchangeRepository
                    .findById(request.getUserExchangeId())
                    .orElseThrow(() -> new GroupException(GroupErrorCode.DIRECT_EXCHANGE_PLACE_NOT_FOUND));

            if (!userExchange.getUser().getId().equals(host.getId())) {
                throw new GroupException(GroupErrorCode.NOT_MY_EXCHANGE_PLACE);
            }

            GroupPlace groupPlace = GroupPlace.builder()
                    .group(group)
                    .sourceType(GroupPlaceSourceType.USER_EXCHANGE)
                    .placeName(userExchange.getLocation().getPlaceName())
                    .address(userExchange.getLocation().getAddress())
                    .zipCode(userExchange.getLocation().getZipCode())
                    .x(userExchange.getLocation().getX())
                    .y(userExchange.getLocation().getY())
                    .addressDetail(userExchange.getAddressDetail())
                    .build();
            validateDirectGroupPlace(groupPlace);
            return groupPlace;
        }

        throw new GroupException(GroupErrorCode.INVALID_GROUP_SELECTED_PLACE);
    }

    private void validateDeliveryGroupPlace(GroupPlace groupPlace) {
        if (groupPlace.getSourceType() != GroupPlaceSourceType.USER_DELIVERY
                || isBlank(groupPlace.getReceiverName())
                || isBlank(groupPlace.getPhoneNumber())
                || isBlank(groupPlace.getAddress())
                || isBlank(groupPlace.getZipCode())) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_SELECTED_PLACE);
        }
    }

    private void validateDirectGroupPlace(GroupPlace groupPlace) {
        if (groupPlace.getSourceType() != GroupPlaceSourceType.USER_EXCHANGE
                || isBlank(groupPlace.getPlaceName())
                || isBlank(groupPlace.getAddress())
                || groupPlace.getX() == null
                || groupPlace.getY() == null) {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_SELECTED_PLACE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateRules(List<RuleDTO> rules) {
        if (rules == null || rules.isEmpty() || rules.size() > 5) {
            throw new GroupException(GroupErrorCode.INVALID_RULES);
        }

        boolean hasReadingStyleTag = false;
        for (RuleDTO rule : rules) {
            if (rule == null || rule.tag() == null || rule.tag() == Tag.NO_IDEA) {
                throw new GroupException(GroupErrorCode.INVALID_RULES);
            }
            if (READING_STYLE_TAGS.contains(rule.tag())) {
                hasReadingStyleTag = true;
            }
            validateRule(rule);
        }
        if (!hasReadingStyleTag) {
            throw new GroupException(GroupErrorCode.READING_STYLE_TAG_REQUIRED);
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

        if (request.getReadingPeriod() != null) {
            if (!List.of(3, 7, 14, 21, 28).contains(request.getReadingPeriod())) {
                throw new GroupException(GroupErrorCode.INVALID_READING_PERIOD);
            }
            group.setReadingPeriod(request.getReadingPeriod());
        }

        //그룹 소개글 수정
        if(request.getGroupComment() != null){
            if (request.getGroupComment().length() > 500) {
                throw new GroupException(GroupErrorCode.INTRODUCTION_TOO_LONG);
            }
            if (badWordService.containsBadWord(request.getGroupComment())) {
                throw new GroupException(GroupErrorCode.FORBIDDEN_WORD_INCLUDED);
            }
            group.setGroupComment(request.getGroupComment());
        }

        // 그룹명 수정
        if (request.getGroupName() != null) {
            group.setGroupName(request.getGroupName());
        }

        // 규칙 수정
        if (request.getRules() != null) {
            validateRules(request.getRules());
            group.getGroupRules().clear();
            request.getRules().forEach(rule ->
                    group.getGroupRules().add(GroupRule.create(group, resolveRuleContent(rule), rule.tag())));
        }

        return GroupResponseDTO.UpdateResultDTO.builder()
                .groupId(group.getId())
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
        publisher.publish(new GroupNotificationEvent(GROUP_DELETED, host.getId(), group.getBook().getTitle(), null, receiverIds, group.getId()));

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

        Groups group = groupsRepository.findDetailById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 2. 해당 그룹에 참여가 확정된 멤버 리스트를 조회 (동그란 멤버 아이콘 리스트용)
        List<MatchedMember> matchedMembers = matchedMemberRepository.findAllByGroupOrderByCreatedAtAsc(group);
        int waitingCount = (int) applicationRepository.countByGroupIdAndApplicationStatus(groupId, ApplicationStatus.PENDING);

        // 4. 대기 인원이 정원의 3배 이상일 경우 'HOT' 배지 활성화 여부 판단
        boolean isHot = waitingCount >= (group.getMaxCapacity() * 3);

        // 5. 기획서 UI에 맞춰 확정 멤버 정보와 빈 슬롯(EMPTY)을 혼합하여 참여자 목록 가공
        List<GroupResponseDTO.ParticipantSlotDTO> participantSlots = buildParticipantSlots(group, matchedMembers, userId);

        // 6. 조회자의 역할(방장/게스트)과 그룹 상태에 따라 하단에 노출될 버튼의 종류를 결정
        String buttonStatus = determineButtonStatus(group, userId, matchedMembers);


        // 7. 최종 DTO 조립 (엔티티 데이터를 화면 요구사항에 맞게 변환)
        return GroupResponseDTO.GroupDetailDTO.builder()
                .groupId(group.getId())
                .groupComment(group.getGroupComment())
                .groupStatus(group.getGroupStatus().name())
                .isHost(group.getHost().getId().equals(userId))
                .tradeType(group.getTradeType().name())
                .placeName(group.getGroupPlace() != null ? group.getGroupPlace().getPlaceName() : null)
                .address(group.getGroupPlace() != null ? group.getGroupPlace().getAddress() : null)
                .title(group.getBook().getTitle())
                .bookImage(group.getBook().getImage())
                .author(group.getBook().getAuthor())
                .genre(group.getBook().getCategory().getLabel())
                .readingPeriod(group.getReadingPeriod())
                .matchedCount(matchedMembers.size())
                .maxCapacity(group.getMaxCapacity())
                .waitingCount(waitingCount)
                .isHot(isHot)
                .hostNickname(group.getHost().getNickName())
                .hostProfileImageUrl(userProfileImageUrl(group.getHost()))
                .createdAt(group.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))) // 그룹생성일
                .startDate(group.getStartDate() != null ? group.getStartDate().toString() : null)
                .participantSlots(participantSlots)
                .buttonStatus(buttonStatus)
                .groupName(group.getGroupName())
                .rules(group.getGroupRules().stream()
                        .map(r -> new RuleDTO(r.getTag(), r.getRuleContent()))
                        .toList())
                .build();
    }

    private List<GroupResponseDTO.ParticipantSlotDTO> buildParticipantSlots(Groups group, List<MatchedMember> matchedMembers, Long userId) {
        List<GroupResponseDTO.ParticipantSlotDTO> slots = new ArrayList<>();

        for (MatchedMember mm : matchedMembers) {
            slots.add(GroupResponseDTO.ParticipantSlotDTO.builder()
                    .nickname(mm.getUser().getNickName())
                    .profileImageUrl(userProfileImageUrl(mm.getUser()))
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
        if (applicationRepository.existsByGroupIdAndGuestIdAndApplicationStatus(group.getId(), userId, ApplicationStatus.PENDING)) {
            return "CANCEL";
        }

        // 4. 모집 완료 및 신청 가능 여부
        if (group.getGroupStatus() == GroupStatus.MATCHED) {
            return "FULL";
        }

        return "APPLY";
    }

    private void validateRule(RuleDTO rule) {
        if (rule.tag() == Tag.CUSTOM) {
            if (rule.content() == null || rule.content().isBlank()) {
                throw new GroupException(GroupErrorCode.INVALID_RULES);
            }
            if (badWordService.containsBadWord(rule.content())) {
                throw new GroupException(GroupErrorCode.FORBIDDEN_WORD_INCLUDED);
            }
        }
    }

    private String resolveRuleContent(RuleDTO rule) {
        if (rule.tag() == Tag.CUSTOM) {
            return rule.content();
        }
        return rule.tag().getDefaultContent();
    }

    // 그룹 목록 조회 (필터링 + 추천순)
    @Transactional(readOnly = true)
    public GroupResponseDTO.GroupSliceResponseDTO getGroupList(User user, GroupRequestDTO.FilterDTO filter) {
        PageRequest pageable = PageRequest.of(filter.page(), filter.size());

        long totalCount = groupQueryRepository.countGroupsByFilters(filter);

        // 2. 메인 그룹 리스트 조회 (1번 쿼리)
        Slice<Groups> groupsSlice = groupQueryRepository.findGroupsByFilters(filter, pageable);
        List<Long> groupIds = groupsSlice.getContent().stream().map(Groups::getId).toList();

        if (groupIds.isEmpty()) {
            return new GroupResponseDTO.GroupSliceResponseDTO(new ArrayList<>(), totalCount, groupsSlice.getNumber(), false);
        }

        // 3. [N+1 해결 1] 대기자 수 배치 조회 (2번 쿼리)
        Map<Long, Integer> waitingCountMap = applicationRepository.countPendingByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        // 5. DTO 변환 (메모리상의 Map에서 데이터를 매핑)
        List<GroupResponseDTO.GroupSummaryDTO> dtoList = groupsSlice.stream()
                .map(group -> {
                    int waitingCount = waitingCountMap.getOrDefault(group.getId(), 0);
                    boolean isHot = waitingCount >= (group.getMaxCapacity() * 3);

                    return GroupResponseDTO.GroupSummaryDTO.builder()
                            .groupId(group.getId())
                            .groupName(group.getGroupName())
                            .title(group.getBook().getTitle())
                            .author(group.getBook().getAuthor())
                            .genre(group.getBook().getCategory().getLabel())
                            .hostProfileImageUrl(userProfileImageUrl(group.getHost()))
                            .bookImage(group.getBook().getImage())
                            .hostNickname(group.getHost().getNickName())
                            .groupStatus(group.getGroupStatus().name())
                            .currentCount(group.getMatchedMember().size())
                            .maxCapacity(group.getMaxCapacity())
                            .waitingCount(waitingCount)
                            .isHot(isHot)
                            .tradeType(group.getTradeType().name())
                            .pictureBadge(determinePictureBadge(group))
                            .readingPeriod(group.getReadingPeriod())
                            .build();
                }).toList();

        return new GroupResponseDTO.GroupSliceResponseDTO(dtoList, totalCount, groupsSlice.getNumber(), groupsSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public List<GroupResponseDTO.MyHostedGroupDTO> getMyHostedGroups(User user) {
        if (user == null) {
            throw new UserException(UserErrorCode.NOT_FOUND);
        }

        return groupsRepository.findMyHostedGroups(user.getId(), GroupStatus.DELETED).stream()
                .map(this::toMyHostedGroup)
                .toList();
    }

    private GroupResponseDTO.MyHostedGroupDTO toMyHostedGroup(Groups group) {
        Book book = group.getBook();
        User host = group.getHost();

        return GroupResponseDTO.MyHostedGroupDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .groupType(group.getGroupType().name())
                .tradeType(group.getTradeType().name())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .author(book.getAuthor())
                .bookCoverImageUrl(book.getImage())
                .readingPeriod(group.getReadingPeriod())
                .hostId(host.getId())
                .hostNickname(host.getNickName())
                .hostProfileImageUrl(userProfileImageUrl(host))
                .displayStatus(resolveHostedGroupDisplayStatus(group.getGroupStatus()))
                .build();
    }

    private HostedGroupDisplayStatus resolveHostedGroupDisplayStatus(GroupStatus groupStatus) {
        return switch (groupStatus) {
            case RECRUITING -> HostedGroupDisplayStatus.BEFORE_MATCHING;
            case MATCHED -> HostedGroupDisplayStatus.IN_PROGRESS;
            case COMPLETED -> HostedGroupDisplayStatus.COMPLETED;
            case DELETED -> throw new GroupException(GroupErrorCode.GROUP_NOT_FOUND);
        };
    }

    private String determinePictureBadge(Groups group) {
        return group.getTradeType() == TradeType.DELIVERY ? "택배" : "직접";
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
        List<Long> groupIds = content.stream().map(Groups::getId).toList();

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

        // 5. 엔티티 -> GroupSummaryDTO 변환 (기존 리스트 조회와 동일한 카드 포맷)
        List<GroupResponseDTO.GroupSummaryDTO> dtoList = content.stream()
                .map(group -> {
                    int waitingCount = waitingCountMap.getOrDefault(group.getId(), 0);
                    boolean isHot = waitingCount >= (group.getMaxCapacity() * 3);

                    return GroupResponseDTO.GroupSummaryDTO.builder()
                            .groupId(group.getId())
                            .groupName(group.getGroupName())
                            .title(group.getBook().getTitle())
                            .author(group.getBook().getAuthor())
                            .genre(group.getBook().getCategory().getLabel())
                            .bookImage(group.getBook().getImage())
                            .hostNickname(group.getHost().getNickName())
                            .hostProfileImageUrl(userProfileImageUrl(group.getHost()))
                            .groupStatus(group.getGroupStatus().name())
                            .currentCount(group.getMatchedMember().size())
                            .maxCapacity(group.getMaxCapacity())
                            .waitingCount(waitingCount)
                            .isHot(isHot)
                            .tradeType(group.getTradeType().name())
                            .readingPeriod(group.getReadingPeriod())
                            .pictureBadge(determinePictureBadge(group))
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
        if (!matchedMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new GroupException(GroupErrorCode.FORBIDDEN_GROUP_ACCESS);
        }

        // 현재 user를 제외한 나머지 멤버 조회
        return matchedMemberQueryRepository.findMemberDtosByGroupId(groupId, userId);
    }

    // ===== 그룹 홈 화면 조회 =====
    @Transactional(readOnly = true)
    public GroupResponseDTO.HomeResponseDTO getHome(User user) {
        if (user == null) {
            throw new UserException(UserErrorCode.NOT_FOUND);
        }

        Long userId = user.getId();
        List<GroupResponseDTO.HomeSectionDTO> sections = new ArrayList<>(7);

        addIfPresent(sections, buildNewGroupsSection(userId));
        sections.add(buildPopularBooksSection(userId));
        addIfPresent(sections, buildGenreSection(userId));
        sections.add(buildBestsellerBooksSection());
        addIfPresent(sections, buildClassicGroupsSection(userId));
        addIfPresent(sections, buildPackageGroupsSection(userId));
        addIfPresent(sections, buildNearbyDirectGroupsSection(userId));

        return GroupResponseDTO.HomeResponseDTO.builder().sections(sections).build();
    }

    private GroupResponseDTO.HomeSectionDTO buildNewGroupsSection(Long userId) {
        List<GroupResponseDTO.HomeGroupCardDTO> items = toHomeCards(
                groupQueryRepository.findRecentGroups(
                        userId,
                        HomeSeedUtil.twentyFourHoursAgoKst(),
                        HOME_GROUP_LIMIT
                )
        );
        return groupSection(
                HomeSectionType.NEW_GROUPS,
                "신규 그룹을 확인해보세요",
                "오늘 만들어진 따끈따끈한 그룹들만 모았어요",
                items
        );
    }

    private GroupResponseDTO.HomeSectionDTO buildPopularBooksSection(Long userId) {
        List<GroupResponseDTO.HomeBookThumbnailDTO> items = toBookThumbnails(
                groupQueryRepository.findPopularBooks(userId, HOME_POPULAR_BOOK_LIMIT)
        );
        return bookSection(
                HomeSectionType.POPULAR_BOOKS_TOP5,
                "인기 도서 TOP 5",
                "지금 바로 부키부키에서 핫한 도서를 확인해보세요",
                HomeLayoutType.BOOK_THUMBNAIL_CAROUSEL,
                items
        );
    }

    private GroupResponseDTO.HomeSectionDTO buildGenreSection(Long userId) {
        List<HomeGenre> candidates = groupQueryRepository
                .findCategoriesWithRecruitingGroups(userId)
                .stream()
                .map(HomeGenre::from)
                .flatMap(java.util.Optional::stream)
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        HomeGenre picked = deterministicPick(
                candidates,
                HomeSeedUtil.currentSeedKey()
        );
        List<GroupResponseDTO.HomeGroupCardDTO> items = toHomeCards(
                groupQueryRepository.findGroupsByCategories(
                        userId,
                        picked.getCategories(),
                        HOME_GROUP_LIMIT
                )
        );
        return groupSection(
                HomeSectionType.RANDOM_GENRE_GROUPS,
                picked.getLabel() + " 도서와 함께해볼까요?",
                "평소 자주 읽지 않았던 장르여도 도전해보세요",
                items
        );
    }

    private GroupResponseDTO.HomeSectionDTO buildBestsellerBooksSection() {
        List<GroupResponseDTO.HomeBookThumbnailDTO> items = toBestsellerBookThumbnails(
                groupQueryRepository.findBestsellerBooks(HOME_BESTSELLER_BOOK_LIMIT)
        );
        return bookSection(
                HomeSectionType.BESTSELLER_BOOKS,
                "나 빼고 다 읽은 책 여기 있어요",
                "이번 기회에 베스트셀러 읽어볼까요?",
                HomeLayoutType.BOOK_THUMBNAIL_GRID,
                items
        );
    }

    private GroupResponseDTO.HomeSectionDTO buildClassicGroupsSection(Long userId) {
        List<GroupResponseDTO.HomeGroupCardDTO> items = toHomeCards(
                groupQueryRepository.findClassicGroups(
                        userId,
                        HomeCandidateSectionType.CLASSIC_BOOK_GROUP,
                        HOME_GROUP_LIMIT
                )
        );
        return groupSection(
                HomeSectionType.CLASSIC_GROUPS,
                "고전, 한 번쯤은 읽어야죠",
                "파트너와 함께라면 더 재미있을 거예요",
                items
        );
    }

    private GroupResponseDTO.HomeSectionDTO buildPackageGroupsSection(Long userId) {
        List<GroupResponseDTO.HomeGroupCardDTO> items = toHomeCards(
                groupQueryRepository.findGroupsByTradeType(
                        userId,
                        TradeType.DELIVERY,
                        HOME_GROUP_LIMIT
                )
        );
        return groupSection(
                HomeSectionType.PACKAGE_GROUPS,
                "택배로 책을 안전하게 교환해요",
                "전국 어디에 있어도 책을 교환할 수 있어요",
                items
        );
    }

    private GroupResponseDTO.HomeSectionDTO buildNearbyDirectGroupsSection(Long userId) {
        List<UserExchange> exchanges = userExchangeRepository
                .findByUserIdWithLocation(userId)
                .stream()
                .filter(exchange -> exchange.getLocation().getX() != null)
                .filter(exchange -> exchange.getLocation().getY() != null)
                .filter(distinctByCoordinate())
                .filter(exchange -> groupQueryRepository.existsDirectGroupsAtCoordinate(
                        userId,
                        exchange.getLocation().getX(),
                        exchange.getLocation().getY()
                ))
                .limit(2)
                .toList();
        if (exchanges.isEmpty()) {
            return null;
        }

        UserExchange picked = deterministicPick(
                exchanges,
                HomeSeedUtil.userSeedKey(userId, HomeSeedUtil.currentSeedKey())
        );
        List<GroupResponseDTO.HomeGroupCardDTO> items = toHomeCards(
                groupQueryRepository.findDirectGroupsAtCoordinate(
                        userId,
                        picked.getLocation().getX(),
                        picked.getLocation().getY(),
                        HOME_GROUP_LIMIT
                )
        );
        return groupSection(
                HomeSectionType.NEARBY_DIRECT_GROUPS,
                "근처에서 직접 만나 교환해요",
                "",
                items
        );
    }

    private <T> T deterministicPick(List<T> candidates, String seedKey) {
        return candidates.get(HomeSeedUtil.pickIndex(seedKey, candidates.size()));
    }

    private java.util.function.Predicate<UserExchange> distinctByCoordinate() {
        Set<String> seen = new java.util.HashSet<>();
        return exchange -> seen.add(
                coordinateKey(
                        exchange.getLocation().getX(),
                        exchange.getLocation().getY()
                )
        );
    }

    private String coordinateKey(BigDecimal x, BigDecimal y) {
        return x.stripTrailingZeros().toPlainString()
                + ":"
                + y.stripTrailingZeros().toPlainString();
    }

    private List<GroupResponseDTO.HomeGroupCardDTO> toHomeCards(
            List<Groups> groups
    ) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        return groups.stream().map(this::toHomeCard).toList();
    }

    private List<GroupResponseDTO.HomeBookThumbnailDTO> toBookThumbnails(
            List<GroupQueryRepository.HomeBookProjection> books
    ) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        List<GroupResponseDTO.HomeBookThumbnailDTO> items =
                new ArrayList<>(books.size());
        for (int i = 0; i < books.size(); i++) {
            GroupQueryRepository.HomeBookProjection book = books.get(i);
            items.add(GroupResponseDTO.HomeBookThumbnailDTO.builder()
                    .isbn13(book.isbn13())
                    .title(book.title())
                    .author(book.author())
                    .bookImage(book.image())
                    .searchKeyword(book.title())
                    .rank(i + 1)
                    .build());
        }
        return items;
    }

    private List<GroupResponseDTO.HomeBookThumbnailDTO> toBestsellerBookThumbnails(
            List<GroupQueryRepository.HomeBestsellerBookProjection> books
    ) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }
        return books.stream()
                .filter(book -> !isBlank(book.isbn13()))
                .filter(book -> !isBlank(book.title()))
                .map(book -> GroupResponseDTO.HomeBookThumbnailDTO.builder()
                        .isbn13(book.isbn13())
                        .title(book.title())
                        .author(book.author())
                        .bookImage(book.image())
                        .searchKeyword(book.title())
                        .rank(book.ranking())
                        .build())
                .toList();
    }

    private GroupResponseDTO.HomeSectionDTO groupSection(
            HomeSectionType sectionType,
            String title,
            String subtitle,
            List<GroupResponseDTO.HomeGroupCardDTO> items
    ) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return GroupResponseDTO.HomeSectionDTO.builder()
                .sectionType(sectionType)
                .title(title)
                .subtitle(subtitle)
                .layoutType(HomeLayoutType.GROUP_CARD_CAROUSEL)
                .items(items)
                .build();
    }

    private GroupResponseDTO.HomeSectionDTO bookSection(
            HomeSectionType sectionType,
            String title,
            String subtitle,
            HomeLayoutType layoutType,
            List<GroupResponseDTO.HomeBookThumbnailDTO> items
    ) {
        return GroupResponseDTO.HomeSectionDTO.builder()
                .sectionType(sectionType)
                .title(title)
                .subtitle(subtitle)
                .layoutType(layoutType)
                .items(items == null ? List.of() : items)
                .build();
    }

    private void addIfPresent(
            List<GroupResponseDTO.HomeSectionDTO> sections,
            GroupResponseDTO.HomeSectionDTO section
    ) {
        if (section != null) {
            sections.add(section);
        }
    }

    private GroupResponseDTO.HomeGroupCardDTO toHomeCard(Groups group) {
        return GroupResponseDTO.HomeGroupCardDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .hostNickname(group.getHost().getNickName())
                .hostProfileImageUrl(userProfileImageUrl(group.getHost()))
                .bookImage(group.getBook().getImage())
                .bookTitle(group.getBook().getTitle())
                .author(group.getBook().getAuthor())
                .readingPeriod(group.getReadingPeriod())
                .build();
    }
}
