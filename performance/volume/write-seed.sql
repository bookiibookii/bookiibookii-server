-- =====================================================================
-- 쓰기 API 볼륨/동시성 테스트용 데이터 (seed.sql 적재 후 실행, 로컬 bookii_volume 전용)
--
-- 유저 (신규, 기존 볼륨 데이터와 겹치지 않음)
--   400001 ~ 401000 : 그룹 생성용 호스트 (각자 희망교환장소 user_exchange 1개)
--   401001 ~ 401003 : 인기 그룹 호스트 (참여 신청 동시성 테스트)
--   401101 ~ 401112 : 수락 테스트용 그룹 호스트
--   402001 ~ 404000 : 참여 신청자 (활성 그룹 없음)
--   404001 ~ 404500 : 탈퇴 테스트용 일반 유저
--
-- 그룹
--   2200001 ~ 2200003 : 인기 그룹 (RECRUITING, 신청 없음)
--   2100001 ~ 2100012 : 수락 테스트 그룹, 대기 신청 수 1/1/1, 100x3, 1000x3, 5000x3
--
-- 키워드
--   약 10만 개 (무작위 한글 2~3음절 + 도서 제목 단어)
--   '바다' 구독자 5만 / '겨울' 1만 / '여름' 1천 / '도시' 0명
--   → 책 제목 첫 단어가 해당 키워드인 책(book_id % 20 = 1/3/2/4)으로 그룹을 만들어 구독자 수별 비교
-- =====================================================================

SET SESSION sql_log_bin = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 이전 실행이 남긴 테스트 데이터 정리 (재실행 시 호스트 그룹 한도·중복 신청에 걸리지 않도록)
--   * group_id 2100001 이상: 이 스크립트가 만든 그룹 + 테스트 중 POST /api/groups로 생성된 그룹
--     (seed.sql의 그룹은 2,000,006번까지라 겹치지 않는다)
--   * 신청: 위 그룹에 대한 신청 + 분산 신청 테스트에서 기존 그룹에 넣은 신청(신청자 300001~302000, 402001~404000)
--   * 알림: notification_id 2,000,000 초과 (seed.sql의 알림은 2,000,000번까지)
-- ---------------------------------------------------------------------
DELETE FROM application
WHERE group_id >= 2100001
   OR guest_id BETWEEN 300001 AND 302000
   OR guest_id BETWEEN 402001 AND 404000;
DELETE FROM member_book WHERE group_id >= 2100001;
DELETE FROM matchedmember WHERE group_id >= 2100001;
DELETE FROM group_place WHERE group_id >= 2100001;
DELETE FROM group_rule WHERE group_id >= 2100001;
DELETE FROM `groups` WHERE group_id >= 2100001;
DELETE FROM notification WHERE notification_id > 2000000;

-- ---------------------------------------------------------------------
-- 유저
-- ---------------------------------------------------------------------
DELETE FROM users WHERE id BETWEEN 400001 AND 404500;
INSERT INTO users (id, created_at, updated_at, nickname, social_id, social_type, role, status, onboarding_status, gender, introduction)
SELECT n, NOW(6), NOW(6), CONCAT('writer', n), CONCAT('volume-', n), 'KAKAO', 'USER', 'ACTIVE', 'COMPLETED', 'NONE', '쓰기 테스트 유저'
FROM seq WHERE n BETWEEN 400001 AND 404500;

-- 그룹 생성 호스트의 희망교환장소
DELETE FROM user_exchange WHERE user_id BETWEEN 400001 AND 401000;
DELETE FROM location WHERE location_id BETWEEN 900001 AND 901000;
INSERT INTO location (location_id, created_at, updated_at, place_name, address, zip_code, x, y)
SELECT 900000 + n, NOW(6), NOW(6), '강남역 11번 출구', '서울특별시 강남구 강남대로 396', '06232', 127.0276210000, 37.4979420000
FROM seq WHERE n BETWEEN 1 AND 1000;
INSERT INTO user_exchange (user_exchange_id, user_id, location_id, is_default, address_detail, created_at, updated_at)
SELECT 900000 + n, 400000 + n, 900000 + n, 1, '1층', NOW(6), NOW(6)
FROM seq WHERE n BETWEEN 1 AND 1000;

-- ---------------------------------------------------------------------
-- 그룹 (인기 그룹 3개 + 수락 테스트 그룹 12개): groups + group_place + host matchedmember
-- ---------------------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS wgroup;
CREATE TEMPORARY TABLE wgroup (group_id BIGINT PRIMARY KEY, host_id BIGINT, pending INT);
INSERT INTO wgroup VALUES
  (2200001, 401001, 0), (2200002, 401002, 0), (2200003, 401003, 0),
  (2100001, 401101, 1), (2100002, 401102, 1), (2100003, 401103, 1),
  (2100004, 401104, 100), (2100005, 401105, 100), (2100006, 401106, 100),
  (2100007, 401107, 1000), (2100008, 401108, 1000), (2100009, 401109, 1000),
  (2100010, 401110, 5000), (2100011, 401111, 5000), (2100012, 401112, 5000);

