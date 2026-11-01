package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.GroupPlace;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.HomeCandidateSectionType;
import com.example.bookiibookii.domain.group.enums.HomeSectionType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupPlaceRepository;
import com.example.bookiibookii.domain.group.repository.GroupQueryRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberQueryRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.entity.UserExchange;
import com.example.bookiibookii.domain.location.repository.UserDeliveryRepository;
import com.example.bookiibookii.domain.location.repository.UserExchangeRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.service.KeywordMatchService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserImage;
import com.example.bookiibookii.domain.user.service.BadWordService;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.global.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceHomeTest {

    @Mock private GroupsRepository groupsRepository;
    @Mock private MatchedMemberRepository matchedMemberRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private BookService bookService;
    @Mock private GroupQueryRepository groupQueryRepository;
    @Mock private KeywordMatchService keywordMatchService;
    @Mock private DomainEventPublisher publisher;
    @Mock private MatchedMemberQueryRepository matchedMemberQueryRepository;
    @Mock private UserImageS3Service userImageS3Service;
    @Mock private RedisUtil redisUtil;
    @Mock private MeetingRepository meetingRepository;
    @Mock private BadWordService badWordService;
    @Mock private UserExchangeRepository userExchangeRepository;
    @Mock private UserDeliveryRepository userDeliveryRepository;
    @Mock private GroupPlaceRepository groupPlaceRepository;

    @InjectMocks
    private GroupService groupService;

    private final User user = User.builder().id(1L).nickName("viewer").build();

    @BeforeEach
    void setUpEmptyHomeData() {
        lenient().when(groupQueryRepository.findRecentGroups(any(), any(Instant.class), anyInt()))
                .thenReturn(List.of());
        lenient().when(groupQueryRepository.findPopularBooks(any(), anyInt()))
                .thenReturn(List.of());
        lenient().when(groupQueryRepository.findCategoriesWithRecruitingGroups(any()))
                .thenReturn(List.of());
        lenient().when(groupQueryRepository.findBestsellerBooks(anyInt()))
                .thenReturn(List.of());
        lenient().when(groupQueryRepository.findClassicGroups(any(), any(), anyInt()))
                .thenReturn(List.of());
        lenient().when(groupQueryRepository.findGroupsByTradeType(any(), any(), anyInt()))
                .thenReturn(List.of());
        lenient().when(userExchangeRepository.findByUserIdWithLocation(any()))
                .thenReturn(List.of());
    }

    @Test
    void exposesNewGroupsWhenRecentOpenGroupExists() {
        when(groupQueryRepository.findRecentGroups(eq(1L), any(Instant.class), eq(5)))
                .thenReturn(List.of(group(10L, CustomCategory.KOREAN_NOVEL)));
        Instant beforeCall = Instant.now()
                .minus(java.time.Duration.ofHours(24))
                .minus(java.time.Duration.ofMinutes(1));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(sectionTypes(response)).contains(HomeSectionType.NEW_GROUPS);
        assertThat(section(response, HomeSectionType.NEW_GROUPS).items()).hasSize(1);
        ArgumentCaptor<Instant> cutoff =
                ArgumentCaptor.forClass(Instant.class);
        verify(groupQueryRepository).findRecentGroups(eq(1L), cutoff.capture(), eq(5));
        assertThat(cutoff.getValue()).isAfter(beforeCall);
    }

    @Test
    void groupCarouselItemIncludesTradeTypeAndGenre() {
        when(groupQueryRepository.findRecentGroups(eq(1L), any(Instant.class), eq(5)))
                .thenReturn(List.of(group(11L, CustomCategory.KOREAN_NOVEL)));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        GroupResponseDTO.HomeGroupCardDTO item = (GroupResponseDTO.HomeGroupCardDTO)
                section(response, HomeSectionType.NEW_GROUPS).items().get(0);
        assertThat(item.tradeType()).isEqualTo(TradeType.DIRECT.name());
        assertThat(item.genre()).isEqualTo("한국소설");
    }

    @Test
    void packageGroupUsesDeliveryTradeTypeInsteadOfRelayGroupType() {
        Groups deliveryGroup = group(15L, CustomCategory.SCIENCE_IT);
        deliveryGroup.setTradeType(TradeType.DELIVERY);
        when(groupQueryRepository.findGroupsByTradeType(1L, TradeType.DELIVERY, 5))
                .thenReturn(List.of(deliveryGroup));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        GroupResponseDTO.HomeGroupCardDTO item = (GroupResponseDTO.HomeGroupCardDTO)
                section(response, HomeSectionType.PACKAGE_GROUPS).items().get(0);
        assertThat(item.tradeType()).isEqualTo(TradeType.DELIVERY.name());
    }

    @Test
    void groupDetailIncludesAddressDetail() {
        Groups group = group(16L, CustomCategory.KOREAN_NOVEL);
        group.setMaxCapacity(2);
        GroupPlace groupPlace = GroupPlace.builder()
                .group(group)
                .placeName("교환 장소")
                .address("서울시 강남구")
                .addressDetail("101호")
                .build();
        group.setGroupPlace(groupPlace);
        org.springframework.test.util.ReflectionTestUtils.setField(group, "createdAt", Instant.now());
        when(groupsRepository.findDetailByIdAllStatuses(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupOrderByCreatedAtAsc(group)).thenReturn(List.of());

        GroupResponseDTO.GroupDetailDTO response = groupService.getGroupDetail(group.getId(), group.getHost().getId());

        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
        assertThat(response.getDetailAddress()).isEqualTo("101호");
    }

    @Test
    void homeCardReturnsNullProfileUrlWhenHostImageIsMissingOrBlank() {
        Groups noImage = group(12L, CustomCategory.SCIENCE_IT);
        Groups blankImage = group(13L, CustomCategory.ART_CULTURE, " ");
        when(groupQueryRepository.findRecentGroups(eq(1L), any(Instant.class), eq(5)))
                .thenReturn(List.of(noImage, blankImage));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(section(response, HomeSectionType.NEW_GROUPS).items())
                .extracting(item -> ((GroupResponseDTO.HomeGroupCardDTO) item).hostProfileImageUrl())
                .containsExactly(null, null);
        verify(userImageS3Service, never()).generatePresignedGetUrl(any(), anyInt());
    }

    @Test
    void homeRequestGeneratesPresignedUrlOncePerS3KeyAcrossSections() {
        Groups repeatedGroup = group(14L, CustomCategory.SCIENCE_IT, "image/users/99/profile");
        when(groupQueryRepository.findRecentGroups(eq(1L), any(Instant.class), eq(5)))
                .thenReturn(List.of(repeatedGroup));
        when(groupQueryRepository.findGroupsByTradeType(1L, TradeType.DELIVERY, 5))
                .thenReturn(List.of(repeatedGroup));
        when(userImageS3Service.generatePresignedGetUrl("image/users/99/profile", 60))
                .thenReturn("https://signed.example/profile");

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        GroupResponseDTO.HomeGroupCardDTO newGroup = (GroupResponseDTO.HomeGroupCardDTO)
                section(response, HomeSectionType.NEW_GROUPS).items().get(0);
        GroupResponseDTO.HomeGroupCardDTO packageGroup = (GroupResponseDTO.HomeGroupCardDTO)
                section(response, HomeSectionType.PACKAGE_GROUPS).items().get(0);
        assertThat(newGroup.hostProfileImageUrl()).isEqualTo("https://signed.example/profile");
        assertThat(packageGroup.hostProfileImageUrl()).isEqualTo("https://signed.example/profile");
        verify(userImageS3Service, times(1))
                .generatePresignedGetUrl("image/users/99/profile", 60);
    }

    @Test
    void hidesNewGroupsSectionWhenNoGroupExists() {
        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(sectionTypes(response)).doesNotContain(HomeSectionType.NEW_GROUPS);
    }

    @Test
    void popularBooksAreGlobalAndDoNotReceiveUserFilter() {
        GroupQueryRepository.HomeBookProjection popular =
                new GroupQueryRepository.HomeBookProjection(
                        "9781234567890",
                        "인기 책",
                        "저자",
                        "image"
                );
        when(groupQueryRepository.findPopularBooks(any(), eq(5))).thenReturn(List.of(popular));

        GroupResponseDTO.HomeResponseDTO first = groupService.getHome(user);
        GroupResponseDTO.HomeResponseDTO second =
                groupService.getHome(User.builder().id(2L).build());

        assertThat(section(first, HomeSectionType.POPULAR_BOOKS_TOP5).items())
                .isEqualTo(section(second, HomeSectionType.POPULAR_BOOKS_TOP5).items());
        verify(groupQueryRepository).findPopularBooks(1L, 5);
        verify(groupQueryRepository).findPopularBooks(2L, 5);
    }

    @Test
    void hidesGenreSectionWhenNoOpenGenreExists() {
        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(sectionTypes(response))
                .doesNotContain(HomeSectionType.RANDOM_GENRE_GROUPS);
    }

    @Test
    void exposesPickedGenreOnlyWhenVisibleGroupsRemain() {
        when(groupQueryRepository.findCategoriesWithRecruitingGroups(1L))
                .thenReturn(List.of(CustomCategory.SCIENCE_IT));
        when(groupQueryRepository.findGroupsByCategories(
                1L,
                List.of(CustomCategory.SCIENCE_IT),
                5
        )).thenReturn(List.of(group(15L, CustomCategory.SCIENCE_IT)));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        GroupResponseDTO.HomeSectionDTO genre =
                section(response, HomeSectionType.RANDOM_GENRE_GROUPS);
        assertThat(genre.title()).isEqualTo("과학·IT 도서와 함께해볼까요?");
        assertThat(genre.items()).hasSize(1);
    }

    @Test
    void bestsellerSectionUsesCachedDatabaseProjectionAndAllowsEmptyGroups() {
        when(groupQueryRepository.findBestsellerBooks(3)).thenReturn(List.of(
                new GroupQueryRepository.HomeBestsellerBookProjection(
                        "9781234567890",
                        "베스트셀러",
                        "저자",
                        "image",
                        7
                )
        ));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(section(response, HomeSectionType.BESTSELLER_BOOKS).items())
                .hasSize(1);
        GroupResponseDTO.HomeBookThumbnailDTO item =
                (GroupResponseDTO.HomeBookThumbnailDTO)
                        section(response, HomeSectionType.BESTSELLER_BOOKS).items().get(0);
        assertThat(item.rank()).isEqualTo(7);
        verify(groupQueryRepository).findBestsellerBooks(3);
    }

    @Test
    void classicSectionUsesSeedCandidateIsbn13() {
        when(groupQueryRepository.findClassicGroups(
                1L,
                HomeCandidateSectionType.CLASSIC_BOOK_GROUP,
                5
        )).thenReturn(List.of(group(20L, CustomCategory.WORLD_NOVEL)));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(section(response, HomeSectionType.CLASSIC_GROUPS).items())
                .hasSize(1);
    }

    @Test
    void packageSectionDelegatesOpenDeliveryFilteringToHomeQuery() {
        when(groupQueryRepository.findGroupsByTradeType(1L, TradeType.DELIVERY, 5))
                .thenReturn(List.of(group(30L, CustomCategory.SCIENCE_IT)));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(section(response, HomeSectionType.PACKAGE_GROUPS).items())
                .hasSize(1);
        verify(groupQueryRepository)
                .findGroupsByTradeType(1L, TradeType.DELIVERY, 5);
    }

    @Test
    void hidesNearbyDirectSectionWhenUserHasNoExchangePlace() {
        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(sectionTypes(response))
                .doesNotContain(HomeSectionType.NEARBY_DIRECT_GROUPS);
        verify(groupQueryRepository, never())
                .findDirectGroupsAtCoordinate(any(), any(), any(), anyInt());
    }

    @Test
    void filtersValidNearbyDirectCoordinatesBeforeLimitingToTwo() {
        BigDecimal firstX = new BigDecimal("126.9000");
        BigDecimal firstY = new BigDecimal("37.5000");
        BigDecimal secondX = new BigDecimal("127.1000");
        BigDecimal secondY = new BigDecimal("37.6000");
        BigDecimal thirdX = new BigDecimal("127.2000");
        BigDecimal thirdY = new BigDecimal("37.7000");
        when(userExchangeRepository.findByUserIdWithLocation(1L)).thenReturn(List.of(
                exchange(1L, firstX, firstY),
                exchange(2L, secondX, secondY),
                exchange(3L, thirdX, thirdY)
        ));
        when(groupQueryRepository.existsDirectGroupsAtCoordinate(
                1L,
                firstX,
                firstY
        )).thenReturn(false);
        when(groupQueryRepository.existsDirectGroupsAtCoordinate(
                1L,
                secondX,
                secondY
        )).thenReturn(false);
        when(groupQueryRepository.existsDirectGroupsAtCoordinate(
                1L,
                thirdX,
                thirdY
        )).thenReturn(true);
        when(groupQueryRepository.findDirectGroupsAtCoordinate(
                1L,
                thirdX,
                thirdY,
                5
        )).thenReturn(List.of(group(40L, CustomCategory.ART_CULTURE)));

        GroupResponseDTO.HomeResponseDTO response = groupService.getHome(user);

        assertThat(section(response, HomeSectionType.NEARBY_DIRECT_GROUPS).items())
                .hasSize(1);
        verify(groupQueryRepository).findDirectGroupsAtCoordinate(
                1L,
                thirdX,
                thirdY,
                5
        );
    }

    private List<HomeSectionType> sectionTypes(
            GroupResponseDTO.HomeResponseDTO response
    ) {
        return response.sections().stream()
                .map(GroupResponseDTO.HomeSectionDTO::sectionType)
                .toList();
    }

    private GroupResponseDTO.HomeSectionDTO section(
            GroupResponseDTO.HomeResponseDTO response,
            HomeSectionType sectionType
    ) {
        return response.sections().stream()
                .filter(section -> section.sectionType() == sectionType)
                .findFirst()
                .orElseThrow();
    }

    private Groups group(Long id, CustomCategory category) {
        return group(id, category, null);
    }

    private Groups group(Long id, CustomCategory category, String s3Key) {
        User host = User.builder().id(99L).nickName("host").build();
        if (s3Key != null) {
            UserImage image = UserImage.builder()
                    .id(id)
                    .user(host)
                    .s3Key(s3Key)
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(host, "userImage", image);
        }
        Book book = Book.builder()
                .id(id)
                .isbn13("9781234567" + String.format("%03d", id))
                .title("책 " + id)
                .author("저자")
                .publisher("출판사")
                .image("image")
                .totalPages(200)
                .link("link")
                .category(category)
                .build();
        return Groups.builder()
                .id(id)
                .host(host)
                .book(book)
                .groupName("그룹 " + id)
                .groupStatus(GroupStatus.RECRUITING)
                .groupType(GroupType.RELAY)
                .tradeType(TradeType.DIRECT)
                .readingPeriod(14)
                .build();
    }

    private UserExchange exchange(Long id, BigDecimal x, BigDecimal y) {
        return UserExchange.builder()
                .id(id)
                .user(user)
                .location(Location.builder()
                        .id(id)
                        .placeName("장소 " + id)
                        .address("주소 " + id)
                        .x(x)
                        .y(y)
                        .build())
                .build();
    }
}
