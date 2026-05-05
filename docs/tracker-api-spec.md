# Tracker Domain API 명세서

> 기준 브랜치: HEAD  
> 최종 수정일: 2026-05-05

---

## 목차

1. [개요](#1-개요)
2. [공통 정보](#2-공통-정보)
3. [Enum 정의](#3-enum-정의)
4. [공통 DTO](#4-공통-dto)
5. [API 목록](#5-api-목록)
   - [5.1 목록 조회](#51-목록-조회)
   - [5.2 상세 조회](#52-상세-조회)
   - [5.3 이미지 / presigned URL](#53-이미지--presigned-url)
   - [5.4 독서 상태](#54-독서-상태)
   - [5.5 배송 (택배 교환)](#55-배송-택배-교환)
   - [5.6 약속 (직접 교환)](#56-약속-직접-교환)
6. [상태 전이 다이어그램](#6-상태-전이-다이어그램)
7. [에러 코드](#7-에러-코드)

---

## 1. 개요

Tracker 도메인은 책 릴레이(교환) 그룹의 진행 상태를 관리합니다.

### 거래 유형 (TradeType)

| 값 | 설명 |
|----|------|
| `PARCEL` | 택배 교환 — 배송사 + 운송장 번호 등록, 이미지 증빙 |
| `DIRECT` | 직접 교환 — 약속 일시·장소 등록, 쌍방 확인 |

### 그룹 유형 (GroupType)

| 값 | 설명 |
|----|------|
| `RELAY` | 1:1 릴레이 독서 |
| `TOGETHER` | 다수 참여 함께 읽기 |

---

## 2. 공통 정보

### Base URL

```
/api/groups
```

### 인증

모든 엔드포인트는 Bearer 토큰 인증이 필요합니다.

```
Authorization: Bearer {accessToken}
```

### 공통 응답 형식

```json
{
  "isSuccess": true,
  "code": "TRACKER_DETAIL_GET_OK",
  "message": "트래커 상세 조회 성공",
  "result": { ... }
}
```

---

## 3. Enum 정의

### TrackerStatus — 트래커 전체 상태 (9단계)

| 값 | 의미 |
|----|------|
| `READY` | 매칭 완료, 독서 전 |
| `MY_BOOK_READING` | 자신의 책 읽는 중 |
| `MY_BOOK_REVIEWING` | 1차 독서 완료, 후기 작성 중 |
| `EXCHANGING` | 1차 교환 진행 중 |
| `EXCHANGED` | 1차 교환 완료 |
| `PARTNER_BOOK_READING` | 파트너 책 읽는 중 |
| `PARTNER_BOOK_REVIEWING` | 2차 독서 완료, 후기 작성 중 |
| `RETURNING` | 2차 교환(반납) 진행 중 |
| `COMPLETED` | 릴레이 종료 |

### ReadingStatus — 개인 독서 상태 (8단계)

| 값 | 의미 |
|----|------|
| `IDLE` | 초기 |
| `MY_BOOK_READING` | 자신의 책 읽는 중 |
| `MY_BOOK_READ_DONE` | 자신의 책 읽기 완료 |
| `MY_BOOK_REVIEW_DONE` | 자신의 책 후기 완료, 교환 준비 |
| `PARTNER_BOOK_READING` | 파트너 책 읽는 중 |
| `PARTNER_BOOK_READ_DONE` | 파트너 책 읽기 완료 |
| `PARTNER_BOOK_REVIEW_DONE` | 파트너 책 후기 완료, 반납 준비 |
| `DONE` | 반납 완료 |

### DeliveryStatus

| 값 | 의미 |
|----|------|
| `SHIPPING` | 배송 중 |
| `RETURNED` | 수령/반납 완료 |

---

## 4. 공통 DTO

### TrackerDetailResponseDTO

| 필드 | 타입 | 설명 |
|------|------|------|
| `trackerId` | `Long` | 트래커 ID |
| `bookTitle` | `String` | 교환 도서 제목 |
| `partnerNickname` | `String` | 파트너 닉네임 |
| `trackerStatus` | `TrackerStatus` | 현재 트래커 상태 |
| `startDate` | `LocalDateTime` | 예정 독서 시작일 |
| `endDate` | `LocalDateTime` | 예정 독서 종료일 (연장 반영) |
| `startedAt` | `LocalDateTime` | 실제 독서 시작 시각 |
| `completedAt` | `LocalDateTime` | 실제 완료 시각 |
| `extensionCount` | `Integer` | 연장 횟수 (최대 1) |
| `extensionDays` | `Integer` | 누적 연장 일수 |
| `readingPeriod` | `Integer` | 그룹 기본 독서 기간(일) |
| `remainingDays` | `Integer` | 남은 일수 (독서 중: endDate 기준, 교환/반납 중: meetingTime 기준, 그 외: 0) |
| `deliveryInfo` | `DeliveryInfo?` | 배송 정보 (택배 교환 시, 최근 배송 건) |
| `meetingInfo` | `MeetingInfo?` | 약속 정보 (직접 교환 시) |

#### DeliveryInfo (중첩 객체)

| 필드 | 타입 | 설명 |
|------|------|------|
| `deliveryCompany` | `String` | 택배사명 |
| `trackingNumber` | `String` | 운송장 번호 |

#### MeetingInfo (중첩 객체)

| 필드 | 타입 | 설명 |
|------|------|------|
| `meetingTime` | `LocalDateTime?` | 약속 일시 |
| `placeName` | `String?` | 장소명 (약속 미등록 시 그룹 선호 지역) |
| `address` | `String?` | 주소 |

---

### TrackerListResponseDTO

| 필드 | 타입 | 설명 |
|------|------|------|
| `groupId` | `Long` | 그룹 ID |
| `groupType` | `String` | `"RELAY"` 또는 `"TOGETHER"` |
| `bookTitle` | `String?` | 도서 제목 |
| `bookImage` | `String?` | 도서 이미지 URL |
| `bookAuthor` | `String?` | 저자 |
| `bookCategory` | `String?` | 카테고리 |
| `tradeType` | `TradeType` | `PARCEL` or `DIRECT` |
| `relayDetail` | `RelayDetail?` | RELAY 그룹 전용 상세 |
| `togetherDetail` | `TogetherDetail?` | TOGETHER 그룹 전용 상세 |

#### RelayDetail (RELAY 그룹 전용)

| 필드 | 타입 | 설명 |
|------|------|------|
| `partnerNickname` | `String` | 파트너 닉네임 |
| `hostProfileImageUrl` | `String?` | 호스트 프로필 이미지 Presigned GET URL (60분 유효) |
| `guestProfileImageUrls` | `List<String?>` | 게스트 프로필 이미지 Presigned GET URL 목록 |
| `trackerStatus` | `TrackerStatus` | 현재 트래커 상태 |
| `stepDates` | `List<String>` | 단계별 날짜 목록 (`"MM.dd"` 형식) |

#### TogetherDetail (TOGETHER 그룹 전용)

| 필드 | 타입 | 설명 |
|------|------|------|
| `hostNickname` | `String` | 호스트 닉네임 |
| `participantCount` | `int` | 참여 인원 수 |
| `myReadingRate` | `int` | 내 독서율 (0–100) |
| `groupReadingRate` | `int` | 그룹 전체 독서율 (0–100) |

---

## 5. API 목록

---

### 5.1 목록 조회

---

#### GET `/me/trackers` — 전체 트래커 목록 조회

내가 참여한 모든 그룹의 트래커 목록을 반환합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/me/trackers` |
| Auth | 필요 |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_LIST_GET_OK` (200) |
| result 타입 | `List<TrackerListResponseDTO>` |

---

#### GET `/me/trackers/host` — 호스트 트래커 목록 조회

내가 호스트인 그룹의 트래커 목록을 반환합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/me/trackers/host` |
| Auth | 필요 |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_HOST_LIST_GET_OK` (200) |
| result 타입 | `List<TrackerListResponseDTO>` |

---

#### GET `/me/trackers/guest` — 게스트 트래커 목록 조회

내가 게스트인 그룹의 트래커 목록을 반환합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/me/trackers/guest` |
| Auth | 필요 |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_GUEST_LIST_GET_OK` (200) |
| result 타입 | `List<TrackerListResponseDTO>` |

---

### 5.2 상세 조회

---

#### GET `/{groupId}/tracker` — 트래커 상세 조회

특정 그룹의 트래커 상세 정보를 반환합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/{groupId}/tracker` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_DETAIL_GET_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `TRACKER_NOT_FOUND` | 404 | 트래커 없음 |
| `PARTNER_NOT_FOUND` | 404 | 파트너 정보 없음 |

---

### 5.3 이미지 / Presigned URL

---

#### POST `/{groupId}/tracker/images/presigned-url` — 이미지 업로드 Presigned PUT URL 발급

트래커 이미지(배송 증빙/수령 증빙)를 S3에 업로드하기 위한 Presigned PUT URL을 발급합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `POST` |
| Path | `/api/groups/{groupId}/tracker/images/presigned-url` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKING_PRESIGNED_URL_ISSUED` (200) |
| result 타입 | `PresignedUrlResponseDTO` |

**PresignedUrlResponseDTO**

| 필드 | 타입 | 설명 |
|------|------|------|
| `presignedPutUrl` | `String` | S3 Presigned PUT URL (유효 시간: **10분**) |

> **S3 키 형식**: `image/trackers/{uuid}` 형식을 반드시 준수해야 합니다. 이후 배송 등록(`POST /delivery`) 또는 수령 확인(`PATCH /reception`) 호출 시 이 키를 `s3Key` 필드에 전달합니다.

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |

---

#### GET `/{groupId}/tracker/images/delivery` — 배송 증빙 이미지 조회

발신자가 업로드한 배송 증빙 이미지의 Presigned GET URL을 반환합니다.  
현재 사용자가 수신자(receiver)인 가장 최근 `SHIPPING` 상태 배송 건의 이미지를 조회합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/{groupId}/tracker/images/delivery` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKING_IMAGE_FOUND` (200) |
| result 타입 | `TrackerImageGetResponseDTO` |

**TrackerImageGetResponseDTO**

| 필드 | 타입 | 설명 |
|------|------|------|
| `presignedGetUrl` | `String` | Presigned GET URL (유효 시간: **60분**) |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `TRACKING_IMAGE_NOT_FOUND` | 404 | 배송 이미지 없음 |

---

#### GET `/{groupId}/tracker/images/received` — 수령 증빙 이미지 조회

수신자가 업로드한 수령 증빙 이미지의 Presigned GET URL을 반환합니다.  
현재 사용자가 발신자(sender)인 가장 최근 `RETURNED` 상태 배송 건의 이미지를 조회합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/{groupId}/tracker/images/received` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `RECEIVED_IMAGE_FOUND` (200) |
| result 타입 | `TrackerImageGetResponseDTO` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `RECEIVED_IMAGE_NOT_FOUND` | 404 | 수령 이미지 없음 |

---

### 5.4 독서 상태

---

#### PATCH `/{groupId}/tracker/reading` — 독서 시작 등록

현재 독서를 시작했음을 등록합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| Path | `/api/groups/{groupId}/tracker/reading` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_READING_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**상태 전이 조건**

| 현재 TrackerStatus | 현재 ReadingStatus | 전이 후 TrackerStatus |
|---|---|---|
| `READY` | `IDLE` or `MY_BOOK_REVIEW_DONE` | `MY_BOOK_READING` |
| `EXCHANGED` | `IDLE` or `MY_BOOK_REVIEW_DONE` | `PARTNER_BOOK_READING` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRACKER_STATUS` | 400 | 상태 전이 불가 |

---

#### PATCH `/{groupId}/tracker/done` — 독서 완료 등록

현재 책 읽기를 완료했음을 등록합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| Path | `/api/groups/{groupId}/tracker/done` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_DONE_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**상태 전이 조건**

> 개인 ReadingStatus가 변경됩니다. 양쪽 모두 완료 시 TrackerStatus가 전이됩니다.

| 현재 TrackerStatus | 완료 시 ReadingStatus |
|---|---|
| `MY_BOOK_READING` | `MY_BOOK_READ_DONE` |
| `PARTNER_BOOK_READING` | `PARTNER_BOOK_READ_DONE` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRACKER_STATUS` | 400 | 상태 전이 불가 |

---

#### PATCH `/{groupId}/tracker/extension` — 독서 기간 연장 요청

독서 종료 예정일을 연장합니다. 릴레이 당 **최대 1회**만 가능합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| Path | `/api/groups/{groupId}/tracker/extension` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `days` | `int` | N | `3` | 연장할 일수 (반드시 > 0) |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_EXTENSION_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**호출 가능 조건**

| 조건 | 내용 |
|------|------|
| TrackerStatus | `MY_BOOK_READING` 또는 `PARTNER_BOOK_READING` |
| extensionCount | `0` (미연장 상태) |
| days | `> 0` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRACKER_STATUS` | 400 | 독서 중 상태가 아님 |
| `EXTENSION_LIMIT_EXCEEDED` | 400 | 이미 1회 연장함 |
| `INVALID_TRACKER_DAYS` | 400 | days ≤ 0 |

---

### 5.5 배송 (택배 교환)

> `TradeType = PARCEL` 그룹에서 사용합니다.

---

#### POST `/{groupId}/tracker/delivery` — 배송 시작 등록

책을 발송했음을 등록합니다. S3에 배송 증빙 이미지를 먼저 업로드한 후 해당 `s3Key`를 전달해야 합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `POST` |
| Path | `/api/groups/{groupId}/tracker/delivery` |
| Auth | 필요 |
| Content-Type | `application/json` |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**Request Body — TrackerShippingRequestDTO**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `deliveryCompany` | `String` | Y | `@NotBlank` | 택배사명 |
| `trackingNumber` | `String` | Y | `@NotBlank` | 운송장 번호 |
| `s3Key` | `String` | Y | `@NotBlank`, 형식: `image/trackers/{uuid}` | 배송 증빙 이미지 S3 키 |

**요청 예시**

```json
{
  "deliveryCompany": "CJ대한통운",
  "trackingNumber": "123456789012",
  "s3Key": "image/trackers/550e8400-e29b-41d4-a716-446655440000"
}
```

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_SHIPPING_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**상태 전이**

| 현재 TrackerStatus | 전이 후 TrackerStatus | 설명 |
|---|---|---|
| `MY_BOOK_REVIEWING` | `EXCHANGING` | 1차 교환 발송 |
| `PARTNER_BOOK_REVIEWING` | `RETURNING` | 2차 반납 발송 |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRACKER_STATUS` | 400 | 발송 불가 상태 |
| `ALREADY_SHIPPED` | 409 | 동일 단계 중복 발송 |
| `INVALID_S3_KEY_FORMAT` | 400 | S3 키 형식 오류 |
| `IMAGE_NOT_FOUND_IN_S3` | 400 | S3에 이미지 없음 |
| `DUPLICATE_S3_KEY` | 400 | 이미 사용된 S3 키 |

---

#### PATCH `/{groupId}/tracker/reception` — 수령 확인

책 수령을 확인합니다. S3에 수령 증빙 이미지를 먼저 업로드한 후 해당 `s3Key`를 전달해야 합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| Path | `/api/groups/{groupId}/tracker/reception` |
| Auth | 필요 |
| Content-Type | `application/json` |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**Request Body — TrackerReceiveRequestDTO**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `s3Key` | `String` | Y | `@NotBlank`, 형식: `image/trackers/{uuid}` | 수령 증빙 이미지 S3 키 |

**요청 예시**

```json
{
  "s3Key": "image/trackers/660e9400-f30c-52e5-b827-557766551111"
}
```

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_RECEIVE_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**상태 전이**

수신자/발신자 양쪽 모두 수령 확인 완료 시 TrackerStatus가 전이됩니다.

| 현재 TrackerStatus | 전이 후 TrackerStatus | 조건 |
|---|---|---|
| `EXCHANGING` | `EXCHANGED` | 양쪽 수령 확인 완료 |
| `RETURNING` | `COMPLETED` | 양쪽 수령 확인 완료 |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRACKER_STATUS` | 400 | 수령 확인 불가 상태 (`EXCHANGING` 또는 `RETURNING` 아님) |
| `INVALID_S3_KEY_FORMAT` | 400 | S3 키 형식 오류 |

---

### 5.6 약속 (직접 교환)

> `TradeType = DIRECT` 그룹에서만 사용 가능합니다.

---

#### GET `/{groupId}/tracker/meetings` — 약속 정보 조회

현재 등록된 약속 정보를 조회합니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| Path | `/api/groups/{groupId}/tracker/meetings` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_MEETING_GET_OK` (200) |
| result 타입 | `TrackerMeetingResponseDTO` |

**TrackerMeetingResponseDTO**

| 필드 | 타입 | 설명 |
|------|------|------|
| `meetingTime` | `LocalDateTime?` | 약속 일시 |
| `placeName` | `String?` | 장소명 |
| `address` | `String?` | 주소 |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRADE_TYPE` | 400 | 택배 교환 그룹에서 호출 |
| `MEETING_NOT_FOUND` | 404 | 약속 정보 없음 |

---

#### PATCH `/{groupId}/tracker/meetings` — 약속 등록/수정

약속 일시·장소를 등록하거나 수정합니다. 최초 등록 시 TrackerStatus가 전이됩니다.  
수정 시 파트너의 완료 확인이 초기화됩니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| Path | `/api/groups/{groupId}/tracker/meetings` |
| Auth | 필요 |
| Content-Type | `application/json` |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**Request Body — TrackerMeetingRequestDTO**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `meetingTime` | `LocalDateTime` | Y | `@NotNull`, `@Future` | 약속 일시 (미래 시각만 허용, 형식: `yyyy-MM-dd'T'HH:mm:ss`) |
| `placeName` | `String` | Y | `@NotBlank` | 장소명 |
| `address` | `String` | N | — | 주소 |
| `addressDetail` | `String` | N | — | 상세 주소 |
| `zipCode` | `String` | N | `@Size(max=5)` | 우편번호 |

**요청 예시**

```json
{
  "meetingTime": "2026-05-10T14:30:00",
  "placeName": "강남역 2번 출구 앞",
  "address": "서울특별시 강남구 강남대로 396",
  "addressDetail": "2번 출구",
  "zipCode": "06232"
}
```

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_MEETING_UPDATE_OK` (200) |
| result 타입 | `TrackerMeetingResponseDTO` |

**상태 전이 (최초 등록 시)**

| 현재 TrackerStatus | 전이 후 TrackerStatus |
|---|---|
| `MY_BOOK_REVIEWING` | `EXCHANGING` |
| `PARTNER_BOOK_REVIEWING` | `RETURNING` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRADE_TYPE` | 400 | 택배 교환 그룹에서 호출 |
| `INVALID_TRACKER_STATUS` | 400 | 약속 등록 불가 상태 |
| `MEETING_NOT_FOUND` | 404 | 약속 객체 없음 |

---

#### PATCH `/{groupId}/tracker/meetings/completion` — 약속 완료 확인

직접 교환이 완료되었음을 확인합니다. **호스트와 게스트 모두 각각 호출**해야 최종 완료됩니다.

**요청**

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| Path | `/api/groups/{groupId}/tracker/meetings/completion` |
| Auth | 필요 |

**Path Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `groupId` | `Long` | Y | 그룹 ID |

**응답**

| 항목 | 내용 |
|------|------|
| 성공 코드 | `TRACKER_MEETING_DONE_OK` (200) |
| result 타입 | `TrackerDetailResponseDTO` |

**상태 전이 (양쪽 모두 확인 완료 시)**

| 현재 TrackerStatus | 전이 후 TrackerStatus |
|---|---|
| `EXCHANGING` | `EXCHANGED` |
| `RETURNING` | `COMPLETED` |

**에러**

| 코드 | HTTP | 발생 조건 |
|------|------|----------|
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `INVALID_TRACKER_STATUS` | 400 | 상태가 `EXCHANGING` 또는 `RETURNING` 아님 |
| `MEETING_NOT_FOUND` | 404 | 약속 정보 없음 |

---

## 6. 상태 전이 다이어그램

### 전체 흐름 (PARCEL / DIRECT 공통)

```
                        ┌─────────────────────────────────┐
                        │           READY                 │
                        │  (매칭 완료, 독서 전)            │
                        └────────────┬────────────────────┘
                                     │ PATCH /reading
                                     ▼
                        ┌─────────────────────────────────┐
                        │       MY_BOOK_READING           │
                        │  (자신의 책 읽는 중)             │
                        └────────────┬────────────────────┘
                                     │ PATCH /done (양쪽 완료 후 자동 전이)
                                     ▼
                        ┌─────────────────────────────────┐
                        │      MY_BOOK_REVIEWING          │
                        │  (후기 작성 중)                 │
                        └────────────┬────────────────────┘
                    PARCEL           │              DIRECT
          POST /delivery             │         PATCH /meetings (최초 등록)
                    ┌────────────────┘────────────────┐
                    ▼                                  ▼
       ┌────────────────────┐             ┌────────────────────┐
       │     EXCHANGING     │             │     EXCHANGING     │
       │  (배송 중)         │             │  (약속 진행 중)    │
       └─────────┬──────────┘             └────────┬───────────┘
                 │ PATCH /reception (양쪽)           │ PATCH /meetings/completion (양쪽)
                 ▼                                  ▼
       ┌──────────────────────────────────────────────────────┐
       │                     EXCHANGED                        │
       │              (1차 교환 완료)                         │
       └──────────────────────┬───────────────────────────────┘
                              │ PATCH /reading
                              ▼
                 ┌─────────────────────────────────┐
                 │      PARTNER_BOOK_READING        │
                 │   (파트너 책 읽는 중)            │
                 └────────────┬────────────────────┘
                              │ PATCH /done (양쪽 완료 후 자동 전이)
                              ▼
                 ┌─────────────────────────────────┐
                 │     PARTNER_BOOK_REVIEWING       │
                 │   (후기 작성 중)                │
                 └────────────┬────────────────────┘
             PARCEL           │              DIRECT
   POST /delivery             │         PATCH /meetings (최초 등록)
             ┌────────────────┘────────────────┐
             ▼                                  ▼
┌────────────────────┐             ┌────────────────────┐
│     RETURNING      │             │     RETURNING      │
│  (반납 배송 중)    │             │  (반납 약속 중)    │
└─────────┬──────────┘             └────────┬───────────┘
          │ PATCH /reception (양쪽)           │ PATCH /meetings/completion (양쪽)
          ▼                                  ▼
┌──────────────────────────────────────────────────────┐
│                     COMPLETED                        │
│                  (릴레이 종료)                       │
└──────────────────────────────────────────────────────┘
```

### 연장 가능 구간

```
MY_BOOK_READING     ─── PATCH /extension ──▶ endDate + N일
PARTNER_BOOK_READING ── PATCH /extension ──▶ endDate + N일
(릴레이 당 최대 1회)
```

---

## 7. 에러 코드

### TrackerErrorCode

| 에러 코드 | HTTP | 설명 |
|-----------|------|------|
| `INVALID_TRACKER_STATUS` | 400 | 현재 상태에서 허용되지 않는 전이 |
| `INVALID_DELIVERY_INFO` | 400 | 유효하지 않은 배송 정보 |
| `EXTENSION_LIMIT_EXCEEDED` | 400 | 연장 횟수 초과 (최대 1회) |
| `INVALID_TRACKER_DAYS` | 400 | 연장 일수 ≤ 0 |
| `INVALID_TRADE_TYPE` | 400 | 직접 교환 전용 API를 택배 그룹에서 호출 |
| `TRACKER_NOT_CREATED` | 400 | 트래커 생성 실패 |
| `INVALID_PARTNER_COUNT` | 400 | 1:1 그룹이 아님 |
| `NOT_YOUR_TURN` | 403 | 현재 독서 차례가 아님 |
| `NOT_GROUP_MEMBER` | 403 | 해당 그룹 미소속 |
| `NOT_TRACKER_OWNER` | 403 | 트래커 제어 권한 없음 |
| `TRACKER_ALREADY_EXISTS` | 403 | 이미 트래커 존재 |
| `NOT_RELAY_GROUP` | 403 | 릴레이 그룹이 아님 |
| `TRACKER_NOT_FOUND` | 404 | 트래커 없음 |
| `NEXT_MEMBER_NOT_FOUND` | 404 | 다음 독서자 없음 |
| `FIRST_MEMBER_NOT_FOUND` | 404 | 첫 번째 독서자 없음 |
| `MEETING_NOT_FOUND` | 404 | 약속 정보 없음 |
| `PARTNER_NOT_FOUND` | 404 | 파트너 정보 없음 |
| `ALREADY_SHIPPED` | 409 | 동일 단계 중복 발송 |

### TrackerImageErrorCode

| 에러 코드 | HTTP | 설명 |
|-----------|------|------|
| `INVALID_S3_KEY_FORMAT` | 400 | S3 키 형식 오류 (`image/trackers/{uuid}` 아님) |
| `DUPLICATE_S3_KEY` | 400 | 이미 사용된 S3 키 |
| `INVALID_IMAGE_TYPE` | 400 | 잘못된 이미지 타입 |
| `IMAGE_NOT_FOUND_IN_S3` | 400 | S3에 이미지 없음 (업로드 미완료) |
| `TRACKING_IMAGE_NOT_FOUND` | 404 | 배송 증빙 이미지 없음 |
| `RECEIVED_IMAGE_NOT_FOUND` | 404 | 수령 증빙 이미지 없음 |
| `S3_ACCESS_ERROR` | 500 | S3 접근 오류 |

---

## 부록: 이미지 업로드 플로우

배송 등록 또는 수령 확인 시 아래 순서를 따릅니다.

```
1. POST /api/groups/{groupId}/tracker/images/presigned-url
   → presignedPutUrl, s3Key 수신

2. PUT {presignedPutUrl}  (S3 직접 업로드, 유효 시간 10분 이내)
   Body: 이미지 바이너리
   Content-Type: image/*

3. POST /api/groups/{groupId}/tracker/delivery  (택배 발송)
   또는
   PATCH /api/groups/{groupId}/tracker/reception  (수령 확인)
   Body: { "s3Key": "image/trackers/{uuid}", ... }
```

---

## 부록: 알림 발송 이벤트

| TrackerAction | 발송 시점 | 알림 제목 |
|---------------|----------|----------|
| `READING_STARTED` | 독서 시작 등록 | 파트너가 책을 읽기 시작했어요! |
| `READING_FINISHED` | 독서 완료 등록 | 파트너가 책을 다 읽었어요 |
| `EXTEND_REQUESTED` | 기간 연장 | 독서 기간이 조금 늘어났어요 |
| `SHIPPING_REGISTERED` | 배송 시작 (1차) | 소중한 책이 오고 있어요! |
| `RECEIVED_CONFIRMED` | 수령 확인 / 약속 완료 (1차) | 책이 무사히 도착했대요! |
| `SHIPPING_REGISTERED` (반납) | 배송 시작 (2차) | 책이 집으로 돌아오고 있어요 |
| `RECEIVED_CONFIRMED` (완료) | 수령 확인 / 약속 완료 (2차) | 교환독서가 모두 끝났어요 |
| `REVIEW_DONE_CONFIRMED` | 후기 작성 완료 | 파트너가 후기를 작성했어요! |
