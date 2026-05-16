package com.example.bookiibookii.domain.tracker.dto.res;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackerListItemResDTO {

    // 그룹의 메타데이터
    private Long groupId;
    private String groupName;
    private TradeType tradeType;

    // 매칭멤버의 ReadingStatus, ExchangeStatus로 조립
    private TrackerDisplayStatus displayStatus;

    private Integer remainingDays; // 배송중이면 null, D-day
    private BookInfo myCurrentBook;
    private BookInfo partnerCurrentBook;

}