-- =====================================================================
-- 그룹 홈 측정용 데이터 보정 (seed.sql 적재 후 실행, 로컬 bookii_volume 전용)
--
-- seed.sql은 user_image를 만들지 않았는데, user_image가 비어 있으면 홈 섹션 쿼리의
-- LEFT JOIN user_image가 hash join으로 실행되어 LIMIT 조기 종료가 깨진다(실제 서비스와 다른 실행계획).
-- 실제 서비스처럼 프로필 이미지가 있는 유저와 고전 후보 도서를 넣는다.
-- =====================================================================

SET SESSION sql_log_bin = 0;

-- 프로필 이미지: 유저의 30% (id 끝자리 0, 3, 7)
DELETE FROM user_image WHERE s3_key LIKE 'profile/%';
INSERT INTO user_image (user_id, s3_key, created_at, updated_at)
SELECT id, CONCAT('profile/', id, '.jpg'), NOW(6), NOW(6)
FROM users
WHERE id % 10 IN (0, 3, 7) AND id <= 400000;

-- 홈 고전 섹션 후보 도서 200권 (운영은 generate_classic_book_candidates_seed.py로 적재)
DELETE FROM home_section_book_candidates WHERE section_type = 'CLASSIC_BOOK_GROUP';
INSERT INTO home_section_book_candidates (section_type, isbn13, title, author, source_type, display_order, active, created_at, updated_at)
SELECT 'CLASSIC_BOOK_GROUP', isbn13, title, author, 'ALADIN_CATEGORY_SEED', ROW_NUMBER() OVER (ORDER BY book_id), 1, NOW(6), NOW(6)
FROM book
WHERE book_id % 500 = 7
LIMIT 200;

ANALYZE TABLE user_image, home_section_book_candidates;
