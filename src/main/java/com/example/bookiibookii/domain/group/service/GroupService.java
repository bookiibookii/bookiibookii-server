package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookCategoryMapper;
import com.example.bookiibookii.domain.book.service.BookService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupService {

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final BookService bookService;

    //그룹생성 service
    public GroupResponseDTO.CreateResultDTO createGroup(User host, GroupRequestDTO.CreateDTO request){
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
                .user(host)                // 엔티티의 private User userId;
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
        if (request.getIsbn13() == null || request.getIsbn13().isBlank()) {
            throw new GroupException(GroupErrorCode.BOOK_NOT_SELECTED);
        }

        // 시작 날짜는 오늘 이후(내일부터) 선택 가능
        if (request.getStartDate() == null || !request.getStartDate().isAfter(LocalDate.now())) {
            throw new GroupException(GroupErrorCode.INVALID_START_DATE);
        }

        // 독서 기간 최소 3일 ~ 최대 30일
        if (request.getReadingPeriod() == null || request.getReadingPeriod() < 3 || request.getReadingPeriod() > 30) {
            throw new GroupException(GroupErrorCode.INVALID_READING_PERIOD);
        }

        // 그룹 소개글 검증 로직
        if (request.getGroupComment() == null || request.getGroupComment().isBlank()) {
            throw new GroupException(GroupErrorCode.COMMENT_REQUIRED);
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

        // 택배 교환(DELIVERY) 시: 등록된 배송지(Address) 존재 여부 확인
        /*if (request.getTradeType() == TradeType.DELIVERY) {
            // addressRepository를 통해 해당 유저의 주소가 등록되어 있는지 확인
            boolean hasAddress = addressRepository.existsByUserId(host.getId());
            if (!hasAddress) {
                // "마이페이지에서 배송지를 먼저 등록해주세요." 에러 발생
                throw new GroupException(GroupErrorCode.ADDRESS_NOT_FOUND);
            }*/
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

        //독서 태그 수정
        //TO DO

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

        //soft delete 실행
        group.markAsDELETED();

        return GroupResponseDTO.DeleteResultDTO.builder()
                .groupId(groupId)
                .deletedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")))
                .build();

    }

}