INSERT INTO `groups` (group_id, book_id, host_id, created_at, updated_at, group_period, max_capacity, start_date,
                      group_comment, group_name, group_status, group_type, trade_type)
SELECT group_id, 1004, host_id, NOW(6), NOW(6), 14, 2, NULL, '쓰기 테스트', CONCAT('쓰기 테스트 그룹 ', group_id), 'RECRUITING', 'RELAY', 'DIRECT'
FROM wgroup;

INSERT INTO group_place (group_id, created_at, updated_at, place_name, address, zip_code, x, y, source_type)
SELECT group_id, NOW(6), NOW(6), '강남역 11번 출구', '서울특별시 강남구 강남대로 396', '06232', 127.0276210000, 37.4979420000, 'USER_EXCHANGE'
FROM wgroup;

INSERT INTO matchedmember (group_id, user_id, role, member_status, reading_status, exchange_status, is_review_written, created_at, updated_at)
SELECT group_id, host_id, 'HOST', 'JOINED', 'MY_BOOK_READING', 'NOT_STARTED', 0, NOW(6), NOW(6)
FROM wgroup;

-- 수락 테스트 그룹의 대기 신청: 신청자는 그룹마다 다른 기존 볼륨 유저 (11 ~ 5010 + 오프셋)
INSERT INTO application (group_id, guest_id, book_id, apply_msg, application_status, created_at, updated_at)
SELECT w.group_id, 11 + (w.group_id - 2100001) * 5000 + s.n, 2000 + s.n % 1000, '같이 읽고 싶어요', 'PENDING', NOW(6), NOW(6)
FROM wgroup w JOIN seq s ON s.n <= w.pending
WHERE w.pending > 0;

-- ---------------------------------------------------------------------
-- 키워드 약 10만 개 + 구독
-- ---------------------------------------------------------------------
DELETE FROM user_keyword;
DELETE FROM keyword;

-- 도서 제목에 쓰인 단어 (정확 매칭 대상)
INSERT INTO keyword (created_at, updated_at, content, normalized_content, prefix2)
SELECT NOW(6), NOW(6), w, w, LEFT(w, 2) FROM (
  SELECT '고양이' w UNION ALL SELECT '바다' UNION ALL SELECT '여름' UNION ALL SELECT '겨울' UNION ALL SELECT '도시'
  UNION ALL SELECT '숲' UNION ALL SELECT '편지' UNION ALL SELECT '기억' UNION ALL SELECT '시간' UNION ALL SELECT '우주'
  UNION ALL SELECT '노래' UNION ALL SELECT '산책' UNION ALL SELECT '비밀' UNION ALL SELECT '이야기' UNION ALL SELECT '여행'
) t;

-- 무작위 한글 2~3음절 키워드 (INSERT IGNORE로 정규화 중복 제거)
INSERT IGNORE INTO keyword (created_at, updated_at, content, normalized_content, prefix2)
SELECT NOW(6), NOW(6), k, k, LEFT(k, 2) FROM (
  -- CHAR(코드포인트 USING ucs2)로 한글 음절을 만든 뒤 utf8mb4로 변환
  -- (n % 11172, (n DIV 11172)*997 + n*13) 조합으로 주기 반복 없이 약 10만 개 생성
  SELECT CONVERT(CONCAT(CHAR(0xAC00 + n % 11172 USING ucs2),
                        CHAR(0xAC00 + ((n DIV 11172) * 997 + n * 13) % 11172 USING ucs2),
                        IF(n % 3 = 0, '', CHAR(0xAC00 + (n * 31) % 11172 USING ucs2))) USING utf8mb4) k
  FROM seq WHERE n <= 110000
) t;

-- 구독: '바다' 5만(유저 11~50010), '겨울' 1만(50011~60010), '여름' 1천(60011~61010), '도시' 0
INSERT INTO user_keyword (user_id, keyword_id, created_at, updated_at)
SELECT 10 + s.n, k.id, NOW(6), NOW(6) FROM seq s JOIN keyword k ON k.normalized_content = '바다' WHERE s.n <= 50000;
INSERT INTO user_keyword (user_id, keyword_id, created_at, updated_at)
SELECT 50010 + s.n, k.id, NOW(6), NOW(6) FROM seq s JOIN keyword k ON k.normalized_content = '겨울' WHERE s.n <= 10000;
INSERT INTO user_keyword (user_id, keyword_id, created_at, updated_at)
SELECT 60010 + s.n, k.id, NOW(6), NOW(6) FROM seq s JOIN keyword k ON k.normalized_content = '여름' WHERE s.n <= 1000;

SET FOREIGN_KEY_CHECKS = 1;

SELECT (SELECT COUNT(*) FROM keyword) keywords,
       (SELECT COUNT(*) FROM user_keyword) subscriptions,
       (SELECT COUNT(*) FROM application) applications,
       (SELECT COUNT(*) FROM user_exchange WHERE user_id BETWEEN 400001 AND 401000) host_places;
