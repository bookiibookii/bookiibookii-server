-- =============================================================================
-- memberbook 독서카드 API 테스트용 더미 데이터
-- 대상 API (3개):
--   GET  /api/member-books/group/{groupId}/cards          목록
--   GET  /api/member-books/cards/detail/{cardId}            상세
--   DELETE /api/member-books/cards/{cardId}                 내 화면에서 제거
-- =============================================================================
-- 사용법 (local, ddl-auto=create-drop 인 경우):
--   1. 애플리케이션을 한 번 실행해 테이블 생성
--   2. MySQL에서 booki_local DB 선택 후 본 스크립트 실행
--   3. 호스트(user 90001) 또는 게스트(user 90002)로 로그인 후 scripts/memberbook-card-api.http 호출
--
-- 고정 ID 요약:
--   users: 90001(호스트), 90002(게스트)
--   group_id: 90001
--   matchedmember_id: 90001(호스트), 90002(게스트)
--   member_book_id: 90001~90004
--   card_id: 90001(TEXT/호스트), 90002(IMAGE/게스트), 90003(TEXT/호스트-상대책)
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테스트 ID 충돌 시 삭제 (재실행용)
DELETE FROM member_card WHERE card_id IN (90001, 90002, 90003);
DELETE FROM card_images WHERE card_id IN (90001, 90002, 90003);
DELETE FROM cards WHERE card_id IN (90001, 90002, 90003);
DELETE FROM member_book WHERE member_book_id BETWEEN 90001 AND 90004;
DELETE FROM matchedmember WHERE matchedmember_id IN (90001, 90002);
DELETE FROM `groups` WHERE group_id = 90001;
DELETE FROM book WHERE book_id IN (90001, 90002);
DELETE FROM users WHERE id IN (90001, 90002);

-- ----------------------------------------------------------------------------- users
INSERT INTO users (id, nickname, social_type, social_id, status, role, onboarding_status, created_at, updated_at)
VALUES
    (90001, '테스트호스트', 'KAKAO', 'mb-test-host-90001', 'ACTIVE', 'USER', 'COMPLETED', NOW(), NOW()),
    (90002, '테스트게스트', 'KAKAO', 'mb-test-guest-90002', 'ACTIVE', 'USER', 'COMPLETED', NOW(), NOW());

-- ----------------------------------------------------------------------------- book
INSERT INTO book (book_id, isbn13, title, author, publisher, image, totalPages, link, category, created_at, updated_at)
VALUES
    (90001, '9788900000001', '호스트의 책', '김호스트', '북키출판', 'https://example.com/host-book.jpg', 300,
     'https://example.com/host-book', 'KOREAN_NOVEL', NOW(), NOW()),
    (90002, '9788900000002', '게스트의 책', '이게스트', '북키출판', 'https://example.com/guest-book.jpg', 250,
     'https://example.com/guest-book', 'KOREAN_NOVEL', NOW(), NOW());

-- ----------------------------------------------------------------------------- groups
INSERT INTO `groups` (
    group_id, book_id, host_id, max_capacity, start_date, group_period,
    group_status, group_comment, group_type, trade_type, prefer_region, group_name,
    created_at, updated_at
)
VALUES (
    90001, 90001, 90001, 2, CURDATE(), 14,
    'MATCHED', 'memberbook 카드 API 테스트 그룹', 'RELAY', 'DIRECT', '서울', '카드테스트방',
    NOW(), NOW()
);

-- ----------------------------------------------------------------------------- matchedmember
INSERT INTO matchedmember (
    matchedmember_id, group_id, user_id, role, member_status,
    reading_status, exchange_status, is_review_written, created_at, updated_at
)
VALUES
    (90001, 90001, 90001, 'HOST',  'JOINED', 'MY_BOOK_READING', 'NOT_STARTED', 0, NOW(), NOW()),
    (90002, 90001, 90002, 'GUEST', 'JOINED', 'MY_BOOK_READING', 'NOT_STARTED', 0, NOW(), NOW());

-- ----------------------------------------------------------------------------- member_book (그룹당 멤버 2권)
INSERT INTO member_book (
    member_book_id, group_id, book_id, matchedmember_id, is_mine, progress_rate, removed_at,
    created_at, updated_at
)
VALUES
    (90001, 90001, 90001, 90001, 1, 10.0, NULL, NOW(), NOW()),  -- 호스트 + 호스트책
    (90002, 90001, 90002, 90001, 0,  0.0, NULL, NOW(), NOW()),  -- 호스트 + 게스트책
    (90003, 90001, 90002, 90002, 1, 20.0, NULL, NOW(), NOW()),  -- 게스트 + 게스트책
    (90004, 90001, 90001, 90002, 0,  0.0, NULL, NOW(), NOW());  -- 게스트 + 호스트책

-- ----------------------------------------------------------------------------- cards
INSERT INTO cards (
    card_id, member_book_id, card_type, page, memo, quotation, created_at, updated_at
)
VALUES
    (90001, 90001, 'TEXT',  42, '호스트 메모', '호스트가 남긴 인용문입니다.', NOW(), NOW()),
    (90002, 90003, 'IMAGE', 88, '게스트 이미지 카드', NULL, DATE_ADD(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 1 MINUTE)),
    (90003, 90002, 'TEXT',  15, '상대책 메모', '호스트가 상대 책에 남긴 카드', DATE_ADD(NOW(), INTERVAL 2 MINUTE), DATE_ADD(NOW(), INTERVAL 2 MINUTE));

-- IMAGE 카드용 (S3 실제 파일 없어도 목록/상세/삭제 테스트 가능, presigned URL만 실패할 수 있음)
INSERT INTO card_images (card_image_id, card_id, s3_key, created_at, updated_at)
VALUES (
    90001, 90002,
    'image/cards/11111111-1111-1111-1111-111111111111',
    NOW(), NOW()
);

-- 삭제 API 사전 검증용: 호스트가 90003을 숨긴 상태로 넣고 싶을 때 아래 주석 해제
-- INSERT INTO member_card (card_state_id, card_id, matchedmember_id, bookmarked, hidden, created_at, updated_at)
-- VALUES (90001, 90003, 90001, 0, 1, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 테스트 시나리오
-- =============================================================================
-- [목록] GET /api/member-books/group/90001/cards
--   - 호스트(90001) 로그인: 카드 90001, 90002, 90003 모두 보임 (hidden 없을 때)
--   - 게스트(90002) 로그인: 동일 3건 보임
--
-- [상세] GET /api/member-books/cards/detail/90002
--   - IMAGE 카드, bookTitle=게스트의 책, creatorName=테스트게스트
--
-- [삭제] DELETE /api/member-books/cards/{cardId}
--   1) 호스트로 DELETE .../90002  → member_card 생성, hidden=1
--   2) 호스트로 목록 재조회       → 90002 안 보임
--   3) 게스트로 목록 조회         → 90002 여전히 보임 (소프트 삭제)
-- =============================================================================
