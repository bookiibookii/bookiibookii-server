-- 스웨거 테스트용 더미 데이터 (user_id 1 = 카카오 로그인만 한 상태, 나머지 DB 비어 있음 가정)
-- 실행 순서: book → groups → user_book → matchedmember → tracker → card → card_image → card_state
--
-- 테스트 가능 API:
--   GET /api/cards/group/1        → currentBookOwner, myComment, partnerComment, cards
--   GET /api/cards/detail/1      → 카드1 상세(북마크 O)
--   GET /api/cards/detail/2      → 404(숨김)
--   GET /api/cards/bookmarks     → 카드1만
--   PATCH /api/cards/3/bookmark  → 토글
--   DELETE /api/cards/3          → 숨김 처리
--   GET /api/library/books       → 서재 1권(groupType=RELAY 포함)
--
-- currentBookOwner.nickname이 비어 나오면 user 테이블에서 id=1인 행의 nickname을 설정해두면 됨.
-- myComment: user 1로 로그인 후 GET /api/cards/group/1 호출 시 아래 한줄평이 나옴.
-- partnerComment: 현재 주자가 user 1이므로 null. (상대가 주자일 때만 상대 한줄평이 찍힘)

-- 1. 책 1권
INSERT INTO book (book_id, isbn13, title, author, publisher, image, totalPages, category)
VALUES (1, '9788966260950', '스웨거 테스트 책', '저자', '출판사', 'https://example.com/cover.jpg', 100, 'NOVEL_GENRE');

-- 2. 그룹 1개 (host = user 1, RELAY로 해서 트래커 currentBookOwner 테스트)
INSERT INTO `groups` (group_id, book_id, host_id, max_capacity, start_date, group_period, group_status, group_comment, group_type, trade_type, created_at, updated_at)
VALUES (1, 1, 1, 4, CURDATE(), 14, 'MATCHED', '테스트 그룹', 'RELAY', 'DIRECT', NOW(), NOW());

-- 3. 유저북 (user 1이 이 그룹에서 읽는 책 → 라이브러리/groupType·myComment 테스트)
INSERT INTO user_book (user_book_id, user_id, book_id, group_id, rating, comment, created_at, updated_at)
VALUES (1, 1, 1, 1, 4.5, '나의 한줄평: 이 책 정말 좋았어요!', NOW(), NOW());

-- 4. 그룹 멤버 (user 1 = 방장 = 현재 주자)
INSERT INTO matchedmember (matchedmember_id, group_id, user_id, role, reading_order, current_reading_rate, created_at, updated_at)
VALUES (1, 1, 1, 'HOST', 1, 0, NOW(), NOW());

-- 5. 트래커 (현재 주자 = matchedmember 1 → GET /api/cards/group/1 시 currentBookOwner 반환용)
INSERT INTO tracker (tracker_id, group_id, tracker_status, start_date, end_date, extension_count, extension_days, matchedmember_id, created_at, updated_at)
VALUES (1, 1, 'HOST_READING', NOW(), NULL, 0, 0, 1, NOW(), NOW());

-- 6. 독서 카드 3장
INSERT INTO card (card_id, user_book_id, group_id, page, memo, created_at, updated_at)
VALUES
  (1, 1, 1, 1, '카드1 메모', NOW(), NOW()),
  (2, 1, 1, 2, '카드2 메모', NOW(), NOW()),
  (3, 1, 1, 3, '카드3 메모', NOW(), NOW());

-- 7. 카드 이미지 (s3_key 유니크)
INSERT INTO card_image (card_image_id, s3_key, card_id, created_at, updated_at)
VALUES
  (1, 'dummy/card1-image-key', 1, NOW(), NOW()),
  (2, 'dummy/card2-image-key', 2, NOW(), NOW()),
  (3, 'dummy/card3-image-key', 3, NOW(), NOW());

-- 8. card_state (북마크/숨김 테스트)
-- 카드1: 북마크 O → 그룹 목록·내 북마크에 표시
-- 카드2: 숨김 O → 그룹 목록에서 제외
-- 카드3: 없음 → 토글/삭제 API 테스트용
INSERT INTO card_state (card_state_id, user_id, card_id, bookmarked, hidden, created_at, updated_at)
VALUES
  (1, 1, 1, 1, 0, NOW(), NOW()),
  (2, 1, 2, 0, 1, NOW(), NOW());
