-- [역할] keyword-notification 부하 테스트에 필요한 DB 시드 데이터를 생성/정리하는 저장 프로시저
--
-- [용도]
--   테스트용 키워드 1개와 구독자 유저 N명을 DB에 생성해 부하 테스트 환경을 구성한다.
--   테스트 데이터는 TEST_KN_ prefix로 식별되어 운영 데이터와 충돌하지 않는다.
--
-- [등록] 최초 1회만 실행하면 프로시저가 DB에 등록된다.
--   Get-Content performance/k6/seed-keyword-notification.sql | mysql -h HOST -u USER -pPASS DB_NAME
--
-- [사용법]
--   CALL seed_keyword_notification(N);   -- 구독자 N명 생성 (테스트 전)
--   CALL cleanup_keyword_notification(); -- 테스트 데이터 전체 삭제 (테스트 후)

DROP PROCEDURE IF EXISTS seed_keyword_notification;

DELIMITER $$
CREATE PROCEDURE seed_keyword_notification(IN n INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE kw_id BIGINT;
    DECLARE new_user_id BIGINT;

    SET kw_id = (SELECT id FROM keyword WHERE normalized_content = 'test_kn_keyword' LIMIT 1);
    IF kw_id IS NULL THEN
        INSERT INTO keyword (content, normalized_content, prefix2, created_at, updated_at)
        VALUES ('TEST_KN_Keyword', 'test_kn_keyword', 'te', NOW(), NOW());
        SET kw_id = LAST_INSERT_ID();
    END IF;

    WHILE i <= n DO
        -- 이미 존재하면 해당 행의 ID를 LAST_INSERT_ID()로 반환, 없으면 새로 생성
        INSERT INTO users (nickname, social_type, social_id, status, role, created_at, updated_at)
        VALUES (CONCAT('TEST_KN_User_', i), 'KAKAO', CONCAT('TEST_KN_', i), 'ACTIVE', 'USER', NOW(), NOW())
        ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
        SET new_user_id = LAST_INSERT_ID();

        -- 이미 구독 중이면 무시
        INSERT IGNORE INTO user_keyword (user_id, keyword_id, created_at, updated_at)
        VALUES (new_user_id, kw_id, NOW(), NOW());

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS cleanup_keyword_notification;

DELIMITER $$
CREATE PROCEDURE cleanup_keyword_notification()
BEGIN
    DELETE n FROM notification n
    INNER JOIN users u ON n.receiver_user_id = u.id
    WHERE u.nickname LIKE 'TEST_KN_%';

    DELETE uk FROM user_keyword uk
    INNER JOIN users u ON uk.user_id = u.id
    WHERE u.nickname LIKE 'TEST_KN_%';

    DELETE FROM users WHERE nickname LIKE 'TEST_KN_%';

    DELETE FROM keyword WHERE normalized_content = 'test_kn_keyword';
END$$
DELIMITER ;
