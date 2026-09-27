-- 볼륨 테스트(L: groups 200만)로 확인한 병목 쿼리 인덱스
-- ddl-auto=validate 환경(DEV/PROD)은 수동 적용 필요. 온라인 DDL로 읽기/쓰기 차단 없이 생성.

-- 1) 그룹 목록/검색/홈 섹션: WHERE group_status = ? ORDER BY created_at DESC, group_id DESC LIMIT n
--    → 인덱스 순서로 읽다가 LIMIT에서 멈춤(filesort 제거), 모집중 COUNT는 인덱스만으로 계산(커버링)
ALTER TABLE `groups` ADD INDEX idx_groups_status_created (group_status, created_at, group_id), ALGORITHM=INPLACE, LOCK=NONE;

-- 2) 홈 인기 도서 집계(바깥 쿼리): book 기준 조인 → book_id 선두 + 집계에 쓰는 group_status, created_at 포함
--    → Covering index lookup (groups 테이블 랜덤 접근 제거)
ALTER TABLE `groups` ADD INDEX idx_groups_book_status_created (book_id, group_status, created_at), ALGORITHM=INPLACE, LOCK=NONE;

-- 3) 홈 인기 도서 서브쿼리 / 카테고리 섹션: WHERE group_status = 'RECRUITING' AND host_id <> ? (book_id 조인)
--    → Covering index lookup
ALTER TABLE `groups` ADD INDEX idx_groups_status_book_host (group_status, book_id, host_id), ALGORITHM=INPLACE, LOCK=NONE;

-- 4) 타 유저 프로필/책장: WHERE nickname = ?
ALTER TABLE users ADD INDEX idx_users_nickname (nickname), ALGORITHM=INPLACE, LOCK=NONE;

-- 시행착오 기록
-- - (group_status, book_id, created_at): 홈 집계가 book → groups 방향으로 조인해 선두 컬럼이 맞지 않음
--   → FK(book_id) 인덱스 + 테이블 랜덤 접근을 선택, 오히려 79.9s → 120s(타임아웃)으로 악화 → 2)로 교체
-- - group_place (group_id, address): 주소 LIKE '%서울%'은 인덱스로 탐색 불가, 개선 효과 불분명 → 적용하지 않음
