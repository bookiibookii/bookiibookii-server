-- 병목 쿼리 실행계획 확인용 (Hibernate가 실제로 보낸 SQL을 performance_schema 다이제스트에서 복원)

-- Q1. GET /api/groups 목록 (GroupQueryRepository.findGroupsByFilters)
EXPLAIN SELECT g.*, b.*, h.* FROM `groups` g JOIN book b ON b.book_id = g.book_id JOIN users h ON h.id = g.host_id
LEFT JOIN group_place gp ON g.group_id = gp.group_id
WHERE g.group_status = 'RECRUITING' GROUP BY g.group_id ORDER BY g.created_at DESC, g.group_id DESC LIMIT 11;

-- Q2. GET /api/groups 전체 개수 (countGroupsByFilters)
EXPLAIN SELECT COUNT(DISTINCT g.group_id) FROM `groups` g WHERE g.group_status = 'RECRUITING';

-- Q3. GET /api/groups/home 인기 도서 (findPopularBooks)
EXPLAIN SELECT b.isbn13, b.title, b.author, b.image FROM `groups` g JOIN book b ON b.book_id = g.book_id
WHERE g.group_status <> 'DELETED' AND b.isbn13 IN (
  SELECT DISTINCT b2.isbn13 FROM `groups` g2 JOIN book b2 ON b2.book_id = g2.book_id
  WHERE g2.group_status = 'RECRUITING' AND g2.host_id <> 1)
GROUP BY b.book_id, b.isbn13, b.title, b.author, b.image
ORDER BY COUNT(g.group_id) DESC, MAX(g.created_at) DESC, b.book_id DESC LIMIT 5;

-- Q4. GET /api/groups/search 개수 (searchGroupsByKeyword)
EXPLAIN SELECT COUNT(DISTINCT g.group_id) FROM `groups` g JOIN book b ON b.book_id = g.book_id
WHERE (lower(b.title) LIKE '%산책%' OR lower(b.author) LIKE '%산책%') AND g.group_status = 'RECRUITING';

-- Q5. GET /api/profiles/{nickname} (UserRepository.findByNickName)
EXPLAIN SELECT * FROM users u WHERE u.nickname = 'reader1';
