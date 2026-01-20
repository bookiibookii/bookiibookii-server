package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupService {

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    //book repo
    //private final BookRepositoy bookRepository;

    //그룹생성 service
    public GroupResponseDTO.CreateResultDTO createGroup(User host, GroupRequestDTO.CreateDTO request){
        // 1. 공통 정책 검증 (도서 필수, 날짜, 기간 체크)
        validateCommonPolicy(request);

        // 2. 도서 존재 여부 확인
        //Book book = bookRepository.findById(request.getBookId())
          //      .orElseThrow(() -> new GeneralException(ErrorStatus.BOOK_NOT_FOUND));

        // 3. 타입별 변수 설정 및 정책 검증
        Integer finalCapacity;
        TradeType finalTradeType;

        if (request.getGroupType() == GroupType.RELAY) {
            // [1:1 이어읽기] 방장 포함 2명 고정 및 위치 정보 검증
            validateRelayPolicy(host, request);
            finalCapacity = 2;
            finalTradeType = request.getTradeType();
        } else {
            // [1:N 함께읽기] 방장 포함 최대 8명 제한 및 기본 택배 설정
            validateTogetherPolicy(request);
            finalCapacity = request.getMaxCapacity();
            finalTradeType = TradeType.DELIVERY;
        }

        // 4. Groups 엔티티 빌드 (ID 기반 연관관계 매핑)
        Groups group = Groups.builder()
                //.book(book)
                .host(host)
                .maxCapacity(finalCapacity)
                .startDate(request.getStartDate())
                .readingPeriod(request.getReadingPeriod())
                .groupComment(request.getGroupComment())
                .groupType(request.getGroupType())
                .tradeType(finalTradeType)
                .groupStatus(GroupStatus.RECRUITING) // 초기 상태는 모집 중
                .build();

        // 5. 독서 태그 저장 로직
        //TODO

        Groups savedGroup = groupsRepository.save(group);

        // 방장을 MatchedMember의 첫 번째 멤버로 등록
        MatchedMember hostMember = MatchedMember.builder()
                .group(savedGroup)           // 엔티티의 private Groups group;
                .userId(host)                // 엔티티의 private User userId;
                .role(RoleStatus.HOST)       // 엔티티의 RoleStatus 타입 사용
                .readingOrder(1)             // 엔티티의 private Integer readingOrder;
                .build();

        matchedMemberRepository.save(hostMember);
        return GroupResponseDTO.CreateResultDTO.builder()
                .groupId(savedGroup.getGroupId()) //
                .groupStatus(savedGroup.getGroupStatus()) //
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")))
                .build();
    }

    private void validateCommonPolicy(GroupRequestDTO.CreateDTO request) {
        // 도서 선택 필수
        if (request.getBookId() == null) {
            throw new GroupException(GroupErrorCode.BOOK_NOT_SELECTED);
        }

        // 시작 날짜는 오늘 이후(내일부터) 선택 가능
        if (request.getStartDate() == null || !request.getStartDate().isAfter(LocalDateTime.now())) {
            throw new GroupException(GroupErrorCode.INVALID_START_DATE);
        }

        // 독서 기간 최소 3일 ~ 최대 30일
        if (request.getReadingPeriod() == null || request.getReadingPeriod() < 3 || request.getReadingPeriod() > 30) {
            throw new GroupException(GroupErrorCode.INVALID_READING_PERIOD);
        }
    }

    //1:1 relay 읽기 정책
    private void validateRelayPolicy(User host, GroupRequestDTO.CreateDTO request) {
        // 직접 교환 시 유저 엔티티의 지역/상세장소 정보 필수
        if (request.getTradeType() == TradeType.DIRECT) {
            if (host.getRegion() == null || host.getMeetPlace() == null) {
                throw new GroupException(GroupErrorCode.USER_LOCATION_NOT_FOUND);
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
}


