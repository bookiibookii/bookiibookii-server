-- 그룹 홈 섹션 쿼리 (GroupQueryRepository의 homeGroupQuery 기반, userId=1)
-- 공통: groups ⨝ book ⨝ users(host) ⟕ user_image, host_id <> 나, 최신순 LIMIT 5

-- S1. 신규 그룹 (findRecentGroups)
EXPLAIN ANALYZE SELECT g.*, b.*, u.*, ui.* FROM `groups` g JOIN book b ON b.book_id = g.book_id JOIN users u ON u.id = g.host_id LEFT JOIN user_image ui ON ui.user_id = u.id
WHERE g.host_id <> 1 AND g.group_status = 'RECRUITING' AND g.created_at >= NOW(6) - INTERVAL 1 DAY
ORDER BY g.created_at DESC, g.group_id DESC LIMIT 5;

-- S2. 장르 후보 카테고리 (findCategoriesWithRecruitingGroups)
EXPLAIN ANALYZE SELECT DISTINCT b.category FROM `groups` g JOIN book b ON b.book_id = g.book_id
WHERE g.group_status = 'RECRUITING' AND g.host_id <> 1 AND b.category NOT IN ('ALL', 'LITERATURE_ALL', 'NON_LITERATURE_ALL')
ORDER BY b.category;

-- S3. 장르 그룹 (findGroupsByCategories, 예: 한국소설)
EXPLAIN ANALYZE SELECT g.*, b.*, u.*, ui.* FROM `groups` g JOIN book b ON b.book_id = g.book_id JOIN users u ON u.id = g.host_id LEFT JOIN user_image ui ON ui.user_id = u.id
WHERE g.host_id <> 1 AND g.group_status = 'RECRUITING' AND b.category IN ('KOREAN_NOVEL')
ORDER BY g.created_at DESC, g.group_id DESC LIMIT 5;

-- S4. 고전 그룹 (findClassicGroups)
EXPLAIN ANALYZE SELECT g.*, b.*, u.*, ui.* FROM `groups` g JOIN book b ON b.book_id = g.book_id JOIN users u ON u.id = g.host_id LEFT JOIN user_image ui ON ui.user_id = u.id
WHERE g.host_id <> 1 AND g.group_status = 'RECRUITING'
  AND b.isbn13 IN (SELECT c.isbn13 FROM home_section_book_candidates c WHERE c.section_type = 'CLASSIC_BOOK_GROUP' AND c.active = 1)
ORDER BY g.created_at DESC, g.group_id DESC LIMIT 5;

-- S5. 택배 그룹 (findGroupsByTradeType)
EXPLAIN ANALYZE SELECT g.*, b.*, u.*, ui.* FROM `groups` g JOIN book b ON b.book_id = g.book_id JOIN users u ON u.id = g.host_id LEFT JOIN user_image ui ON ui.user_id = u.id
WHERE g.host_id <> 1 AND g.group_status = 'RECRUITING' AND g.trade_type = 'DELIVERY'
ORDER BY g.created_at DESC, g.group_id DESC LIMIT 5;
