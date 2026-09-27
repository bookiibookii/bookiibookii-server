-- =====================================================================
-- 볼륨 테스트용 더미 데이터 적재 (로컬 bookii_volume 스키마 전용)
--
-- 모델: 그룹 1개 = 1:1 교환독서
--   groups.group_id           = g
--   matchedmember             = 2g-1 (HOST), 2g (GUEST)
--   member_book               = 4g-3 host 내 책 / 4g-2 host가 읽는 상대 책
--                               4g-1 guest 내 책 / 4g   guest가 읽는 상대 책
--   book_review, member_review = 2g-1, 2g (완료 그룹만)
--   notification              = g (host에게 1건)
--   RECRUITING / DELETED 그룹은 host의 matchedmember(2g-1), member_book(4g-3)만 존재
--
-- 단계(누적): stage_of(g) = 'S' (g%8=0) / 'M' (g%2=0 AND g%8<>0) / 'L' (g%2=1)
--   S  : 25만 그룹  / M 누적: 100만 그룹 / L 누적: 200만 그룹
--   → 어느 단계든 created_at이 약 2년 구간에 고르게 분포하고 밀도만 달라진다.
--
-- 상태 분포(해시 기반): RECRUITING 10% / MATCHED 5% / COMPLETED 70% / DELETED 15%
-- 헤비 유저(user 1): g%1000=0 인 그룹 전부 참여(단계와 무관하게 2,000건, 모두 COMPLETED)
--
-- 사용:
--   mysql bookii_volume < performance/volume/seed.sql        -- (최초 1회) 시퀀스·기초 데이터·계획 테이블·프로시저 생성
--   CALL seed_stage('S');  CALL seed_stage('M');  CALL seed_stage('L');
--   CALL seed_fixtures();                                    -- 진행 중 트래커 등 테스트 픽스처
-- =====================================================================

SET SESSION sql_log_bin = 0;

