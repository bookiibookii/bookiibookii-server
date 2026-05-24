-- GroupBook → MemberBook + BookReview 데이터 마이그레이션 (#475)
-- 실행 전: 운영 DB 백업 필수
-- 실행 환경: MySQL 8.x
--
-- 매핑 규칙
--   group_book.user_id + group_id → matchedmember
--   group_book.book_id (= groups.book_id) → member_book (is_mine: HOST=1, GUEST=0)
--   group_book.rating/comment       → book_review (star/comment)

START TRANSACTION;

-- 1) member_book 백필 (없는 경우만 INSERT)
INSERT INTO member_book (
    group_id,
    book_id,
    matchedmember_id,
    is_mine,
    current_page,
    removed_at,
    created_at,
    updated_at
)
SELECT
    gb.group_id,
    gb.book_id,
    mm.matchedmember_id,
    CASE WHEN mm.role = 'HOST' THEN 1 ELSE 0 END AS is_mine,
    0 AS current_page,
    gb.removed_at,
    gb.created_at,
    gb.updated_at
FROM group_book gb
INNER JOIN matchedmember mm
    ON mm.user_id = gb.user_id
   AND mm.group_id = gb.group_id
WHERE NOT EXISTS (
    SELECT 1
    FROM member_book mb
    WHERE mb.matchedmember_id = mm.matchedmember_id
      AND mb.book_id = gb.book_id
      AND mb.is_mine = CASE WHEN mm.role = 'HOST' THEN 1 ELSE 0 END
);

-- 2) removed_at 동기화 (이미 member_book이 있는 경우)
UPDATE member_book mb
INNER JOIN matchedmember mm
    ON mb.matchedmember_id = mm.matchedmember_id
INNER JOIN group_book gb
    ON gb.user_id = mm.user_id
   AND gb.group_id = mm.group_id
   AND mb.book_id = gb.book_id
   AND mb.is_mine = CASE WHEN mm.role = 'HOST' THEN 1 ELSE 0 END
SET mb.removed_at = gb.removed_at
WHERE gb.removed_at IS NOT NULL
  AND mb.removed_at IS NULL;

-- 3) book_review 백필 (rating이 있는 GroupBook만)
INSERT INTO book_review (
    matchedmember_id,
    member_book_id,
    star,
    comment,
    created_at,
    updated_at
)
SELECT
    mm.matchedmember_id,
    mb.member_book_id,
    gb.rating,
    gb.comment,
    gb.created_at,
    gb.updated_at
FROM group_book gb
INNER JOIN matchedmember mm
    ON mm.user_id = gb.user_id
   AND mm.group_id = gb.group_id
INNER JOIN member_book mb
    ON mb.matchedmember_id = mm.matchedmember_id
   AND mb.book_id = gb.book_id
   AND mb.is_mine = CASE WHEN mm.role = 'HOST' THEN 1 ELSE 0 END
WHERE gb.rating IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM book_review br
      WHERE br.matchedmember_id = mm.matchedmember_id
        AND br.member_book_id = mb.member_book_id
  );

COMMIT;

-- 검증 쿼리 (선택)
-- SELECT COUNT(*) FROM group_book gb WHERE gb.rating IS NOT NULL;
-- SELECT COUNT(*) FROM book_review;
-- SELECT COUNT(*) FROM group_book gb
--   LEFT JOIN matchedmember mm ON mm.user_id = gb.user_id AND mm.group_id = gb.group_id
--  WHERE mm.matchedmember_id IS NULL;