-- ---------------------------------------------------------------------
-- 공통: 숫자 시퀀스 (1 ~ 2,000,000)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS seq;
CREATE TABLE seq (n INT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO seq (n)
SELECT d0.d + d1.d*10 + d2.d*100 + d3.d*1000 + d4.d*10000 + d5.d*100000 + d6.d*1000000 + 1
FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d0,
     (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1,
     (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2,
     (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d3,
     (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d4,
     (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d5,
     (SELECT 0 d UNION ALL SELECT 1) d6
WHERE d0.d + d1.d*10 + d2.d*100 + d3.d*1000 + d4.d*10000 + d5.d*100000 + d6.d*1000000 < 2000000;

-- ---------------------------------------------------------------------
-- 결정적(deterministic) 분배 함수
-- ---------------------------------------------------------------------
DROP FUNCTION IF EXISTS stage_of;
DROP FUNCTION IF EXISTS status_of;
DROP FUNCTION IF EXISTS host_of;
DROP FUNCTION IF EXISTS guest_of;
DROP FUNCTION IF EXISTS created_of;

DELIMITER //
CREATE FUNCTION stage_of(g INT) RETURNS CHAR(1) DETERMINISTIC NO SQL
BEGIN
  IF g % 8 = 0 THEN RETURN 'S'; END IF;
  IF g % 2 = 0 THEN RETURN 'M'; END IF;
  RETURN 'L';
END //

CREATE FUNCTION status_of(g INT) RETURNS VARCHAR(12) DETERMINISTIC NO SQL
BEGIN
  DECLARE h INT;
  IF g % 1000 = 0 THEN RETURN 'COMPLETED'; END IF;          -- 헤비 유저 그룹
  SET h = (g * 2654435761) % 100;
  IF h < 10 THEN RETURN 'RECRUITING'; END IF;
  IF h < 15 THEN RETURN 'MATCHED'; END IF;
  IF h < 85 THEN RETURN 'COMPLETED'; END IF;
  RETURN 'DELETED';
END //

-- 일반 유저 id: 11 ~ 400,000 (1~10은 테스트용 예약)
CREATE FUNCTION host_of(g INT) RETURNS BIGINT DETERMINISTIC NO SQL
BEGIN
  IF g % 2000 = 0 THEN RETURN 1; END IF;
  RETURN 11 + (g * 48271) % 399990;
END //

CREATE FUNCTION guest_of(g INT) RETURNS BIGINT DETERMINISTIC NO SQL
BEGIN
  DECLARE u BIGINT;
  IF g % 1000 = 0 AND g % 2000 <> 0 THEN RETURN 1; END IF;
  SET u = 11 + (g * 16807 + 7) % 399990;
  IF u = host_of(g) THEN SET u = 11 + (u - 10) % 399990; END IF;
  RETURN u;
END //

-- 그룹 생성 시각: 현재로부터 약 2년 전 ~ 현재에 고르게 분포 (g가 클수록 최근)
CREATE FUNCTION created_of(g INT) RETURNS DATETIME(6) DETERMINISTIC NO SQL
BEGIN
  RETURN TIMESTAMP('2026-09-27 00:00:00') - INTERVAL ((2000000 - g) * 32) SECOND;
END //
DELIMITER ;

-- ---------------------------------------------------------------------
-- 기초 데이터: users 40만, book 10만 (모든 단계 공통)
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
SET unique_checks = 0;

INSERT INTO users (id, created_at, updated_at, nickname, social_id, social_type, role, status, onboarding_status, gender, introduction)
SELECT n, TIMESTAMP('2024-06-01') + INTERVAL n SECOND, NOW(6),
       CONCAT('reader', n), CONCAT('volume-', n), 'KAKAO', IF(n = 1, 'ADMIN', 'USER'),
       'ACTIVE', 'COMPLETED', ELT(1 + n % 3, 'FEMALE', 'MALE', 'NONE'), '볼륨 테스트 유저'
FROM seq WHERE n <= 400000;

-- 책 제목/저자: 검색(LIKE) 테스트를 위해 한글 단어 조합으로 생성
INSERT INTO book (book_id, created_at, updated_at, total_pages, author, image, isbn13, link, publisher, title, category)
SELECT n, NOW(6), NOW(6), 200 + n % 400,
       CONCAT(ELT(1 + n % 12, '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오'),
              ELT(1 + (n DIV 12) % 10, '민준', '서연', '도윤', '하은', '지호', '수아', '예준', '지우', '시우', '하린')),
       CONCAT('https://image.example.com/book/', n, '.jpg'),
       CONCAT('979', LPAD(n, 10, '0')),
       CONCAT('https://book.example.com/', n),
       ELT(1 + n % 6, '민음사', '문학동네', '창비', '위즈덤하우스', '김영사', '한빛미디어'),
       CONCAT(ELT(1 + n % 20, '고양이', '바다', '여름', '겨울', '도시', '숲', '편지', '기억', '시간', '우주',
                             '밤', '별', '서점', '정원', '기차', '섬', '골목', '비', '달', '마음'),
              '의 ',
              ELT(1 + (n DIV 20) % 20, '노래', '산책', '비밀', '이야기', '여행', '끝', '온도', '언어', '계절', '방',
                                       '문', '지도', '소리', '빛', '그림자', '약속', '하루', '정원', '편지', '책'),
              ' ', n),
       ELT(1 + n % 16, 'ART_CULTURE', 'ECONOMY_BUSINESS', 'GENRE_NOVEL', 'HISTORICAL_NOVEL', 'HOME_HOBBY',
           'HUMANITIES_HISTORY', 'KOREAN_NOVEL', 'LITERATURE_ETC', 'NON_LITERATURE_ETC', 'PLAY_LITERATURE',
           'POETRY_ESSAY', 'POLITICS_SOCIETY', 'ROMANCE', 'SCIENCE_IT', 'SELF_DEVELOPMENT', 'WORLD_NOVEL')
FROM seq WHERE n <= 100000;

SET FOREIGN_KEY_CHECKS = 1;
SET unique_checks = 1;

-- ---------------------------------------------------------------------
-- 그룹별 계산값 사전 계산 (행마다 함수를 반복 호출하지 않도록 1회만 계산)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS gplan;
CREATE TABLE gplan (
  n INT NOT NULL PRIMARY KEY,
  stage CHAR(1) NOT NULL,
  st VARCHAR(12) NOT NULL,
  host BIGINT NOT NULL,
  guest BIGINT NOT NULL,
  created DATETIME(6) NOT NULL,
  book BIGINT NOT NULL,
  guest_book BIGINT NOT NULL,
  direct TINYINT NOT NULL,   -- 거래방식: 단계(g%8, g%2)와 무관하게 절반씩 분포
  KEY ix_gplan_stage (stage, n)
) ENGINE=InnoDB;
INSERT INTO gplan
SELECT n, stage_of(n), status_of(n), host_of(n), guest_of(n), created_of(n), 1 + (n * 7919) % 100000, 1 + (n * 104729) % 100000, (n DIV 8) % 2 = 0
FROM seq;

-- ---------------------------------------------------------------------
-- 단계별 교환독서 데이터 적재
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_stage;
DELIMITER //
CREATE PROCEDURE seed_stage(IN p_stage CHAR(1))
BEGIN
  DECLARE v_from INT DEFAULT 1;
  DECLARE v_batch INT DEFAULT 100000;

  SET SESSION sql_log_bin = 0;
  SET FOREIGN_KEY_CHECKS = 0;
  SET unique_checks = 0;

  WHILE v_from <= 2000000 DO
    START TRANSACTION;

    -- groups
    INSERT INTO `groups` (group_id, book_id, host_id, created_at, updated_at, group_period, max_capacity, start_date,
                          group_comment, group_name, group_status, group_type, trade_type)
    SELECT n, book, host, created, created + INTERVAL 1 DAY, 14, 2,
           DATE(created), '같이 읽고 교환해요', CONCAT('교환독서 모임 ', n), st, 'RELAY',
           IF(direct = 1, 'DIRECT', 'DELIVERY')
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage;

    -- group_place (DIRECT 그룹만)
    INSERT INTO group_place (group_place_id, group_id, created_at, updated_at, place_name, address, zip_code, x, y, source_type)
    SELECT n, n, created, created,
           CONCAT(ELT(1 + n % 5, '강남역', '홍대입구역', '잠실역', '서면역', '동성로'), ' 앞'),
           CONCAT(ELT(1 + n % 5, '서울특별시 강남구 강남대로', '서울특별시 마포구 양화로', '서울특별시 송파구 올림픽로',
                                 '부산광역시 부산진구 중앙대로', '대구광역시 중구 동성로'), ' ', n % 500),
           LPAD(n % 99999, 5, '0'),
           127.0 + (n % 1000) / 1000, 37.0 + (n % 700) / 1000, 'USER_EXCHANGE'
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND direct = 1;

    -- matchedmember: HOST (모든 그룹)
    INSERT INTO matchedmember (matchedmember_id, group_id, user_id, role, member_status, reading_status, exchange_status,
                               is_review_written, created_at, updated_at, reading_started_at, completed_at, current_member_book_id)
    SELECT 2*n - 1, n, host, 'HOST', 'JOINED',
           CASE st WHEN 'COMPLETED' THEN 'COMPLETED' ELSE 'MY_BOOK_READING' END,
           CASE st WHEN 'COMPLETED' THEN IF(direct = 1, 'MEETING_COMPLETED', 'RECEIVED_CONFIRMED') ELSE 'NOT_STARTED' END,
           st = 'COMPLETED',
           created, created + INTERVAL 30 DAY, created + INTERVAL 3 DAY,
           IF(st = 'COMPLETED', created + INTERVAL 30 DAY, NULL),
           CASE st WHEN 'COMPLETED' THEN 4*n - 2 WHEN 'MATCHED' THEN 4*n - 3 ELSE NULL END
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage;

    -- matchedmember: GUEST (MATCHED / COMPLETED 그룹)
    INSERT INTO matchedmember (matchedmember_id, group_id, user_id, role, member_status, reading_status, exchange_status,
                               is_review_written, created_at, updated_at, reading_started_at, completed_at, current_member_book_id)
    SELECT 2*n, n, guest, 'GUEST', 'JOINED',
           CASE st WHEN 'COMPLETED' THEN 'COMPLETED' ELSE 'MY_BOOK_READING' END,
           CASE st WHEN 'COMPLETED' THEN IF(direct = 1, 'MEETING_COMPLETED', 'RECEIVED_CONFIRMED') ELSE 'NOT_STARTED' END,
           st = 'COMPLETED',
           created + INTERVAL 2 DAY, created + INTERVAL 30 DAY, created + INTERVAL 3 DAY,
           IF(st = 'COMPLETED', created + INTERVAL 30 DAY, NULL),
           CASE st WHEN 'COMPLETED' THEN 4*n WHEN 'MATCHED' THEN 4*n - 1 END
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage
      AND st IN ('MATCHED', 'COMPLETED');

    -- member_book: host 내 책 (모든 그룹)
    INSERT INTO member_book (member_book_id, matchedmember_id, group_id, book_id, is_mine, current_page, created_at, updated_at)
    SELECT 4*n - 3, 2*n - 1, n, book, 1, (n * 31) % 200, created, created + INTERVAL 30 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage;

    -- member_book: 나머지 3권 (MATCHED / COMPLETED 그룹)
    INSERT INTO member_book (member_book_id, matchedmember_id, group_id, book_id, is_mine, current_page, created_at, updated_at)
    SELECT 4*n - 2, 2*n - 1, n, guest_book, 0, (n * 17) % 200, created, created + INTERVAL 30 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st IN ('MATCHED', 'COMPLETED')
    UNION ALL
    SELECT 4*n - 1, 2*n, n, guest_book, 1, (n * 13) % 200, created, created + INTERVAL 30 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st IN ('MATCHED', 'COMPLETED')
    UNION ALL
    SELECT 4*n, 2*n, n, book, 0, (n * 7) % 200, created, created + INTERVAL 30 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st IN ('MATCHED', 'COMPLETED');

    -- book_review: 완료 그룹에서 각자 읽은 상대 책에 대한 리뷰
    INSERT INTO book_review (book_review_id, matchedmember_id, member_book_id, star, comment, created_at, updated_at)
    SELECT 2*n - 1, 2*n - 1, 4*n - 2, 3 + (n % 5) / 2, '재미있게 읽었어요', created + INTERVAL 20 DAY, created + INTERVAL 20 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st = 'COMPLETED'
    UNION ALL
    SELECT 2*n, 2*n, 4*n, 3 + ((n + 1) % 5) / 2, '추천합니다', created + INTERVAL 21 DAY, created + INTERVAL 21 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st = 'COMPLETED';

    -- member_review: 완료 그룹에서 서로에 대한 후기
    INSERT INTO member_review (member_review_id, group_id, writer_matched_member_id, target_matched_member_id, reaction, comment, created_at, updated_at)
    SELECT 2*n - 1, n, 2*n - 1, 2*n, IF(n % 7 = 0, 'BOOM_DOWN', 'BOOM_UP'), '좋은 교환이었어요', created + INTERVAL 30 DAY, created + INTERVAL 30 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st = 'COMPLETED'
    UNION ALL
    SELECT 2*n, n, 2*n, 2*n - 1, 'BOOM_UP', '감사합니다', created + INTERVAL 30 DAY, created + INTERVAL 30 DAY
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage AND st = 'COMPLETED';

    -- notification: 그룹마다 host에게 참여 요청 알림 1건
    INSERT INTO notification (notification_id, receiver_user_id, actor_user_id, category, type, title, message, payload,
                              dedup_key, is_read, read_at, created_at, updated_at)
    SELECT n, host, guest, 'SYSTEM', 'GROUP_JOIN_REQUEST', '새로운 참여 요청이 도착했어요',
           CONCAT('교환독서 모임 ', n, '에 참여 요청이 있어요'),
           CONCAT('{"redirectType":"GROUP_DETAIL","groupId":', n, '}'),
           CONCAT('NOTI-GRP-001:', n), n % 3 <> 0, IF(n % 3 <> 0, created + INTERVAL 1 HOUR, NULL),
           created, created
    FROM gplan WHERE n BETWEEN v_from AND v_from + v_batch - 1 AND stage = p_stage;

    COMMIT;
    SET v_from = v_from + v_batch;
  END WHILE;

  SET FOREIGN_KEY_CHECKS = 1;
  SET unique_checks = 1;
END //
DELIMITER ;

-- ---------------------------------------------------------------------
-- 테스트 픽스처: 진행 중 트래커 (user 1 = 헤비 유저, user 2 = 일반 유저)
--   group_id 2,000,001 ~ 2,000,006 : MATCHED, MY_BOOK_READING
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS seed_fixtures;
DELIMITER //
CREATE PROCEDURE seed_fixtures()
BEGIN
  SET SESSION sql_log_bin = 0;
  SET FOREIGN_KEY_CHECKS = 0;

  DELETE FROM member_book WHERE group_id > 2000000;
  DELETE FROM matchedmember WHERE group_id > 2000000;
  DELETE FROM `groups` WHERE group_id > 2000000;

  INSERT INTO `groups` (group_id, book_id, host_id, created_at, updated_at, group_period, max_capacity, start_date,
                        group_comment, group_name, group_status, group_type, trade_type)
  SELECT 2000000 + k, 100 + k, IF(k <= 3, 1, 2), NOW(6) - INTERVAL 3 DAY, NOW(6), 14, 2, CURDATE() - INTERVAL 3 DAY,
         '픽스처', CONCAT('진행중 트래커 ', k), 'MATCHED', 'RELAY', 'DELIVERY'
  FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t;

  INSERT INTO matchedmember (matchedmember_id, group_id, user_id, role, member_status, reading_status, exchange_status,
                             is_review_written, created_at, updated_at, reading_started_at, current_member_book_id)
  SELECT 4000000 + 2*k - 1, 2000000 + k, IF(k <= 3, 1, 2), 'HOST', 'JOINED', 'MY_BOOK_READING', 'NOT_STARTED', 0,
         NOW(6) - INTERVAL 3 DAY, NOW(6), NOW(6) - INTERVAL 3 DAY, 8000000 + 4*k - 3
  FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t
  UNION ALL
  SELECT 4000000 + 2*k, 2000000 + k, 3 + k, 'GUEST', 'JOINED', 'MY_BOOK_READING', 'NOT_STARTED', 0,
         NOW(6) - INTERVAL 3 DAY, NOW(6), NOW(6) - INTERVAL 3 DAY, 8000000 + 4*k - 1
  FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t;

  INSERT INTO member_book (member_book_id, matchedmember_id, group_id, book_id, is_mine, current_page, created_at, updated_at)
  SELECT 8000000 + 4*k - 3, 4000000 + 2*k - 1, 2000000 + k, 100 + k, 1, 10, NOW(6), NOW(6) FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t
  UNION ALL
  SELECT 8000000 + 4*k - 2, 4000000 + 2*k - 1, 2000000 + k, 200 + k, 0, 0, NOW(6), NOW(6) FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t
  UNION ALL
  SELECT 8000000 + 4*k - 1, 4000000 + 2*k, 2000000 + k, 200 + k, 1, 10, NOW(6), NOW(6) FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t
  UNION ALL
  SELECT 8000000 + 4*k, 4000000 + 2*k, 2000000 + k, 100 + k, 0, 0, NOW(6), NOW(6) FROM (SELECT 1 k UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) t;

  SET FOREIGN_KEY_CHECKS = 1;
END //
DELIMITER ;
